# WClock PRD, Architecture, And Roadmap

## 1. Product Summary

WClock is an Android 4.4 compatible always-on clock, weather, and photo collage application for tablets and TV-like devices. It must work without Google Play Services, run both as a system screensaver and as a standalone fullscreen app, and remain suitable for continuous day and night operation.

The first public beta must provide:

- Simultaneous layered display of photo collage, current time, current date, current weather, and 5-day forecast.
- Full Android 4.4 support with `minSdkVersion 19`.
- No Google Play Services dependency.
- Tablet-first UX with TV/D-pad support.
- Local folder based photo collage.
- Screen burn-in mitigation for bright static elements.
- Automatic brightness management by schedule.
- Weather from open providers, with Open-Meteo as the primary provider.

## 2. Goals

1. Build a stable Android 4.4 APK that can run indefinitely on older tablets.
2. Provide two runtime modes in one APK:
   - Android `DreamService` screensaver mode.
   - Standalone fullscreen always-on mode.
3. Render all core information at the same time using layered composition:
   - Bottom layer: dynamic photo collage.
   - Top layers: translucent clock, date, weather, and forecast panels.
4. Make the app usable without Google Play Services, AndroidX runtime requirements, Leanback, GPS, or cloud sync.
5. Provide a high-quality custom collage engine inspired by CyanogenMod 11 Photo Table/Collage behavior, without copying its source code.
6. Reach public beta with predictable setup, recoverable errors, and clear diagnostics in settings.

## 3. Non-Goals For Public Beta

- No Yandex Weather integration. Yandex Weather is excluded because the data is paid and not accepted as a requirement.
- No Google Play Services, Firebase, Google location, Google Maps, or Play Services fused location.
- No automatic GPS/network location detection.
- No web admin panel.
- No cloud photo sync.
- No Android TV Leanback dependency, because official Android TV application APIs target newer platform assumptions than Android 4.4 devices.
- No direct reuse of CyanogenMod/AOSP collage source code.
- No paid weather provider as a required dependency.

## 4. Target Devices And Platform Constraints

### 4.1 Required Platform

- Android 4.4 KitKat, API 19.
- Native Android application written in Java.
- Runtime must avoid mandatory AndroidX dependencies.
- UI must use platform APIs available on API 19.
- Network stack must support TLS behavior available on old Android devices and degrade with clear errors when a provider cannot be reached.

### 4.2 Device Classes

- Primary: always-on Android tablets used as household displays.
- Secondary: Android TV boxes, TV-like devices, and large screens controlled by D-pad/remote.
- Tertiary: old phones used as desk displays.

### 4.3 Input Requirements

- Touch support for tablets.
- D-pad/remote support for TV usage.
- Keyboard support for easier emulator and TV-box setup.

## 5. User Experience Requirements

### 5.1 Main Display

The main display must show all of the following simultaneously:

- Photo collage background.
- Current time.
- Current date.
- Current weather.
- 5-day forecast.
- Weather data freshness/source state when stale or degraded.

The collage must remain visually dominant, while text panels must stay readable through translucent backgrounds.

### 5.2 Layering

Layer order:

1. Background fill.
2. Photo collage.
3. Optional darkening/tint layer for readability and night mode.
4. Translucent weather/date/time panels.
5. Temporary status/error overlays.

Panels must not permanently obscure the same photo area because the collage and top panels both move over time.

### 5.3 Screensaver Mode

The APK must register a `DreamService` implementation for Android screensaver/daydream usage. In this mode:

- The shared display engine renders the collage, clock, date, and weather.
- Settings are not edited inside the dream surface.
- User interaction should exit or wake according to Android dream behavior.
- The service reads the same persisted configuration as fullscreen mode.

### 5.4 Fullscreen Mode

The APK must include a fullscreen activity for continuous operation. In this mode:

- The screen is kept awake with window flags.
- System UI is hidden where supported.
- The app applies brightness according to the configured schedule.
- User can open settings by touch, D-pad action, or menu key.

### 5.5 Settings

Settings must be built into the APK and work with touch and D-pad.

Required settings for public beta:

- Photo folder path.
- City display name.
- Latitude and longitude for weather requests.
- Weather refresh interval.
- Preferred units: Celsius, m/s or km/h wind, mm precipitation.
- Brightness schedule: day, evening, night time ranges and brightness values.
- Night visual profile: overlay opacity and text brightness.
- Collage behavior: photo change rate, max visible photos, motion intensity.
- Anti-burn-in interval range.
- Enable/launch instructions for screensaver mode.
- Weather provider diagnostics and last successful update.

