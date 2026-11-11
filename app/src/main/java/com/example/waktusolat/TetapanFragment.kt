package com.example.waktusolat

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.waktusolat.databinding.FragmentTetapanBinding
import com.google.android.material.slider.Slider
import kotlin.math.roundToInt

class TetapanFragment : Fragment() {

    private var _binding: FragmentTetapanBinding? = null
    private val binding get() = _binding!!

    private var currentThemeIndex = 0
    private lateinit var themePrefs: android.content.SharedPreferences

    private var appliedFontIndex = FontSizeConfig.DEFAULT_INDEX
    private var isTrackingFontSize = false
    private var fontRecreateRunnable: Runnable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTetapanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        themePrefs = requireContext().getSharedPreferences(ThemeConfig.PREFS_KEY, android.content.Context.MODE_PRIVATE)
        currentThemeIndex = themePrefs.getInt(ThemeConfig.PREFS_THEME_INDEX, 0)

        setupThemeRotateButton()
        setupFontSize()
        setupAzanVolume()

        val viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        viewModel.currentThemeIndex.observe(viewLifecycleOwner) { applyThemeColors(it) }

        binding.btnProtect.setOnClickListener { showProtectAppDialog() }
    }

    private fun applyThemeColors(themeIndex: Int) {
        val primaryColor = ThemeHelper.getPrimaryTextColor(themeIndex)
        val secondaryColor = ThemeHelper.getSecondaryTextColor(themeIndex)
        val dividerColor = ThemeHelper.getDividerColor(themeIndex)
        val accentColor = ThemeConfig.themes[themeIndex.coerceIn(0, ThemeConfig.themes.size - 1)].accent

        binding.tvThemeTitle.setTextColor(primaryColor)
        binding.btnRotateTheme.setTextColor(Color.WHITE)
        binding.tvFontSizeTitle.setTextColor(primaryColor)
        binding.tvFontSizeValue.setTextColor(secondaryColor)
        binding.tvVolumeTitle.setTextColor(primaryColor)
        binding.tvAzanVolumeValue.setTextColor(secondaryColor)
        binding.tvVolumeMuted.setTextColor(secondaryColor)
        binding.tvVolumeFull.setTextColor(secondaryColor)
        binding.btnTestAzan.setTextColor(primaryColor)
        binding.btnProtect.setTextColor(primaryColor)
        binding.tvProtectSubtitle.setTextColor(secondaryColor)
        binding.tvCredit.setTextColor(secondaryColor)

        binding.dividerMain.setBackgroundColor(dividerColor)
        binding.dividerFooter.setBackgroundColor(dividerColor)

        val accentTint = ColorStateList.valueOf(accentColor)
        binding.sliderFontSize.trackTintList = accentTint
        binding.sliderFontSize.trackInactiveTintList = ColorStateList.valueOf(0x66FFFFFF)
        binding.sliderFontSize.thumbTintList = accentTint
        binding.sliderFontSize.haloTintList = ColorStateList.valueOf((accentColor and 0x00FFFFFF) or 0x33000000)
        binding.sliderFontSize.tickTintList = accentTint
        binding.sliderFontSize.tickInactiveTintList = ColorStateList.valueOf(0x66FFFFFF)

        binding.sliderAzanVolume.trackTintList = accentTint
        binding.sliderAzanVolume.trackInactiveTintList = ColorStateList.valueOf(0x66FFFFFF)
        binding.sliderAzanVolume.thumbTintList = accentTint
        binding.sliderAzanVolume.haloTintList = ColorStateList.valueOf((accentColor and 0x00FFFFFF) or 0x33000000)
    }

    private fun setupThemeRotateButton() {
        updateRotateButtonText()
        binding.btnRotateTheme.setOnClickListener {
            currentThemeIndex = (currentThemeIndex + 1) % ThemeConfig.themes.size
            themePrefs.edit().putInt(ThemeConfig.PREFS_THEME_INDEX, currentThemeIndex).apply()
            (activity as? MainActivity)?.applyTheme(currentThemeIndex)
            refreshWidget()
            updateRotateButtonText()
        }
    }

    private fun updateRotateButtonText() {
        val themeName = ThemeConfig.themes[currentThemeIndex].name
        binding.btnRotateTheme.text = "Tema: $themeName ⟳"
        val theme = ThemeConfig.themes[currentThemeIndex]
        val density = resources.displayMetrics.density
        val bg = GradientDrawable().apply {
            cornerRadius = 24f * density
            setColor(theme.accent)
        }
        binding.btnRotateTheme.background = bg
    }

    private fun setupAzanVolume() {
        val prefs = requireContext().getSharedPreferences("prefs", android.content.Context.MODE_PRIVATE)
        val saved = Math.round(prefs.getFloat(PrayerNotificationHelper.PREFS_AZAN_VOLUME, 1f) * 100f)
        val initial = (saved / 5) * 5

        binding.sliderAzanVolume.value = initial.toFloat().coerceIn(0f, 100f)
        updateAzanVolumeLabel(initial)

        binding.sliderAzanVolume.addOnChangeListener { _, value, _ ->
            val rounded = Math.round(value / 5f) * 5
            prefs.edit()
                .putFloat(PrayerNotificationHelper.PREFS_AZAN_VOLUME, rounded / 100f)
                .apply()
            updateAzanVolumeLabel(rounded)
        }

        binding.btnTestAzan.setOnClickListener {
            if (previewPlayer != null) stopAzanPreview() else playAzanPreview()
        }
    }

    private fun updateAzanVolumeLabel(value: Int) {
        binding.tvAzanVolumeValue.text = when {
            value <= 0 -> "Senyap"
            value >= 100 -> "100%"
            else -> "$value%"
        }
    }

    private fun setupFontSize() {
        val prefs = requireContext().getSharedPreferences(
            FontSizeConfig.PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
        appliedFontIndex = FontSizeConfig.savedIndex(requireContext())
        binding.sliderFontSize.value = appliedFontIndex.toFloat()
        updateFontSizeLabel(appliedFontIndex)

        binding.sliderFontSize.addOnChangeListener { _, value, fromUser ->
            if (!fromUser) return@addOnChangeListener
            val index = value.roundToInt().coerceIn(0, FontSizeConfig.scales.size - 1)
            prefs.edit().putInt(FontSizeConfig.PREF_INDEX, index).apply()
            updateFontSizeLabel(index)
            if (!isTrackingFontSize) scheduleFontApply(400)
        }
        binding.sliderFontSize.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                isTrackingFontSize = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                isTrackingFontSize = false
                applyFontSizeIfChanged(slider.value.roundToInt())
            }
        })
    }

    private fun updateFontSizeLabel(index: Int) {
        binding.tvFontSizeValue.text = FontSizeConfig.label(index)
    }

    private fun scheduleFontApply(delayMs: Long) {
        fontRecreateRunnable?.let { binding.root.removeCallbacks(it) }
        val runnable = Runnable {
            fontRecreateRunnable = null
            if (!isAdded || view == null) return@Runnable
            applyFontSizeIfChanged(binding.sliderFontSize.value.roundToInt())
        }
        fontRecreateRunnable = runnable
        binding.root.postDelayed(runnable, delayMs)
    }

    private fun applyFontSizeIfChanged(index: Int) {
        val safe = index.coerceIn(0, FontSizeConfig.scales.size - 1)
        if (safe == appliedFontIndex) return
        appliedFontIndex = safe
        fontRecreateRunnable?.let { binding.root.removeCallbacks(it) }
        fontRecreateRunnable = null
        activity?.recreate()
    }

    private var previewPlayer: MediaPlayer? = null

    private fun playAzanPreview() {
        stopAzanPreview()
        val context = requireContext()
        try {
            val file = PrayerNotificationHelper.getAzanFile(context, PrayerNotificationHelper.AZAN_PENDEK_FILENAME)
            val player = if (file != null && file.exists()) {
                MediaPlayer().apply {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    setDataSource(file.absolutePath)
                    prepare()
                }
            } else {
                val resId = resources.getIdentifier("azan_mekah_short", "raw", context.packageName)
                if (resId != 0) MediaPlayer.create(context, resId) else null
            } ?: return

            PrayerNotificationHelper.applyAzanVolume(context, player)
            player.setOnCompletionListener { stopAzanPreview() }
            player.setOnErrorListener { _, _, _ -> stopAzanPreview(); true }
            previewPlayer = player
            player.start()
            binding.btnTestAzan.text = "Hentikan Azan"
        } catch (_: Exception) {
            stopAzanPreview()
        }
    }

    private fun stopAzanPreview() {
        previewPlayer?.let { player ->
            try {
                if (player.isPlaying) player.stop()
            } catch (_: Exception) {
            }
            player.release()
        }
        previewPlayer = null
        _binding?.btnTestAzan?.text = "Uji Azan"
    }

    private fun showProtectAppDialog() {
        val ll = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 28, 48, 20)
        }

        ll.addView(TextView(requireContext()).apply {
            text = "🛡️ Lindungi Aplikasi"
            textSize = 18f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
        })

        ll.addView(TextView(requireContext()).apply {
            text = "\nUntuk memastikan notifikasi waktu solat dan azan sentiasa berfungsi, lindungi aplikasi ini daripada dihentikan oleh sistem."
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
        })

        ll.addView(TextView(requireContext()).apply {
            text = "\nLangkah-langkah:"
            textSize = 15f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        arrayOf(
            "1. Buka Tetapan (Settings) telefon",
            "2. Pergi ke Apps > Waktu Solat",
            "3. Pilih Battery / Penjimatan Kuasa",
            "4. Pilih \"Tiada had\" / \"Don't optimise\"",
            "5. Pada sesetengah telefon, tambah ke senarai \"Protected Apps\""
        ).forEach { step ->
            ll.addView(TextView(requireContext()).apply {
                text = step
                textSize = 14f
                setTextColor(android.graphics.Color.WHITE)
                setPadding(0, 4, 0, 4)
            })
        }

        val accentPurple = 0xFFBB86FC.toInt()
        ll.addView(TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { setMargins(0, 20, 0, 12) }
            setBackgroundColor(0x33FFFFFF.toInt())
        })

        val btnLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END
        }

        var dialog: android.app.Dialog? = null
        btnLayout.addView(TextView(requireContext()).apply {
            text = "TUTUP"
            textSize = 14f
            setTextColor(accentPurple)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(24, 12, 24, 12)
            isClickable = true
            isFocusable = true
            setOnClickListener { dialog?.dismiss() }
        })
        btnLayout.addView(TextView(requireContext()).apply {
            text = "BUKA TETAPAN BATERI"
            textSize = 14f
            setTextColor(accentPurple)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(24, 12, 24, 12)
            setOnClickListener {
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:${requireContext().packageName}")
                    })
                } catch (_: Exception) {}
                dialog?.dismiss()
            }
        })
        ll.addView(btnLayout)

        dialog = android.app.Dialog(requireContext()).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setContentView(ll)
            window?.let { w ->
                w.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                w.setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
                val bg = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xCC1A0033.toInt())
                    cornerRadius = 28f * resources.displayMetrics.density
                }
                ll.setBackgroundDrawable(bg)
            }
        }
        dialog!!.show()
    }

    private fun refreshWidget() {
        val context = requireContext()
        val intent = Intent(context, PrayerWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, PrayerWidgetProvider::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

    override fun onPause() {
        super.onPause()
        stopAzanPreview()
    }

    override fun onDestroyView() {
        stopAzanPreview()
        fontRecreateRunnable?.let { binding.root.removeCallbacks(it) }
        fontRecreateRunnable = null
        super.onDestroyView()
        _binding = null
    }
}
