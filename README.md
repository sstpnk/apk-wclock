# WClock

WClock is an Android 4.4 compatible fullscreen and screensaver clock with weather and a local photo collage background.

## Requirements

- Android 4.4 or newer.
- No Google Play Services required.
- Local photo folder on internal storage, SD, or USB storage exposed by the device.
- Internet access for weather.

## Weather

- Primary provider: Open-Meteo.
- Fallback provider: MET Norway.
- Location is configured manually with city name, latitude, and longitude.
- Yandex Weather is not used.

## Modes

- Fullscreen always-on mode from the launcher icon.
- Android screensaver mode through system Daydream/screensaver settings.

## Build

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

## Beta Limitations

- Brightness automation is schedule-based.
- Ambient light sensor support is planned after beta foundation.
- Folder picker is an in-app filesystem browser because many Android 4.4 TV devices do not provide a reliable system picker.
- Weather text descriptions are mapped locally in Russian.