### 5.6 File Browser

The app must include its own simple file browser instead of relying on a system picker. Requirements:

- Browse visible local storage roots.
- Support internal storage and mounted SD/USB paths when exposed by the device filesystem.
- Save the selected folder path.
- Show readable errors when a folder is missing, inaccessible, or empty.
- Filter image files by supported extensions.

## 6. Weather Requirements

### 6.1 Provider Strategy

Weather providers:

1. Primary: Open-Meteo.
2. Fallback: MET Norway.
3. Optional: WeatherAPI.com when a user supplies an API key and wants ready Russian text descriptions.
4. Optional: OpenWeather when a user supplies an API key and accepts provider limits or payment model.

The public beta must work without any paid provider or API key.

### 6.2 Location

Weather location is configured manually:

- User-facing city name.
- Latitude.
- Longitude.

Automatic location detection is out of scope for public beta.

### 6.3 Displayed Weather Data

Current weather:

- Temperature.
- Feels-like temperature when available or derivable.
- Weather icon.
- Russian text description mapped locally from weather code.
- Precipitation probability or current precipitation when available.
- Wind speed and direction.
- Provider name.
- Last successful update time.

5-day forecast:

- Date/day label.
- Weather icon.
- Russian description.
- Min/max temperature.
- Precipitation probability or precipitation amount.
- Wind summary when available.

### 6.4 Offline And Error Behavior

- Cache the last successful weather response.
- Continue showing cached data with a stale marker when refresh fails.
- Retry on a backoff schedule.
- Never block collage or clock rendering because weather is unavailable.
- Settings diagnostics must show the last error in human-readable form.

## 7. Collage Requirements

### 7.1 Visual Direction

The collage engine must be custom-built and visually inspired by CyanogenMod 11 Photo Table/Collage:

- Multiple photos visible at once.
- Slight rotation and scale variation.
- Overlap with depth ordering.
- Smooth motion and periodic replacement.
- Shadows or subtle borders for separation.
- Continuous, calm movement suitable for long viewing.

### 7.2 Technical Requirements

- Lazy image discovery and metadata caching.
- Downsample large photos before rendering.
- Avoid loading full-resolution images into memory.
- Keep memory usage safe for old Android 4.4 devices.
- Handle folder changes and missing files gracefully.
- Support portrait and landscape screens.
- Keep animation frame work bounded to avoid battery and thermal issues.

### 7.3 Public Beta Acceptance

The collage is acceptable for beta when:

- It can run for 8 hours on a representative Android 4.4 device or emulator without unbounded memory growth.
- It handles at least 500 photos in a folder without startup freezing.
- It continues rendering if individual files are corrupt.
- It remains readable under clock/weather overlays.

## 8. Burn-In Prevention

The app must reduce risk of screen burn-in during continuous use.

Required mechanisms:

- Dynamic photo collage on the bottom layer.
- Bright overlay elements move between safe zones.
- Movement interval configurable in the 5-15 minute range.
- Movement must be smooth, not abrupt.
- Translucent panel opacity can vary slightly over time.
- Night profile reduces brightness and contrast of static UI.

Safe zones must avoid clipping and must preserve readability on tablets and TVs.

## 9. Brightness Management

Public beta brightness management is schedule-based:

- Day, evening, and night periods.
- Per-period brightness levels.
- Smooth transitions between levels.
- Fullscreen mode applies window brightness.
- DreamService applies brightness when platform behavior allows; otherwise it uses the visual night profile.

Roadmap item after beta foundation:

- Optional ambient light sensor mode, only when a device exposes a reliable sensor.

## 10. Architecture

### 10.1 Recommended Architecture

Use one APK with two host modes and one shared rendering/data core.

Hosts:

- `MainActivity`: fullscreen runtime and entry point to settings.
- `WClockDreamService`: Android screensaver/daydream host.

Shared core:

- `ClockWeatherCollageView`: custom composite view used by both hosts.
- `RenderController`: coordinates layers, animation ticks, burn-in movement, and lifecycle.
- `CollageEngine`: image discovery, layout, animation, and bitmap lifecycle.
- `WeatherRepository`: provider selection, caching, refresh, and fallback.
- `SettingsRepository`: persisted configuration.
- `BrightnessController`: schedule evaluation and window brightness application.

