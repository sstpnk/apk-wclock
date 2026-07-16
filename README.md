# WClock

WClock — Android-приложение для настольных часов, фотозаставки и погоды.
Рассчитано на планшет или телефон, который постоянно стоит на столе: показывает крупное время, локальные фотографии и актуальную погоду без Google Play Services, аккаунтов, облачной синхронизации и аналитики.

WClock is an Android clock, photo frame and weather display app.
Designed for a tablet or phone that sits on a desk: shows large time, local photos and current weather — no Google Play Services, accounts, cloud sync, or analytics.

---

## Возможности / Features

**Часы / Clock**
- Крупное время и дата / Large time and date display
- Опциональные секунды / Optional seconds
- Полноэкранный / Fullscreen
- Защита от выгорания (сдвиг подложек) / Burn-in mitigation

**Фотографии / Photos**
- Выбор локальной папки / Local folder picker
- Фотостена (несколько фото одновременно + поворот) / Photo wall (multiple photos + rotation)
- Фоторамка (одно фото на весь фон + панорамирование) / Photo frame (single photo + pan)
- Случайный или последовательный порядок / Random or sequential order
- EXIF-ориентация / EXIF orientation support

**Погода / Weather**
- Температура, описание, влажность, давление / Temperature, description, humidity, pressure
- Прогноз на 5 дней / 5-day forecast
- Провайдеры: Open-Meteo, MET Norway, wttr.in, WeatherAPI.com, OpenWeather
- Fallback между провайдерами при ошибке / Provider fallback on error
- Legacy TLS для Android 4.4

**Яркость / Brightness**
- Автояркость по датчику освещённости / Ambient light sensor
- Расписание день/вечер/ночь / Day/evening/night schedule

## Установка / Installation

1. Скачайте APK из [GitHub Releases](https://github.com/sstpnk/apk-wclock/releases) / Download APK from GitHub Releases
2. Установите на устройство / Install on device
3. Откройте WClock и настройте папку с фото и погоду / Open WClock and configure photo folder and weather

Минимальная версия Android: **4.4 KitKat (minSdk 19)**
Minimum Android version: **4.4 KitKat (minSdk 19)**

## Права / Permissions

- `INTERNET` — загрузка погоды / weather fetching
- `ACCESS_NETWORK_STATE` — проверка сети / network check
- `READ_EXTERNAL_STORAGE` (SDK < 33) / `READ_MEDIA_IMAGES` (SDK >= 33) — чтение фото / photo reading
- `BIND_DREAM_SERVICE` — системная заставка / system screensaver

## Лицензия / License

[MIT](LICENSE)
