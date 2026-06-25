# WClock

WClock is an Android clock, weather, and local-photo screensaver for tablets, phones, and Android TV devices.

The project is designed for long-running ambient use: a full-screen clock and date layer, current weather and five-day forecast, and a local photo background in either photo-wall or photo-frame mode.

## Release Status

Current prepared release: `v0.1.0`.

This repository is being prepared for public beta. A GitHub Release is not published yet.

## Requirements

- Android 4.4 KitKat or newer (`minSdk 19`).
- No Google Play Services dependency.
- Works on touch devices and TV/remote-driven devices.
- Internet access is required for weather updates.
- Local image folder access is required for photo-wall and photo-frame modes.

## Features

- Full-screen launcher mode.
- Android Daydream/screensaver service.
- Clock and date overlay with burn-in position shifting.
- Current weather and five-day forecast.
- Weather providers:
  - Open-Meteo, no API key.
  - MET Norway, no API key.
  - WeatherAPI.com, optional API key.
  - OpenWeather, optional API key.
- Local photo display:
  - Photo wall with animated photo entry, rotation, light frame, and smooth fade-out.
  - Photo frame with cover-fit scaling and pan across oversized photos before switching.
- Manual location by city label and coordinates.
- Schedule-based brightness control.

## Build

Use JDK and Android Gradle Plugin compatible with this repository, then run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Each build exposes a UTC build number in settings. Format: last digit of year, month, day, hour, minute. Example: `606250947`.

## Project Layout

- `app/src/main/java/com/sstpnk/wclock/host` - activity and screensaver hosts.
- `app/src/main/java/com/sstpnk/wclock/render` - clock/weather rendering and frame loop.
- `app/src/main/java/com/sstpnk/wclock/collage` - local image scanning, decoding, photo wall, and photo frame engine.
- `app/src/main/java/com/sstpnk/wclock/weather` - weather providers and cache.
- `app/src/main/java/com/sstpnk/wclock/settings` - settings UI and persistence.

## Beta Limitations

- Brightness automation is schedule-based; ambient light sensor support is planned.
- Folder picking uses SAF on Android 5+ and an in-app file browser fallback for Android 4.4.
- Weather descriptions are mapped locally in Russian for key provider codes.
- This is not yet a Play Store-ready release.

## License

WClock is source-available for non-commercial use. See [LICENSE](LICENSE).

Commercial use, resale, paid redistribution, or selling a derivative product based on this code requires separate written permission from the copyright holder.
