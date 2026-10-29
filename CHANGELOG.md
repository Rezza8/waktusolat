# Changelog

All notable changes to **Waktu Solat** are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-10-29

First public release of Waktu Solat — a prayer-times companion for Malaysia, built natively for Android in Bahasa Melayu.

### Added

**Prayer times**
- Monthly JAKIM prayer times for every zone in Malaysia, with Imsak, Subuh, Syuruk, Zohor, Asar, Maghrib and Isyak
- Automatic zone detection from GPS, with manual *negeri* / *daerah* selection and an IP-based fallback
- Today's schedule with the current prayer highlighted, plus a live countdown to the next prayer
- Hijri date and a daily Quran verse on the home screen
- Last-known schedule cached on device, so times still display when offline

**Azan & reminders**
- Exact-alarm notifications scheduled for every prayer and re-armed automatically after a reboot
- Two azan recitations — *Azan Makkah* short and long — plus any system alarm ringtone as a per-prayer override
- Per-prayer mute, so a single prayer can be silenced without disabling the rest
- Foreground playback service and keep-alive service for reliable delivery on aggressive OEM battery managers
- In-app guidance for the Android 12+ exact-alarm permission and Android 13+ notification permission

**Qibla**
- Sensor-driven compass showing the bearing to the Kaabah with live alignment status and degrees remaining
- Reverse-geocoded location name so you can confirm the reading's origin

**Juz Amma**
- Surah browser for all 37 surahs of Juz Amma
- Ayah-by-ayah reading with Malay translation and Latin transliteration
- Audio recitation playback per ayah

**Widget**
- Home-screen widget showing today's prayer times, kept in sync when the zone or theme changes

**Appearance**
- Seven colour themes — Midnight, Ocean, Royal, Forest, Ember, Crimson and a *Pelangi* rainbow gradient
- Gradient backgrounds with matched status bar and navigation bar tinting
- Bottom navigation across Waktu Solat, Juz Amma, Arah Kiblat and Tetapan

### Technical

- Kotlin 1.9.22, AGP 8.2.2, Gradle 8.5, JDK 17
- `compileSdk` / `targetSdk` 34, `minSdk` 24 (Android 7.0 and above)
- Retrofit + Gson, Kotlin coroutines, ViewBinding, ViewPager2, Material Components

### Known limitations

- An internet connection is required for the first load and for refreshing the month; cached times cover short outages
- Prayer times follow JAKIM zones — locations outside Malaysia are not supported yet
- This release ships source code only; no signed APK is attached
- Very aggressive battery savers (Xiaomi, Oppo, Vivo and similar) can still delay alarms — the app includes a *Lindungi Aplikasi* guide for whitelisting it

[1.0.0]: https://github.com/Rezza8/waktusolat/releases/tag/v1.0.0