This keeps behavior consistent between fullscreen and screensaver modes while allowing each host to handle Android lifecycle differences.

### 10.2 Proposed Package Structure

```text
app/
  src/main/java/com/sstpnk/wclock/
    host/
      MainActivity.java
      WClockDreamService.java
    render/
      ClockWeatherCollageView.java
      RenderController.java
      OverlayLayoutEngine.java
      BurnInController.java
    collage/
      CollageEngine.java
      PhotoScanner.java
      BitmapLoader.java
      PhotoItem.java
      CollageLayout.java
    weather/
      WeatherRepository.java
      WeatherProvider.java
      OpenMeteoProvider.java
      MetNorwayProvider.java
      WeatherApiProvider.java
      OpenWeatherProvider.java
      WeatherCodeMapper.java
      WeatherModels.java
    brightness/
      BrightnessController.java
      BrightnessSchedule.java
      AmbientLightController.java
    settings/
      SettingsActivity.java
      SettingsRepository.java
      FileBrowserActivity.java
    util/
      TimeProvider.java
      NetworkClient.java
      JsonParser.java
      Logger.java
```

### 10.3 Rendering Approach

Use a custom `View` backed by Android Canvas for the first public beta.

Reasons:

- Canvas is available and stable on Android 4.4.
- It avoids WebView and GPU compatibility risks on old devices.
- It gives direct control over bitmap lifecycle and animation.
- It is sufficient for layered 2D collage, panels, and text.

The render loop should be lifecycle-aware:

- Start when host is visible or dreaming.
- Stop when paused/detached.
- Use bounded frame rate appropriate for slow motion.
- Decouple clock/weather data updates from animation ticks.

### 10.4 Persistence

Use `SharedPreferences` for public beta settings:

- Small configuration surface.
- API 19 compatible.
- Easy to backup/export later if needed.

Use file cache for weather responses and photo metadata only if needed after initial profiling. Public beta can start with in-memory metadata plus persisted last weather response.

### 10.5 Networking

Use platform-compatible HTTP client code without Google dependencies.

Requirements:

- Provider-specific request builders.
- Explicit timeouts.
- User-agent for MET Norway.
- Simple JSON parsing.
- Retry/backoff in repository layer.
- Cache last successful result.

### 10.6 Compatibility Risks

Known risks:

- TLS support on old Android 4.4 builds can fail for modern endpoints.
- Device vendors may alter Daydream behavior.
- External SD/USB mount paths vary widely.
- Window brightness behavior may differ in DreamService.
- Very old GPUs may struggle with too many transformed bitmaps.

Mitigations:

- Keep provider layer replaceable.
- Keep fullscreen mode fully functional even if DreamService behavior is vendor-limited.
- Provide manual folder path fallback.
- Add diagnostics screen.
- Tune collage defaults conservatively.

## 11. Testing And Quality Requirements

### 11.1 Automated Tests

Required before public beta:

- Weather code mapping tests.
- Provider response parsing tests using saved fixtures.
- Settings serialization/defaults tests.
- Brightness schedule calculation tests.
- Burn-in safe-zone selection tests.
- Photo scanner filtering tests.

### 11.2 Manual And Device Tests

Required before public beta:

- Android 4.4 emulator smoke test.
- Real Android 4.4 device test if available.
- Tablet landscape and portrait tests.
- TV/D-pad navigation test.
- Fullscreen 8-hour run.
- DreamService activation test.
- Offline weather cache test.
- Empty/missing/corrupt photo folder tests.
- Night schedule transition test.

### 11.3 Beta Exit Criteria

Public beta can ship when:

- APK installs and launches on Android 4.4.
- Fullscreen mode works for 8 hours without crash or unbounded memory growth.
- DreamService renders the shared display.
- User can configure folder, city, coordinates, brightness, and weather refresh.
- Open-Meteo works as primary provider.
- MET Norway fallback works when primary fails in controlled tests.
- Clock/date/weather/forecast/collage are displayed simultaneously.
- Bright overlays move over time.
- No Google Play Services dependency exists in the dependency tree.

## 12. Roadmap To Public Beta

### Phase 0: Repository And Project Foundation

Goal: create a buildable Android project with strict compatibility rules.

Deliverables:

- Git repository with remote `https://github.com/sstpnk/apk-wclock`.
- Gradle Android project.
- Java source set.
- `minSdkVersion 19`.
- Application id `com.sstpnk.wclock`.
- Dependency policy documented: no Google Play Services, no mandatory AndroidX runtime.
- CI or local verification command for build and unit tests.

