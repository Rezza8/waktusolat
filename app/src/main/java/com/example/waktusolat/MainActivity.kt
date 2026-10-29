package com.example.waktusolat

import android.Manifest
import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.waktusolat.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: SharedViewModel

    private var currentThemeIndex = 0
    var pendingCustomPrayers: MutableList<String> = mutableListOf()
    var currentCustomIndex = 0

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            lifecycleScope.launch(Dispatchers.IO) {
                val zones = viewModel.zones.value
                if (zones != null) {
                    val detected = PrayerLocationHelper.detectZone(this@MainActivity, zones)
                    withContext(Dispatchers.Main) {
                        if (detected != null) viewModel.setSelectedZone(detected)
                    }
                }
            }
        }
        requestNotificationPermission()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        if (granted && prefs.getBoolean(PrayerNotificationHelper.PREFS_NOTIFICATION, true)) {
            PrayerNotificationHelper.createNotificationChannel(this)
        }
        viewModel.selectedZone.value?.let { viewModel.loadPrayerTimes(it.jakimCode) }
    }

    val customAzanPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null && currentCustomIndex < pendingCustomPrayers.size) {
            val uri = IntentCompat.getParcelableExtra(
                data, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java
            )
            if (uri != null) {
                val prayerName = pendingCustomPrayers[currentCustomIndex]
                getSharedPreferences("prefs", MODE_PRIVATE)
                    .edit()
                    .putString(PrayerNotificationHelper.getCustomUriKey(prayerName), uri.toString())
                    .apply()
                currentCustomIndex++
                if (currentCustomIndex < pendingCustomPrayers.size) {
                    launchNextCustomPicker()
                    return@registerForActivityResult
                }
            }
        }
        pendingCustomPrayers.clear()
        currentCustomIndex = 0
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        if (prefs.getBoolean(PrayerNotificationHelper.PREFS_NOTIFICATION, true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !PrayerNotificationHelper.hasNotificationPermission(this)
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@registerForActivityResult
            }
            PrayerNotificationHelper.createNotificationChannel(this)
            loadPrayerTimes()
        } else {
            PrayerNotificationHelper.cancelAlarms(this)
        }
    }

    private var savedState: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedState = savedInstanceState
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        currentThemeIndex = getSharedPreferences(ThemeConfig.PREFS_KEY, MODE_PRIVATE)
            .getInt(ThemeConfig.PREFS_THEME_INDEX, 0)
        applyTheme(currentThemeIndex)

        precacheAzanFiles()
        PrayerNotificationHelper.migrateOldSettings(getSharedPreferences("prefs", MODE_PRIVATE))

        setupNavigation()
        requestPermissionsOnFirstLaunch()
        checkExactAlarmPermission()

        viewModel.selectedZone.observe(this) {
            refreshWidget()
        }

        // Handle boot — reschedule alarms and start foreground service
        if (intent?.getBooleanExtra(BootReceiver.EXTRA_BOOT, false) == true) {
            val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
            if (prefs.getBoolean(PrayerNotificationHelper.PREFS_NOTIFICATION, true)) {
                KeepAliveService.start(this)
                loadPrayerTimes()
            }
        }
    }

    private fun setupNavigation() {
        val viewPager = binding.viewPager
        val adapter = MainPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 1

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.bottomNav.menu.getItem(position).isChecked = true
            }
        })

        binding.bottomNav.setOnItemSelectedListener { item ->
            val position = when (item.itemId) {
                R.id.nav_waktu_solat -> 0
                R.id.nav_juz_amma -> 1
                R.id.nav_arah_kiblat -> 2
                R.id.nav_tetapan -> 3
                else -> 0
            }
            viewPager.currentItem = position
            true
        }
        if (savedState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_waktu_solat
        }
    }

    private fun precacheAzanFiles() {
        lifecycleScope.launch(Dispatchers.IO) {
            PrayerNotificationHelper.downloadAzanIfNeeded(this@MainActivity)
            PrayerNotificationHelper.getAzanFile(this@MainActivity, PrayerNotificationHelper.AZAN_PENDEK_FILENAME)
        }
    }

    private fun requestPermissionsOnFirstLaunch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                return
            }
        }
        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            PrayerNotificationHelper.createNotificationChannel(this)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.selectedZone.value?.let { viewModel.loadPrayerTimes(it.jakimCode) }
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Keizinan Penggera Diperlukan")
                    .setMessage("Sila benarkan 'Penggera & peringatan' dalam tetapan untuk pemberitahuan waktu solat berfungsi.")
                    .setPositiveButton("Buka Tetapan") { _, _ ->
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                    .setNegativeButton("Kemudian", null)
                    .show()
            }
        }
    }

    fun applyTheme(index: Int) {
        val theme = ThemeConfig.themes[index]
        currentThemeIndex = index
        viewModel.setThemeIndex(index)

        if (index == ThemeConfig.RAINBOW_INDEX) {
            val gradient = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, ThemeConfig.rainbowGradient)
            gradient.gradientType = GradientDrawable.LINEAR_GRADIENT
            binding.rootLayout.background = gradient
        } else if (theme.gradientColors != null && theme.gradientColors.isNotEmpty()) {
            val gradient = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, theme.gradientColors)
            gradient.gradientType = GradientDrawable.LINEAR_GRADIENT
            binding.rootLayout.background = gradient
        } else {
            binding.rootLayout.setBackgroundColor(theme.bg)
        }

        // Set status bar and navigation bar colors to match theme
        val systemBarColor = if (index == ThemeConfig.RAINBOW_INDEX) {
            ThemeConfig.rainbowGradient[0] // Use first rainbow color for status bar
        } else {
            theme.bg
        }
        window.statusBarColor = systemBarColor
        window.navigationBarColor = systemBarColor

        // Ensure status bar icons are white (light icons for dark backgrounds)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val flags = window.decorView.systemUiVisibility
            window.decorView.systemUiVisibility = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
        }

        // Update fade overlay — interpolate gradient color at 240dp to avoid seam
        val fadeOverlay = findViewById<View>(R.id.fade_overlay)
        val screenHeight = resources.displayMetrics.heightPixels
        val transitionPx = 240f * resources.displayMetrics.density
        val fraction = (transitionPx / screenHeight).coerceIn(0f, 1f)
        val colors = if (index == ThemeConfig.RAINBOW_INDEX) {
            ThemeConfig.rainbowGradient
        } else if (theme.gradientColors != null && theme.gradientColors.isNotEmpty()) {
            theme.gradientColors
        } else {
            null
        }
        val matchedColor = if (colors != null && colors.size >= 2) {
            val totalSegments = colors.size - 1
            val exactSegment = fraction * totalSegments
            val segIndex = exactSegment.toInt().coerceIn(0, totalSegments - 1)
            val segFraction = exactSegment - segIndex
            interpolateColor(colors[segIndex], colors[segIndex + 1], segFraction)
        } else {
            theme.bg
        }
        fadeOverlay?.let { view ->
            val fadeGradient = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(android.graphics.Color.TRANSPARENT, matchedColor)
            )
            view.background = fadeGradient
        }

        binding.bottomNav.setBackgroundColor(theme.card)
        binding.bottomNav.itemIconTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
        binding.bottomNav.itemTextColor = android.content.res.ColorStateList.valueOf(Color.WHITE)
    }

    fun requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun loadPrayerTimes() {
        viewModel.selectedZone.value?.let { viewModel.loadPrayerTimes(it.jakimCode) }
    }

    fun launchNextCustomPicker() {
        if (currentCustomIndex >= pendingCustomPrayers.size) {
            pendingCustomPrayers.clear()
            currentCustomIndex = 0
            loadPrayerTimes()
            return
        }
        val prayerName = pendingCustomPrayers[currentCustomIndex]
        customAzanPickerLauncher.launch(
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Pilih bunyi untuk $prayerName")
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            }
        )
    }

    private fun refreshWidget() {
        val intent = Intent(this, PrayerWidgetProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = AppWidgetManager.getInstance(this)
            .getAppWidgetIds(ComponentName(this, PrayerWidgetProvider::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        sendBroadcast(intent)
    }

    private fun getHijriMonth(month: Int): String {
        return when (month) {
            1 -> "Muharam"; 2 -> "Safar"; 3 -> "Rabiulawal"; 4 -> "Rabiulakhir"
            5 -> "Jamadilawal"; 6 -> "Jamadilakhir"; 7 -> "Rejab"; 8 -> "Syaaban"
            9 -> "Ramadan"; 10 -> "Syawal"; 11 -> "Zulkaedah"; 12 -> "Zulhijjah"
            else -> ""
        }
    }

    private fun interpolateColor(from: Int, to: Int, fraction: Float): Int {
        val r = (Color.red(from) * (1f - fraction) + Color.red(to) * fraction).toInt()
        val g = (Color.green(from) * (1f - fraction) + Color.green(to) * fraction).toInt()
        val b = (Color.blue(from) * (1f - fraction) + Color.blue(to) * fraction).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }
}

class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> WaktuSolatFragment()
            1 -> JuzAmmaFragment()
            2 -> ArahKiblatFragment()
            3 -> TetapanFragment()
            else -> WaktuSolatFragment()
        }
    }
}
