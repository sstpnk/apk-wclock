# Changelog

## v0.1.1 - First test release

### Fixed

- Align clock and weather panels to the same lower edge in landscape mode.
- Reduce excess clock panel padding.
- Increase spacing between weather detail lines.
- Group MET Norway hourly forecast data into distinct forecast days.
- Add legacy Android TLS trust support for current MET Norway certificates.

### Changed

- Publish signed release APK builds with resource shrink and R8 minification.

## v0.1.0 - Public beta

Published as the initial signed GitHub Release.

### Added

- Android 4.4 compatible full-screen clock app.
- Android Daydream/screensaver service.
- Local photo wall mode with animated entry, rotation, frame, and fade-out.
- Local photo frame mode with cover-fit scaling and pan across oversized images.
- Current weather and five-day forecast overlay.
- Open-Meteo and MET Norway providers without API keys.
- Optional WeatherAPI.com and OpenWeather providers.
- Manual location settings with city label and coordinates.
- Weather refresh interval setting.
- Burn-in protection through periodic overlay position shifts.
- Schedule-based brightness control.
- Build number display in settings.
- Unit tests for rendering, weather parsing, settings normalization, and brightness schedule.

### Known Limitations

- Weather UI and settings are intentionally minimal for the first public beta.