Acceptance:

- Clean debug APK build.
- Dependency report confirms no Play Services.

### Phase 1: Shared Display Skeleton

Goal: prove the two-host architecture.

Deliverables:

- `MainActivity` fullscreen host.
- `WClockDreamService` host.
- Shared `ClockWeatherCollageView`.
- Static placeholder layers for collage, clock, date, current weather, forecast.
- Basic D-pad/touch entry into settings placeholder.

Acceptance:

- Same display appears in fullscreen and DreamService.
- Android 4.4 emulator can install and launch APK.

### Phase 2: Settings And File Browser

Goal: make the app configurable on tablets and TVs.

Deliverables:

- Settings screen.
- D-pad navigable controls.
- File browser for folder selection.
- City and coordinate settings.
- Persisted defaults.
- Validation errors for missing folder and invalid coordinates.

Acceptance:

- Configuration survives restart.
- Folder can be selected without system picker.

### Phase 3: Weather Core

Goal: provide reliable current weather and 5-day forecast without paid APIs.

Deliverables:

- Weather domain models.
- Open-Meteo provider.
- MET Norway provider.
- Provider fallback logic.
- Local Russian weather code descriptions.
- Weather icons.
- Last-success cache and stale state.
- Diagnostics in settings.
- Unit tests with fixtures.

Acceptance:

- Weather updates by coordinates.
- Forecast displays 5 days.
- Cached weather remains visible offline.

### Phase 4: Collage Engine

Goal: replace placeholder background with high-quality custom collage.

Deliverables:

- Photo scanner.
- Bitmap downsampling loader.
- Collage layout engine.
- Animated photo movement/replacement.
- Corrupt image handling.
- Conservative memory limits.
- Collage tuning settings.

Acceptance:

- 500-photo folder does not freeze startup.
- Collage runs smoothly with bounded memory.
- Corrupt files do not crash the app.

### Phase 5: Burn-In And Brightness

Goal: make continuous use practical.

Deliverables:

- Burn-in safe-zone controller.
- Smooth overlay relocation.
- Configurable movement interval.
- Schedule-based brightness controller.
- Night visual profile.
- Smooth brightness transitions.

Acceptance:

- Overlay positions change over time.
- Brightness follows configured schedule in fullscreen mode.
- Night profile visibly reduces glare.

### Phase 6: TV And Long-Run Hardening

Goal: make beta stable on target hardware.

Deliverables:

- D-pad navigation pass.
- Large-screen layout tuning.
- Lifecycle fixes for sleep/wake/dream transitions.
- Memory profiling and bitmap cleanup.
- Network failure hardening.
- User-facing diagnostics.

Acceptance:

- 8-hour fullscreen run passes.
- DreamService start/stop cycle does not leak or crash.
- Settings remain usable with remote only.

### Phase 7: Public Beta Packaging

Goal: prepare first public beta release.

Deliverables:

- Release build configuration.
- Basic README with install/setup instructions.
- Known limitations.
- Privacy note: no tracking, weather requests use configured coordinates.
- Beta checklist.
- Versioned APK artifact.

Acceptance:

- Public beta APK can be installed manually.
- Setup flow is documented.
- Known limitations match actual behavior.

## 13. Post-Beta Roadmap

- Optional ambient light sensor brightness mode.
- Optional manual provider keys for WeatherAPI.com and OpenWeather.
- Export/import settings.
- More collage themes.
- Locale support beyond Russian.
- Advanced forecast details.
- Device-specific compatibility profiles.
- Optional crash log export.

## 14. Open Decisions

The following decisions are intentionally deferred until implementation planning or device testing:

- Exact Gradle/Android Gradle Plugin version that best balances modern tooling and Android 4.4 output.
- Exact image memory budget defaults for target devices.
- Whether optional AndroidX test libraries are acceptable for local tests when absent from runtime APK.
- Exact icon style for weather conditions.
- Whether settings should be one activity with sections or multiple focused activities.

## 15. Source Notes

Research references used while shaping the document:

- Android `DreamService` API documentation: `https://developer.android.com/reference/android/service/dreams/DreamService`
- Android window brightness API documentation: `https://developer.android.com/reference/android/view/WindowManager.LayoutParams`
- Open-Meteo documentation: `https://open-meteo.com/en/docs`
- Yandex Weather developer page checked and intentionally excluded from requirements.
