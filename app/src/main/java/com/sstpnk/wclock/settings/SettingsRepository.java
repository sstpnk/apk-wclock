package com.sstpnk.wclock.settings;

import android.content.Context;
import android.content.SharedPreferences;

public final class SettingsRepository {
    private static final String PREFS = "wclock_settings";

    private final SharedPreferences prefs;

    public SettingsRepository(Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public Settings load() {
        Settings defaults = Settings.defaults();
        Settings settings = new Settings();
        settings.photoFolderPath = prefs.getString("photoFolderPath", defaults.photoFolderPath);
        settings.photoFolderUri = prefs.getString("photoFolderUri", defaults.photoFolderUri);
        settings.cityName = prefs.getString("cityName", defaults.cityName);
        settings.latitude = doubleFromPrefs("latitude", defaults.latitude);
        settings.longitude = doubleFromPrefs("longitude", defaults.longitude);
        settings.weatherRefreshMinutes = prefs.getInt("weatherRefreshMinutes", defaults.weatherRefreshMinutes);
        settings.collageEnabled = prefs.getBoolean("collageEnabled", defaults.collageEnabled);
        settings.showClock = prefs.getBoolean("showClock", defaults.showClock);
        settings.showWeather = prefs.getBoolean("showWeather", defaults.showWeather);
        settings.showForecast = prefs.getBoolean("showForecast", defaults.showForecast);
        settings.photoDisplayMode = prefs.getString("photoDisplayMode", defaults.photoDisplayMode);
        settings.photoOrderMode = prefs.getString("photoOrderMode", defaults.photoOrderMode);
        settings.maxVisiblePhotos = prefs.getInt("maxVisiblePhotos", defaults.maxVisiblePhotos);
        settings.photoChangeSeconds = prefs.getInt("photoChangeSeconds", defaults.photoChangeSeconds);
        settings.framePanSpeedPxPerSecond = prefs.getInt("framePanSpeedPxPerSecond", defaults.framePanSpeedPxPerSecond);
        settings.locationMode = prefs.getString("locationMode", defaults.locationMode);
        settings.weatherProvider = prefs.getString("weatherProvider", defaults.weatherProvider);
        settings.weatherIconStyle = prefs.getString("weatherIconStyle", defaults.weatherIconStyle);
        settings.weatherApiKey = prefs.getString("weatherApiKey", defaults.weatherApiKey);
        settings.openWeatherApiKey = prefs.getString("openWeatherApiKey", defaults.openWeatherApiKey);
        settings.showSeconds = prefs.getBoolean("showSeconds", defaults.showSeconds);
        settings.burnInMinMinutes = prefs.getInt("burnInMinMinutes", defaults.burnInMinMinutes);
        settings.dayBrightness = prefs.getFloat("dayBrightness", defaults.dayBrightness);
        settings.eveningBrightness = prefs.getFloat("eveningBrightness", defaults.eveningBrightness);
        settings.nightBrightness = prefs.getFloat("nightBrightness", defaults.nightBrightness);
        settings.autoBrightnessEnabled = prefs.getBoolean("autoBrightnessEnabled", defaults.autoBrightnessEnabled);
        settings.autoBrightnessMin = prefs.getFloat("autoBrightnessMin", defaults.autoBrightnessMin);
        settings.autoBrightnessMax = prefs.getFloat("autoBrightnessMax", defaults.autoBrightnessMax);

        float legacyPanelAlpha = prefs.getFloat("panelBackgroundAlpha", defaults.clockPanelBackgroundAlpha);
        settings.clockPanelBackgroundAlpha = prefs.getFloat("clockPanelBackgroundAlpha", legacyPanelAlpha);
        settings.weatherPanelBackgroundAlpha = prefs.getFloat("weatherPanelBackgroundAlpha", legacyPanelAlpha);
        return settings.normalized();
    }

    public void save(Settings settings) {
        Settings safe = settings.normalized();
        prefs.edit()
                .putString("photoFolderPath", safe.photoFolderPath)
                .putString("photoFolderUri", safe.photoFolderUri)
                .putString("cityName", safe.cityName)
                .putString("latitude", Double.toString(safe.latitude))
                .putString("longitude", Double.toString(safe.longitude))
                .putInt("weatherRefreshMinutes", safe.weatherRefreshMinutes)
                .putBoolean("collageEnabled", safe.collageEnabled)
                .putBoolean("showClock", safe.showClock)
                .putBoolean("showWeather", safe.showWeather)
                .putBoolean("showForecast", safe.showForecast)
                .putString("photoDisplayMode", safe.photoDisplayMode)
                .putString("photoOrderMode", safe.photoOrderMode)
                .putInt("maxVisiblePhotos", safe.maxVisiblePhotos)
                .putInt("photoChangeSeconds", safe.photoChangeSeconds)
                .putInt("framePanSpeedPxPerSecond", safe.framePanSpeedPxPerSecond)
                .putString("locationMode", safe.locationMode)
                .putString("weatherProvider", safe.weatherProvider)
                .putString("weatherIconStyle", safe.weatherIconStyle)
                .putString("weatherApiKey", safe.weatherApiKey)
                .putString("openWeatherApiKey", safe.openWeatherApiKey)
                .putBoolean("showSeconds", safe.showSeconds)
                .putInt("burnInMinMinutes", safe.burnInMinMinutes)
                .putFloat("dayBrightness", safe.dayBrightness)
                .putFloat("eveningBrightness", safe.eveningBrightness)
                .putFloat("nightBrightness", safe.nightBrightness)
                .putBoolean("autoBrightnessEnabled", safe.autoBrightnessEnabled)
                .putFloat("autoBrightnessMin", safe.autoBrightnessMin)
                .putFloat("autoBrightnessMax", safe.autoBrightnessMax)

                .putFloat("clockPanelBackgroundAlpha", safe.clockPanelBackgroundAlpha)
                .putFloat("weatherPanelBackgroundAlpha", safe.weatherPanelBackgroundAlpha)
                .apply();
    }

    private double doubleFromPrefs(String key, double defaultValue) {
        try {
            return Double.parseDouble(prefs.getString(key, Double.toString(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean isValidLatitude(double value) {
        return value >= -90.0 && value <= 90.0;
    }

    public static boolean isValidLongitude(double value) {
        return value >= -180.0 && value <= 180.0;
    }

    static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int recommendedMaxVisiblePhotos(long maxMemoryBytes) {
        if (maxMemoryBytes <= 64L * 1024L * 1024L) {
            return 8;
        }
        if (maxMemoryBytes <= 128L * 1024L * 1024L) {
            return 12;
        }
        if (maxMemoryBytes <= 192L * 1024L * 1024L) {
            return 18;
        }
        return 50;
    }

    static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class Settings {
        public String photoFolderPath;
        public String photoFolderUri;
        public String cityName;
        public double latitude;
        public double longitude;
        public int weatherRefreshMinutes;
        public boolean collageEnabled;
        public boolean showClock;
        public boolean showWeather;
        public boolean showForecast;
        public String photoDisplayMode;
        public String photoOrderMode;
        public int maxVisiblePhotos;
        public int photoChangeSeconds;
        public int framePanSpeedPxPerSecond;
        public String locationMode;
        public String weatherProvider;
        public String weatherIconStyle;
        public String weatherApiKey;
        public String openWeatherApiKey;
        public boolean showSeconds;
        public int burnInMinMinutes;
        public float dayBrightness;
        public float eveningBrightness;
        public float nightBrightness;
        public boolean autoBrightnessEnabled;
        public float autoBrightnessMin;
        public float autoBrightnessMax;
        public float clockPanelBackgroundAlpha;
        public float weatherPanelBackgroundAlpha;
        public static Settings defaults() {
            Settings settings = new Settings();
            settings.photoFolderPath = "";
            settings.photoFolderUri = "";
            settings.cityName = "Москва";
            settings.latitude = 55.7558;
            settings.longitude = 37.6173;
            settings.weatherRefreshMinutes = 30;
            settings.collageEnabled = true;
            settings.showClock = true;
            settings.showWeather = true;
            settings.showForecast = true;
            settings.photoDisplayMode = "photowall";
            settings.photoOrderMode = "random";
            settings.maxVisiblePhotos = 18;
            settings.photoChangeSeconds = 5;
            settings.framePanSpeedPxPerSecond = 20;
            settings.locationMode = "coordinates";
            settings.weatherProvider = "open-meteo";
            settings.weatherIconStyle = "outline";
            settings.weatherApiKey = "";
            settings.openWeatherApiKey = "";
            settings.showSeconds = false;
            settings.burnInMinMinutes = 5;
            settings.dayBrightness = 0.85f;
            settings.eveningBrightness = 0.45f;
            settings.nightBrightness = 0.12f;
            settings.autoBrightnessEnabled = true;
            settings.autoBrightnessMin = 0.08f;
            settings.autoBrightnessMax = 0.90f;
            settings.clockPanelBackgroundAlpha = 0.56f;
            settings.weatherPanelBackgroundAlpha = 0.56f;
            return settings;
        }

        Settings normalized() {
            Settings safe = new Settings();
            safe.photoFolderPath = photoFolderPath == null ? "" : photoFolderPath;
            safe.photoFolderUri = photoFolderUri == null ? "" : photoFolderUri;
            safe.cityName = cityName == null || cityName.trim().length() == 0 ? "Москва" : cityName.trim();
            safe.latitude = isValidLatitude(latitude) ? latitude : 55.7558;
            safe.longitude = isValidLongitude(longitude) ? longitude : 37.6173;
            safe.weatherRefreshMinutes = clampInt(weatherRefreshMinutes, 15, 720);
            safe.collageEnabled = collageEnabled;
            safe.showClock = showClock;
            safe.showWeather = showWeather;
            safe.showForecast = showForecast;
            safe.photoDisplayMode = normalizePhotoDisplayMode(photoDisplayMode);
            safe.photoOrderMode = normalizePhotoOrderMode(photoOrderMode);
            safe.maxVisiblePhotos = clampInt(maxVisiblePhotos, 1, recommendedMaxVisiblePhotos(Runtime.getRuntime().maxMemory()));
            safe.photoChangeSeconds = clampInt(photoChangeSeconds, 1, 60);
            safe.framePanSpeedPxPerSecond = clampInt(framePanSpeedPxPerSecond, 4, 48);
            safe.locationMode = normalizeLocationMode(locationMode);
            safe.weatherProvider = normalizeProvider(weatherProvider);
            safe.weatherIconStyle = normalizeIconStyle(weatherIconStyle);
            safe.weatherApiKey = weatherApiKey == null ? "" : weatherApiKey.trim();
            safe.openWeatherApiKey = openWeatherApiKey == null ? "" : openWeatherApiKey.trim();
            safe.showSeconds = showSeconds;
            safe.burnInMinMinutes = clampInt(burnInMinMinutes, 5, 15);
            safe.dayBrightness = clampFloat(dayBrightness, 0.05f, 1.0f);
            safe.eveningBrightness = clampFloat(eveningBrightness, 0.05f, 1.0f);
            safe.nightBrightness = clampFloat(nightBrightness, 0.02f, 1.0f);
            safe.autoBrightnessEnabled = autoBrightnessEnabled;
            safe.autoBrightnessMin = clampFloat(Math.min(autoBrightnessMin, autoBrightnessMax), 0.05f, 1.0f);
            safe.autoBrightnessMax = clampFloat(Math.max(autoBrightnessMin, autoBrightnessMax), safe.autoBrightnessMin, 1.0f);
            safe.clockPanelBackgroundAlpha = clampFloat(clockPanelBackgroundAlpha, 0.0f, 0.85f);
            safe.weatherPanelBackgroundAlpha = clampFloat(weatherPanelBackgroundAlpha, 0.0f, 0.85f);
            return safe;
        }
    }

    private static String normalizeProvider(String provider) {
        if (provider == null) {
            return "open-meteo";
        }
        String value = provider.trim().toLowerCase();
        if ("weatherapi".equals(value) || "openweather".equals(value)) {
            return value;
        }
        return "open-meteo";
    }

    private static String normalizeLocationMode(String mode) {
        if (mode == null) {
            return "coordinates";
        }
        String value = mode.trim().toLowerCase();
        if ("city".equals(value)) {
            return value;
        }
        return "coordinates";
    }

    private static String normalizePhotoDisplayMode(String mode) {
        if (mode == null) {
            return "photowall";
        }
        String value = mode.trim().toLowerCase();
        if ("frame".equals(value)) {
            return value;
        }
        return "photowall";
    }

    private static String normalizePhotoOrderMode(String mode) {
        if (mode == null) {
            return "random";
        }
        String value = mode.trim().toLowerCase();
        if ("sequential".equals(value)) {
            return value;
        }
        return "random";
    }

    private static String normalizeIconStyle(String style) {
        if (style == null) {
            return "outline";
        }
        String value = style.trim().toLowerCase();
        if ("flat".equals(value)) {
            return value;
        }
        return "outline";
    }
}
