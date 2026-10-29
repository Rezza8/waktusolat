package com.example.waktusolat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.waktusolat.adapter.SurahAdapter
import com.example.waktusolat.api.JuzAmmaSurah
import com.example.waktusolat.databinding.FragmentJuzAmmaBinding

class JuzAmmaFragment : Fragment() {

    private var _binding: FragmentJuzAmmaBinding? = null
    private val binding get() = _binding!!
    private lateinit var surahAdapter: SurahAdapter
    private lateinit var viewModel: SharedViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentJuzAmmaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        surahAdapter = SurahAdapter { surah -> openSurah(surah) }
        binding.rvSurah.adapter = surahAdapter

        loadJuzAmma()

        val currentTheme = viewModel.currentThemeIndex.value ?: 0
        applyTheme(currentTheme)

        viewModel.currentThemeIndex.observe(viewLifecycleOwner) { applyTheme(it) }
    }

    private fun loadJuzAmma() {
        val surahList = listOf(
            JuzAmmaSurah(78, "النبأ", "An-Naba'", "An-Naba'", 40, "Berita Besar"),
            JuzAmmaSurah(79, "النازعات", "An-Nazi'at", "An-Nazi'at", 46, "Malaikat Yang Mencabut"),
            JuzAmmaSurah(80, "عبس", "'Abasa", "'Abasa", 42, "Ia Bermuka Masam"),
            JuzAmmaSurah(81, "التكوير", "At-Takwir", "At-Takwir", 29, "Menggulung"),
            JuzAmmaSurah(82, "الإنفطار", "Al-Infitar", "Al-Infitar", 19, "Terbelah"),
            JuzAmmaSurah(83, "المطففين", "Al-Mutaffifin", "Al-Mutaffifin", 36, "Orang-Orang Yang Curang"),
            JuzAmmaSurah(84, "الإنشقاق", "Al-Inshiqaq", "Al-Insyiqaq", 25, "Terbelah"),
            JuzAmmaSurah(85, "البروج", "Al-Buruj", "Al-Buruj", 22, "Gugusan Bintang"),
            JuzAmmaSurah(86, "الطارق", "At-Tariq", "At-Tariq", 17, "Yang Datang Di Malam Hari"),
            JuzAmmaSurah(87, "الأعلى", "Al-A'la", "Al-A'la", 19, "Yang Paling Tinggi"),
            JuzAmmaSurah(88, "الغاشية", "Al-Ghashiyah", "Al-Ghasyiyah", 26, "Hari Pembalasan"),
            JuzAmmaSurah(89, "الفجر", "Al-Fajr", "Al-Fajr", 30, "Fajar"),
            JuzAmmaSurah(90, "البلد", "Al-Balad", "Al-Balad", 20, "Negeri"),
            JuzAmmaSurah(91, "الشمس", "Ash-Shams", "Asy-Syams", 15, "Matahari"),
            JuzAmmaSurah(92, "الليل", "Al-Layl", "Al-Lail", 21, "Malam"),
            JuzAmmaSurah(93, "الضحى", "Ad-Duha", "Ad-Duha", 11, "Waktu Dhuha"),
            JuzAmmaSurah(94, "الشرح", "Ash-Sharh", "Al-Insyirah", 8, "Melapangkan"),
            JuzAmmaSurah(95, "التين", "At-Tin", "At-Tin", 8, "Buah Tin"),
            JuzAmmaSurah(96, "العلق", "Al-'Alaq", "Al-'Alaq", 19, "Segumpal Darah"),
            JuzAmmaSurah(97, "القدر", "Al-Qadr", "Al-Qadr", 5, "Kemuliaan"),
            JuzAmmaSurah(98, "البينة", "Al-Bayyinah", "Al-Bayyinah", 8, "Bukti Yang Nyata"),
            JuzAmmaSurah(99, "الزلزلة", "Az-Zalzalah", "Az-Zalzalah", 8, "Kegoncangan"),
            JuzAmmaSurah(100, "العاديات", "Al-'Adiyat", "Al-'Aadiyat", 11, "Berlari Kencang"),
            JuzAmmaSurah(101, "القارعة", "Al-Qari'ah", "Al-Qari'ah", 11, "Hari Kiamat"),
            JuzAmmaSurah(102, "التكاثر", "At-Takathur", "At-Takaatsur", 8, "Bermegah-Megahan"),
            JuzAmmaSurah(103, "العصر", "Al-'Asr", "Al-'Asr", 3, "Masa"),
            JuzAmmaSurah(104, "الهمزة", "Al-Humazah", "Al-Humazah", 9, "Pengumpat"),
            JuzAmmaSurah(105, "الفيل", "Al-Fil", "Al-Fiil", 5, "Gajah"),
            JuzAmmaSurah(106, "قريش", "Quraysh", "Quraisy", 4, "Suku Quraisy"),
            JuzAmmaSurah(107, "الماعون", "Al-Ma'un", "Al-Maa'uun", 7, "Barang Yang Berguna"),
            JuzAmmaSurah(108, "الكوثر", "Al-Kawthar", "Al-Kautsar", 3, "Nikmat Yang Berlimpah"),
            JuzAmmaSurah(109, "الكافرون", "Al-Kafirun", "Al-Kaafiruun", 6, "Orang-Orang Kafir"),
            JuzAmmaSurah(110, "النصر", "An-Nasr", "An-Nasr", 3, "Pertolongan"),
            JuzAmmaSurah(111, "المسد", "Al-Masad", "Al-Lahab", 5, "Gejolak Api"),
            JuzAmmaSurah(112, "الإخلاص", "Al-Ikhlas", "Al-Ikhlas", 4, "Memurnikan Keesaan Allah"),
            JuzAmmaSurah(113, "الفلق", "Al-Falaq", "Al-Falaq", 5, "Waktu Subuh"),
            JuzAmmaSurah(114, "الناس", "An-Nas", "An-Naas", 6, "Umat Manusia")
        )
        surahAdapter.submitList(surahList)
    }

    private fun openSurah(surah: JuzAmmaSurah) {
        val intent = android.content.Intent(requireContext(), SurahActivity::class.java)
        intent.putExtra(SurahActivity.EXTRA_SURAH, surah as java.io.Serializable)
        startActivity(intent)
    }

    private fun applyTheme(index: Int) {
        val theme = ThemeConfig.themes[index]
        val isPastel = index != ThemeConfig.RAINBOW_INDEX
        val primaryColor = ThemeHelper.getPrimaryTextColor(index)
        val secondaryColor = ThemeHelper.getSecondaryTextColor(index)
        val accentColor = theme.lightAccent

        binding.tvJuzLabel.setTextColor(accentColor)
        surahAdapter.primaryTextColor = primaryColor
        surahAdapter.secondaryTextColor = secondaryColor
        surahAdapter.badgeColor = if (isPastel) theme.accent else 0xFF43A047.toInt()
        surahAdapter.notifyItemRangeChanged(0, surahAdapter.itemCount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
