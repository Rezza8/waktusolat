package com.example.waktusolat

import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.waktusolat.api.PrayerTimeV2
import com.example.waktusolat.databinding.FragmentWaktuSolatBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WaktuSolatFragment : Fragment() {

    private var _binding: FragmentWaktuSolatBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SharedViewModel
    private lateinit var adapter: PrayerTimeAdapter
    private var countdownHandler: Handler? = null
    private var todayPrayerForCountdown: PrayerTimeV2? = null
    private var lastProgressValue = 0

    private val prayerIconMap = mapOf(
        "Imsak" to R.drawable.ic_imsak,
        "Subuh" to R.drawable.ic_subuh,
        "Syuruk" to R.drawable.ic_syuruk,
        "Zuhur" to R.drawable.ic_zohor,
        "Asar" to R.drawable.ic_asar,
        "Maghrib" to R.drawable.ic_maghrib,
        "Isyak" to R.drawable.ic_isyak
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWaktuSolatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        adapter = PrayerTimeAdapter { prayerName ->
            DialogPrayerNotification(prayerName) { refreshPrayerList() }
                .show(childFragmentManager, "prayer_notif")
        }

        binding.rvPrayerTimes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPrayerTimes.adapter = adapter

        binding.progressNext.progressDrawable = createProgressDrawable(ThemeConfig.themes[0].accent)

        binding.tvCountry.text = viewModel.getCountryName()

        binding.llZoneSelector.setOnClickListener { showZonePicker() }

        viewModel.zones.observe(viewLifecycleOwner) { zones ->
            if (zones.isNotEmpty() && viewModel.selectedZone.value == null) {
                viewModel.restoreSavedZone(zones)
            }
        }

        viewModel.selectedZone.observe(viewLifecycleOwner) { zone ->
            zone?.let { binding.tvZone.text = "${it.negeri} - ${it.daerah}" }
        }

        viewModel.todayPrayer.observe(viewLifecycleOwner) { prayer ->
            prayer?.let { updatePrayerList(it) }
        }

        viewModel.quranVerse.observe(viewLifecycleOwner) { verse ->
            verse?.let {
                binding.tvQuranText.text = "\"${it.text}\""
                binding.tvQuranRef.text = "– Surah ${it.surahName}, ${it.surah}:${it.ayah}"
            }
        }

        viewModel.currentThemeIndex.observe(viewLifecycleOwner) { applyThemeColors(it) }
        viewModel.currentThemeIndex.value?.let { applyThemeColors(it) }

        viewModel.loadQuranVerse()
    }

    private fun applyThemeColors(themeIndex: Int) {
        val primaryColor = ThemeHelper.getPrimaryTextColor(themeIndex)
        val secondaryColor = ThemeHelper.getSecondaryTextColor(themeIndex)
        val cardBg = 0x4D000000.toInt()
        val accentColor = ThemeHelper.getAccentColor(themeIndex)

        binding.tvAppTitle.setTextColor(primaryColor)
        binding.tvAppTitle.setShadowLayer(8f, 0f, 0f, accentColor)
        binding.tvAppTitle.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        binding.tvCountry.setTextColor(primaryColor)
        binding.tvCountry.setShadowLayer(6f, 0f, 0f, accentColor)
        binding.tvCountry.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        binding.tvDate.setTextColor(primaryColor)
        binding.tvHijri.setTextColor(secondaryColor)

        binding.cardDate.setCardBackgroundColor(cardBg)
        binding.cardZone.setCardBackgroundColor(cardBg)
        binding.cardPrayerList.setCardBackgroundColor(cardBg)
        binding.cardNext.setCardBackgroundColor(cardBg)

        adapter.themeIndex = themeIndex

        binding.tvNextName.setTextColor(primaryColor)
        binding.tvNextCountdown.setTextColor(primaryColor)
        binding.tvQuranText.setTextColor(primaryColor)
        binding.tvQuranRef.setTextColor(secondaryColor)

        val currentDrawable = binding.progressNext.progressDrawable
        if (currentDrawable is LayerDrawable && currentDrawable.numberOfLayers >= 2) {
            val progressLayer = currentDrawable.getDrawable(1)
            if (progressLayer is GradientDrawable) {
                progressLayer.setColor(accentColor)
            }
        }
    }

    private fun createProgressDrawable(color: Int): Drawable {
        val density = resources.displayMetrics.density
        val background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 2f * density
            setColor(Color.argb(51, 255, 255, 255))
        }
        val progress = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 2f * density
            setColor(color)
        }
        return LayerDrawable(arrayOf(background, progress)).apply {
            setId(0, android.R.id.background)
            setId(1, android.R.id.progress)
        }
    }

    private fun updatePrayerList(prayer: PrayerTimeV2) {
        val now = System.currentTimeMillis() / 1000
        val times = listOf(
            "Imsak" to (prayer.fajr - 600),
            "Subuh" to prayer.fajr,
            "Syuruk" to prayer.syuruk,
            "Zuhur" to prayer.dhuhr,
            "Asar" to prayer.asr,
            "Maghrib" to prayer.maghrib,
            "Isyak" to prayer.isha
        )

        val nextPrayer = times.firstOrNull { it.second > now }?.first
        todayPrayerForCountdown = prayer
        lastProgressValue = 0
        binding.progressNext.progress = 0
        android.util.Log.d("ProgressDebug", "Progress reset to 0 for new prayer times")
        startCountdown(prayer, nextPrayer)

        val prefs = requireContext().getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
        val items = times.map { (name, epoch) ->
            val notifKey = "notif_${name.lowercase()}"
            val muteKey = PrayerNotificationHelper.getMuteKey(name)
            val defaultEnabled = name != "Imsak" && name != "Syuruk"
            PrayerRowItem(
                name = name,
                time = formatEpoch(epoch),
                iconRes = prayerIconMap[name] ?: R.drawable.ic_bell_on,
                isNext = name == nextPrayer,
                notifEnabled = prefs.getBoolean(notifKey, defaultEnabled),
                isMuted = prefs.getBoolean(muteKey, false)
            )
        }
        adapter.submitList(items)

        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ms", "MY"))
        binding.tvDate.text = dateFormat.format(Date())

        val parts = prayer.hijri.split("-")
        if (parts.size == 3) {
            binding.tvHijri.text = "${parts[2]} ${getHijriMonth(parts[1].toIntOrNull() ?: 1)} ${parts[0]}H"
        }
    }

    private fun startCountdown(prayer: PrayerTimeV2, nextName: String?) {
        countdownHandler?.removeCallbacksAndMessages(null)
        countdownHandler = Handler(Looper.getMainLooper())

        val times = listOf(
            "Imsak" to (prayer.fajr - 600),
            "Subuh" to prayer.fajr,
            "Syuruk" to prayer.syuruk,
            "Zuhur" to prayer.dhuhr,
            "Asar" to prayer.asr,
            "Maghrib" to prayer.maghrib,
            "Isyak" to prayer.isha
        )

        val progressSegments = listOf(
            (prayer.fajr - 600) to prayer.fajr,
            prayer.fajr to prayer.syuruk,
            prayer.syuruk to prayer.dhuhr,
            prayer.dhuhr to prayer.asr,
            prayer.asr to prayer.maghrib,
            prayer.maghrib to prayer.isha,
            prayer.isha to (prayer.fajr + 86400)
        )

        fun update() {
            val now = System.currentTimeMillis() / 1000
            val next = times.firstOrNull { it.second > now }
            if (next != null) {
                val diff = next.second - now
                val h = diff / 3600
                val m = (diff % 3600) / 60
                val s = diff % 60
                binding.tvNextName.text = next.first
                binding.tvNextCountdown.text = "${h}j ${m}m ${s}s lagi"
            } else {
                binding.tvNextName.text = "Subuh"
                binding.tvNextCountdown.text = "Esok"
            }

            val currentSegment = progressSegments.firstOrNull { now >= it.first && now < it.second }
            if (currentSegment != null) {
                val total = currentSegment.second - currentSegment.first
                val elapsed = now - currentSegment.first
                val progress = ((elapsed.toFloat() / total) * 100).toInt().coerceIn(0, 100)
                android.util.Log.d("ProgressDebug", "Segment: ${currentSegment.first}-${currentSegment.second}, Now: $now, Progress: $progress%")
                animateProgress(progress)
            } else {
                android.util.Log.d("ProgressDebug", "No segment found for now=$now, setting to 100%")
                animateProgress(100)
            }

            countdownHandler?.postDelayed({ update() }, 1000)
        }
        update()
    }

    private fun animateProgress(target: Int) {
        if (lastProgressValue == target) return
        
        try {
            val animator = ObjectAnimator.ofInt(binding.progressNext, "progress", lastProgressValue, target)
            animator.duration = 500
            animator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    binding.progressNext.progress = target
                }
            })
            animator.start()
        } catch (e: Exception) {
            android.util.Log.e("ProgressDebug", "Animation failed, using direct update", e)
            binding.progressNext.progress = target
        }
        
        lastProgressValue = target
    }

    private fun showZonePicker() {
        val zones = viewModel.zones.value ?: return
        val names = mutableListOf(" Auto-detect lokasi")
        names.addAll(zones.map { "${it.negeri} - ${it.daerah} [${it.jakimCode}]" })

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Pilih Zon")
            .setItems(names.toTypedArray()) { _, which ->
                if (which == 0) {
                    detectAndSetZone()
                } else {
                    viewModel.setSelectedZone(zones[which - 1])
                }
            }
            .show()
    }

    private fun detectAndSetZone() {
        if (!PrayerLocationHelper.hasLocationPermission(requireContext())) {
            (activity as? MainActivity)?.requestLocationPermission()
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val zones = viewModel.zones.value ?: return@launch
            val detected = PrayerLocationHelper.detectZone(requireContext(), zones)
            withContext(Dispatchers.Main) {
                detected?.let { viewModel.setSelectedZone(it) }
            }
        }
    }

    private fun refreshPrayerList() {
        val currentPrayer = todayPrayerForCountdown ?: return
        val prefs = requireContext().getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
        val times = listOf(
            "Imsak" to (currentPrayer.fajr - 600),
            "Subuh" to currentPrayer.fajr,
            "Syuruk" to currentPrayer.syuruk,
            "Zuhur" to currentPrayer.dhuhr,
            "Asar" to currentPrayer.asr,
            "Maghrib" to currentPrayer.maghrib,
            "Isyak" to currentPrayer.isha
        )
        val now = System.currentTimeMillis() / 1000
        val nextPrayer = times.firstOrNull { it.second > now }?.first
        val items = times.map { (name, epoch) ->
            val notifKey = "notif_${name.lowercase()}"
            val muteKey = PrayerNotificationHelper.getMuteKey(name)
            val defaultEnabled = name != "Imsak" && name != "Syuruk"
            PrayerRowItem(
                name = name,
                time = formatEpoch(epoch),
                iconRes = prayerIconMap[name] ?: R.drawable.ic_bell_on,
                isNext = name == nextPrayer,
                notifEnabled = prefs.getBoolean(notifKey, defaultEnabled),
                isMuted = prefs.getBoolean(muteKey, false)
            )
        }
        adapter.submitList(items)
    }

    private fun formatEpoch(epoch: Long): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
        return sdf.format(Date(epoch * 1000))
    }

    private fun getHijriMonth(month: Int): String {
        return when (month) {
            1 -> "Muharam"; 2 -> "Safar"; 3 -> "Rabiulawal"; 4 -> "Rabiulakhir"
            5 -> "Jamadilawal"; 6 -> "Jamadilakhir"; 7 -> "Rejab"; 8 -> "Syaaban"
            9 -> "Ramadan"; 10 -> "Syawal"; 11 -> "Zulkaedah"; 12 -> "Zulhijjah"
            else -> ""
        }
    }

    fun launchCustomPickerFor(prayerName: String) {
        (activity as? MainActivity)?.customAzanPickerLauncher?.launch(
            android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM)
                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Pilih bunyi untuk $prayerName")
                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            }
        )
    }

    override fun onDestroyView() {
        countdownHandler?.removeCallbacksAndMessages(null)
        super.onDestroyView()
        _binding = null
    }
}
