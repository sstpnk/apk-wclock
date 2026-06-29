# Release Checklist

Target: `v0.1.0`

## Before Publishing

- Run `.\gradlew.bat testDebugUnitTest assembleDebug assembleRelease`.
- Install the APK on:
  - Android 4.4 tablet or emulator.
  - Modern Android phone.
  - Android TV or TV emulator.
- Verify:
  - Full-screen mode starts from launcher.
  - Screensaver service is visible in Android settings.
  - Settings open by touch and TV remote/menu key.
  - Photo wall renders real images and animates at 30 FPS target.
  - Photo frame pans oversized images before switching.
  - Weather provider change shows `Запрос погоды` at the bottom and refreshes data.
  - Weather failure shows a short user-facing error for about one minute.
  - Brightness schedule applies without Google Play Services.
- Publish a signed release APK.
- Create a Git tag only when the public release is approved.
- Create GitHub Release only after final manual device testing.

## Versioning

- Android `versionName`: `0.1.0`.
- Android `versionCode`: `1`.
- Runtime build number is generated at build time in UTC.
