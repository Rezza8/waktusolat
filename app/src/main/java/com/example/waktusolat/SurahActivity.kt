package com.example.waktusolat

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.waktusolat.adapter.AyahAdapter
import com.example.waktusolat.adapter.AyahItem
import com.example.waktusolat.api.JuzAmmaSurah
import com.example.waktusolat.api.QuranApiService
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SurahActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SURAH = "extra_surah"
    }

    private lateinit var quranApi: QuranApiService
    private lateinit var ayahAdapter: AyahAdapter
    private lateinit var rvAyahs: RecyclerView
    private lateinit var tvSurahTitle: TextView
    private lateinit var tvSurahSubtitle: TextView
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var rootLayout: LinearLayout
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_surah)

        @Suppress("DEPRECATION")
        val surah = intent.getSerializableExtra(EXTRA_SURAH) as? JuzAmmaSurah
        if (surah == null) {
            finish()
            return
        }

        rootLayout = findViewById(R.id.root_surah)
        rvAyahs = findViewById(R.id.rv_ayahs)
        tvSurahTitle = findViewById(R.id.tv_surah_title)
        tvSurahSubtitle = findViewById(R.id.tv_surah_subtitle)
        progressIndicator = findViewById(R.id.progress_surah)
        toolbar = findViewById(R.id.toolbar_surah)

        quranApi = QuranApiService.create()
        ayahAdapter = AyahAdapter()
        rvAyahs.adapter = ayahAdapter

        toolbar.setNavigationOnClickListener { finish() }

        tvSurahTitle.text = surah.nameMs
        tvSurahSubtitle.text = "${surah.nameArabic} - ${surah.meaning} (${surah.verses} ayat)"

        val themeIndex = getSharedPreferences(ThemeConfig.PREFS_KEY, MODE_PRIVATE)
            .getInt(ThemeConfig.PREFS_THEME_INDEX, 0)
        applyTheme(themeIndex)

        loadSurah(surah.number)
    }

    private fun applyTheme(index: Int) {
        val theme = ThemeConfig.themes[index]

        if (index == ThemeConfig.RAINBOW_INDEX) {
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                ThemeConfig.rainbowGradient
            )
            gradient.gradientType = GradientDrawable.LINEAR_GRADIENT
            rootLayout.background = gradient
        } else if (theme.gradientColors != null && theme.gradientColors.isNotEmpty()) {
            val gradient = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                theme.gradientColors
            )
            gradient.gradientType = GradientDrawable.LINEAR_GRADIENT
            rootLayout.background = gradient
        } else {
            rootLayout.setBackgroundColor(theme.bg)
        }

        val barColor = if (index == ThemeConfig.RAINBOW_INDEX)
            ThemeConfig.rainbowGradient[0] else theme.bg
        window.statusBarColor = barColor
        window.navigationBarColor = barColor
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }

        toolbar.setBackgroundColor(theme.card)
        tvSurahTitle.setTextColor(Color.WHITE)
        tvSurahSubtitle.setTextColor(Color.argb(179, 255, 255, 255))
        toolbar.navigationIcon?.setTint(Color.WHITE)

        progressIndicator.setIndicatorColor(theme.accent)

        ayahAdapter.primaryTextColor = Color.WHITE
        ayahAdapter.rumiTextColor = Color.argb(179, 255, 255, 255)
        ayahAdapter.malayTextColor = Color.argb(230, 255, 255, 255)
        ayahAdapter.badgeColor = if (index == ThemeConfig.RAINBOW_INDEX)
            0xFF43A047.toInt() else theme.accent
        ayahAdapter.itemBackgroundColor = 0x4D000000.toInt()
        ayahAdapter.notifyItemRangeChanged(0, ayahAdapter.itemCount)
    }

    private fun loadSurah(surahNumber: Int) {
        progressIndicator.visibility = android.view.View.VISIBLE
        lifecycleScope.launch {
            try {
                val arabicResponse = withContext(Dispatchers.IO) {
                    quranApi.getSurahArabic(surahNumber)
                }
                val malayResponse = withContext(Dispatchers.IO) {
                    quranApi.getSurahMalay(surahNumber)
                }
                val translitResponse = withContext(Dispatchers.IO) {
                    quranApi.getSurahTransliteration(surahNumber)
                }

                val arabicAyahs = arabicResponse.data.ayahs
                val malayAyahs = malayResponse.data.ayahs
                val translitAyahs = translitResponse.data.ayahs

                val items = arabicAyahs.mapIndexed { index, ayah ->
                    AyahItem(
                        number = ayah.numberInSurah,
                        arabic = ayah.text,
                        rumi = if (index < translitAyahs.size) translitAyahs[index].text else "",
                        malay = if (index < malayAyahs.size) malayAyahs[index].text else ""
                    )
                }
                ayahAdapter.submitList(items)
            } catch (e: Exception) {
                Toast.makeText(
                    this@SurahActivity,
                    "Gagal memuat surah: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressIndicator.visibility = android.view.View.GONE
            }
        }
    }
}
