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
        settings.cityName = prefs.getString("cityName", defaults.cityName);
        settings.latitude = doubleFromPrefs("latitude", defaults.latitude);
        settings.longitude = doubleFromPrefs("longitude", defaults.longitude);
        settings.weatherRefreshMinutes = prefs.getInt("weatherRefreshMinutes", defaults.weatherRefreshMinutes);
        settings.collageEnabled = prefs.getBoolean("collageEnabled", defaults.collageEnabled);
        settings.maxVisiblePhotos = prefs.getInt("maxVisiblePhotos", defaults.maxVisiblePhotos);
        settings.photoChangeSeconds = prefs.getInt("photoChangeSeconds", defaults.photoChangeSeconds);
        settings.locationMode = prefs.getString("locationMode", defaults.locationMode);
        settings.weatherProvider = prefs.getString("weatherProvider", defaults.weatherProvider);
        settings.weatherIconStyle = prefs.getString("weatherIconStyle", defaults.weatherIconStyle);
        settings.weatherApiKey = prefs.getString("weatherApiKey", defaults.weatherApiKey);
        settings.openWeatherApiKey = prefs.getString("openWeatherApiKey", defaults.openWeatherApiKey);
        settings.showSeconds = prefs.getBoolean("showSeconds", defaults.showSeconds);
        settings.motionIntensity = prefs.getFloat("motionIntensity", defaults.motionIntensity);
        settings.burnInMinMinutes = prefs.getInt("burnInMinMinutes", defaults.burnInMinMinutes);
        settings.burnInMaxMinutes = prefs.getInt("burnInMaxMinutes", defaults.burnInMaxMinutes);
        settings.dayBrightness = prefs.getFloat("dayBrightness", defaults.dayBrightness);
        settings.eveningBrightness = prefs.getFloat("eveningBrightness", defaults.eveningBrightness);
        settings.nightBrightness = prefs.getFloat("nightBrightness", defaults.nightBrightness);
        settings.nightOverlayAlpha = prefs.getFloat("nightOverlayAlpha", defaults.nightOverlayAlpha);
        return settings.normalized();
    }

    public void save(Settings settings) {
        Settings safe = settings.normalized();
        prefs.edit()
                .putString("photoFolderPath", safe.photoFolderPath)
                .putString("cityName", safe.cityName)
                .putString("latitude", Double.toString(safe.latitude))
                .putString("longitude", Double.toString(safe.longitude))
                .putInt("weatherRefreshMinutes", safe.weatherRefreshMinutes)
                .putBoolean("collageEnabled", safe.collageEnabled)
                .putInt("maxVisiblePhotos", safe.maxVisiblePhotos)
                .putInt("photoChangeSeconds", safe.photoChangeSeconds)
                .putString("locationMode", safe.locationMode)
                .putString("weatherProvider", safe.weatherProvider)
                .putString("weatherIconStyle", safe.weatherIconStyle)
                .putString("weatherApiKey", safe.weatherApiKey)
                .putString("openWeatherApiKey", safe.openWeatherApiKey)
                .putBoolean("showSeconds", safe.showSeconds)
                .putFloat("motionIntensity", safe.motionIntensity)
                .putInt("burnInMinMinutes", safe.burnInMinMinutes)
                .putInt("burnInMaxMinutes", safe.burnInMaxMinutes)
                .putFloat("dayBrightness", safe.dayBrightness)
                .putFloat("eveningBrightness", safe.eveningBrightness)
                .putFloat("nightBrightness", safe.nightBrightness)
                .putFloat("nightOverlayAlpha", safe.nightOverlayAlpha)
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

    static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class Settings {
        public String photoFolderPath;
        public String cityName;
        public double latitude;
        public double longitude;
        public int weatherRefreshMinutes;
        public boolean collageEnabled;
        public int maxVisiblePhotos;
        public int photoChangeSeconds;
        public String locationMode;
        public String weatherProvider;
        public String weatherIconStyle;
        public String weatherApiKey;
        public String openWeatherApiKey;
        public boolean showSeconds;
        public float motionIntensity;
        public int burnInMinMinutes;
        public int burnInMaxMinutes;
        public float dayBrightness;
        public float eveningBrightness;
        public float nightBrightness;
        public float nightOverlayAlpha;

        public static Settings defaults() {
            Settings settings = new Settings();
            settings.photoFolderPath = "";
            settings.cityName = "Москва";
            settings.latitude = 55.7558;
            settings.longitude = 37.6173;
            settings.weatherRefreshMinutes = 30;
            settings.collageEnabled = true;
            settings.maxVisiblePhotos = 18;
            settings.photoChangeSeconds = 5;
            settings.locationMode = "coordinates";
            settings.weatherProvider = "open-meteo";
            settings.weatherIconStyle = "outline";
            settings.weatherApiKey = "";
            settings.openWeatherApiKey = "";
            settings.showSeconds = false;
            settings.motionIntensity = 0.35f;
            settings.burnInMinMinutes = 5;
            settings.burnInMaxMinutes = 15;
            settings.dayBrightness = 0.85f;
            settings.eveningBrightness = 0.45f;
            settings.nightBrightness = 0.12f;
            settings.nightOverlayAlpha = 0.45f;
            return settings;
        }

        Settings normalized() {
            Settings safe = new Settings();
            safe.photoFolderPath = photoFolderPath == null ? "" : photoFolderPath;
            safe.cityName = cityName == null || cityName.trim().length() == 0 ? "Москва" : cityName.trim();
            safe.latitude = isValidLatitude(latitude) ? latitude : 55.7558;
            safe.longitude = isValidLongitude(longitude) ? longitude : 37.6173;
            safe.weatherRefreshMinutes = clampInt(weatherRefreshMinutes, 15, 720);
            safe.collageEnabled = collageEnabled;
            safe.maxVisiblePhotos = clampInt(maxVisiblePhotos, 1, 50);
            safe.photoChangeSeconds = clampInt(photoChangeSeconds, 1, 60);
            safe.locationMode = normalizeLocationMode(locationMode);
            safe.weatherProvider = normalizeProvider(weatherProvider);
            safe.weatherIconStyle = normalizeIconStyle(weatherIconStyle);
            safe.weatherApiKey = weatherApiKey == null ? "" : weatherApiKey.trim();
            safe.openWeatherApiKey = openWeatherApiKey == null ? "" : openWeatherApiKey.trim();
            safe.showSeconds = showSeconds;
            safe.motionIntensity = clampFloat(motionIntensity, 0.0f, 1.0f);
            safe.burnInMinMinutes = clampInt(burnInMinMinutes, 5, 15);
            safe.burnInMaxMinutes = clampInt(burnInMaxMinutes, safe.burnInMinMinutes, 15);
            safe.dayBrightness = clampFloat(dayBrightness, 0.05f, 1.0f);
            safe.eveningBrightness = clampFloat(eveningBrightness, 0.05f, 1.0f);
            safe.nightBrightness = clampFloat(nightBrightness, 0.02f, 1.0f);
            safe.nightOverlayAlpha = clampFloat(nightOverlayAlpha, 0.0f, 0.85f);
            return safe;
        }
    }

    private static String normalizeProvider(String provider) {
        if (provider == null) {
            return "open-meteo";
        }
        String value = provider.trim().toLowerCase();
        if ("met-norway".equals(value) || "weatherapi".equals(value) || "openweather".equals(value)) {
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
