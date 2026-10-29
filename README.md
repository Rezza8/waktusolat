# Waktu Solat

An Android app for Malaysian prayer times (waktu solat), qibla direction, and Quran reading — with azan alarms and a home-screen widget.

## Features

- **Prayer times** — daily times for Imsak, Subuh, Syuruk, Zohor, Asar, Maghrib and Isyak, with the current/next prayer highlighted
- **Location aware** — automatic location via GPS, with manual zone selection as a fallback
- **Azan alarms** — exact-alarm notifications with Azan Makkah (short/long) playback, boot-persistent scheduling
- **Qibla compass** — sensor-driven direction to the Kaabah, using the device location
- **Home-screen widget** — today's prayer times at a glance
- **Juz Amma** — surah and ayah browsing with audio playback, plus translation
- **Themes** — multiple colour themes with light/dark support
- **Bahasa Melayu UI** — localised for Malaysian users

## Data sources

| Purpose | API |
| --- | --- |
| Prayer times | [api.waktusolat.app](https://api.waktusolat.app/) |
| Quran / Juz Amma | [api.alquran.cloud](https://alquran.cloud/api) |
| Location fallback | [ip-api.com](http://ip-api.com/) |

## Requirements

- Android Studio (Ladybug or newer recommended)
- Android SDK 34, JDK 17
- Min SDK: Android 7.0 (API 24)

## Build

```bash
git clone https://github.com/<your-username>/waktusolat.git
cd waktusolat
```

Create `local.properties` in the project root with your SDK path:

```properties
sdk.dir=C\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk
```

Then build:

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

```
app/src/main/
├── java/com/example/waktusolat/
│   ├── WaktuSolatFragment.kt      # Prayer times screen
│   ├── ArahKiblatFragment.kt      # Qibla compass screen
│   ├── JuzAmmaFragment.kt         # Surah list
│   ├── SurahActivity.kt           # Ayah reading + audio
│   ├── TetapanFragment.kt         # Settings
│   ├── AzanAlarmReceiver.kt       # Alarm scheduling / firing
│   ├── AzanPlayService.kt         # Foreground azan playback
│   ├── PrayerWidgetProvider.kt    # Home-screen widget
│   └── api/                       # Retrofit services and models
└── res/                           # Layouts, drawables, themes, raw azan audio
```

## Permissions

Location, notifications, exact alarms, and battery-optimisation exemption are requested at runtime and are only used for prayer-time accuracy and azan alarms.

## License

No licence has been chosen yet. All rights reserved by the author unless a licence is added.
