# Changelog

All notable changes to **Waktu Solat** are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Main screen redesigned: large **Assalamu'alaikum** greeting, centred *Waktu Solat / Malaysia* header and a large next-azan countdown card with the prayer name, azan time and progress bar
- Prayer rows compacted and the prayer card stretched to fill the remaining screen height; the screen stays scroll-safe, so all seven waktu (Imsak through Isyak) fit without being clipped
- **Saiz Tulisan** setting in Tetapan with five steps (80%–130%); the chosen scale applies across the whole app and is remembered
- New **Dark** theme with a near-black background and emerald accent, added to the Tema rotation

### Changed
- Prayer times and zones now load from the on-device cache first and refresh in the background, so returning users see the schedule immediately; duplicate startup requests were removed

### Fixed
- The next-azan progress bar now fills gradually across the current interval; it previously painted as a static full-width line because its drawable ignored progress levels
- **Uji Azan** in Tetapan now stops playback on a second tap instead of restarting the clip; the button reads *Hentikan Azan* while playing

## [1.1.0] - 2026-11-11

### Added
- Azan volume slider in *Tetapan* (0–100 %, with a **Uji Azan** preview button); the setting scales the azan independently of the phone's alarm volume
- Home-screen widget now shows a live countdown to the next azan in its header (`date · Hijri · countdown · zone code`), and highlights the next azan's column in the active theme's accent colour
- The widget reads the cached schedule instead of hitting the network on every update, and re-renders itself after each azan, at boot, after an app update, and after a clock or timezone change

### Fixed
- Long azan playback could not be stopped: the player was never assigned to the service field, so the notification's *Tutup* action and service teardown had no effect
- Azan players are now released when playback completes instead of lingering until the service is killed

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

### Installation

1. Download `WaktuSolat-v1.0.0.apk` from the assets below
2. Open the file and allow installation from unknown sources if prompted
3. Grant location and notification permissions on first launch, then allow **Alarms & reminders**
4. Open **Tetapan → Lindungi Aplikasi** and follow the steps so your phone's battery optimiser does not delay azan alarms

The APK is signed with the project release key (SHA-256 `d9:c7:c3:04:32:50:48:71:b4:31:ad:3c:3e:e2:8c:9e:fa:39:02:90:9c:67:b5:5a:d9:8b:60:1c:14:8d:1e:e2`), targets Android 14 and runs on Android 7.0 and above.

### Known limitations

- An internet connection is required for the first load and for refreshing the month; cached times cover short outages
- Prayer times follow JAKIM zones — locations outside Malaysia are not supported yet
- Distributed outside Google Play, so updates must be installed manually
- Very aggressive battery savers (Xiaomi, Oppo, Vivo and similar) can still delay alarms — the app includes a *Lindungi Aplikasi* guide for whitelisting it

[1.0.0]: https://github.com/Rezza8/waktusolat/releases/tag/v1.0.0
