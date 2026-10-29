package com.example.waktusolat

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.waktusolat.databinding.FragmentArahKiblatBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ArahKiblatFragment : Fragment(), SensorEventListener {

    private var _binding: FragmentArahKiblatBinding? = null
    private val binding get() = _binding!!

    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null
    private lateinit var vibrator: Vibrator
    private var lastVibrationTime = 0L
    private var lastSensorUpdate = 0L
    private val MIN_UPDATE_INTERVAL = 16L
    private var qiblaBearing = 0f
    private var currentLat = 0.0
    private var currentLon = 0.0
    private lateinit var viewModel: SharedViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentArahKiblatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        loadLocationAndQibla()
        updateDateTime()
        applyThemeColors(viewModel.currentThemeIndex.value ?: 0)
        viewModel.currentThemeIndex.observe(viewLifecycleOwner) { applyThemeColors(it) }
    }

    private fun loadLocationAndQibla() {
        lifecycleScope.launch {
            val location = withContext(Dispatchers.IO) {
                QiblaHelper.getLastLocation(requireContext())
            }
            if (location != null) {
                currentLat = location.latitude
                currentLon = location.longitude
                qiblaBearing = QiblaHelper.calculateBearing(currentLat, currentLon)
                val distance = QiblaHelper.calculateDistance(currentLat, currentLon)
                
                withContext(Dispatchers.Main) {
                    binding.tvLocationName.text = viewModel.selectedZone.value?.daerah ?: "Lokasi Semasa"
                    binding.tvLocationCountry.text = viewModel.selectedZone.value?.negeri ?: "Malaysia"
                    binding.tvQiblaDegree.text = "${qiblaBearing.toInt()}°"
                    binding.tvDistance.text = String.format(Locale.US, "%.0f km", distance)
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.tvLocationName.text = "Menunggu lokasi..."
                    binding.tvLocationCountry.text = "Sila benarkan akses lokasi"
                }
                // Retry after delay
                delay(3000)
                loadLocationAndQibla()
            }
        }
    }

    private fun updateDateTime() {
        val dateFormat = SimpleDateFormat("d MMM yyyy", Locale("ms", "MY"))
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        val now = Date()
        binding.tvDate.text = dateFormat.format(now)
        binding.tvTime.text = timeFormat.format(now)
    }

    private fun applyThemeColors(themeIndex: Int) {
        val accentColor = ThemeHelper.getAccentColor(themeIndex)
        val cardBg = 0xFFFFFFFF.toInt()

        binding.cardLocation.setCardBackgroundColor(cardBg)
        binding.cardCompass.setCardBackgroundColor(cardBg)
        binding.tvQiblaDegree.setTextColor(accentColor)
        binding.tvCompassStatus.setTextColor(accentColor)
        binding.tvDistance.setTextColor(accentColor)
        binding.tvDate.setTextColor(accentColor)
        binding.tvTime.setTextColor(accentColor)
        binding.compassView.arrowColor = accentColor
    }

    override fun onResume() {
        super.onResume()
        rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            val now = System.currentTimeMillis()
            if (now - lastSensorUpdate < MIN_UPDATE_INTERVAL) return
            lastSensorUpdate = now

            try {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()

                if (azimuth.isNaN() || azimuth.isInfinite()) return

                binding.compassView.qiblaBearing = qiblaBearing
                binding.compassView.deviceAzimuth = azimuth

                if (qiblaBearing > 0f) {
                    val relativeBearing = ((qiblaBearing - azimuth) % 360f + 360f) % 360f
                    if (relativeBearing.isNaN()) return
                    
                    val diff = if (relativeBearing > 180f) 360f - relativeBearing else relativeBearing
                    binding.tvCompassDegrees.text = "${diff.toInt()}° lagi"

                    val wasAligned = binding.compassView.isAligned
                    val isAligned = diff < 3f
                    binding.compassView.isAligned = isAligned
                    binding.tvCompassStatus.text = if (isAligned) "✓ Menghadap Kiblat" else "Cari arah kiblat"

                    if (isAligned && !wasAligned) {
                        lastVibrationTime = System.currentTimeMillis()
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(150)
                        }
                    }
                } else {
                    binding.tvCompassStatus.text = "Menunggu lokasi..."
                    binding.tvCompassDegrees.text = "--"
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
