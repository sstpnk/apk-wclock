package com.sstpnk.wclock.weather;

import com.sstpnk.wclock.util.NetworkClient;

public final class WeatherRepository {
    private final NetworkClient networkClient;
    private final WeatherProvider primary;
    private final WeatherProvider fallback;
    private WeatherData lastSuccessful;
    private String lastError = "";
    private static WeatherData cachedData;
    private static String cachedKey = "";
    private static long cachedAtMillis;

    public WeatherRepository(NetworkClient networkClient, WeatherProvider primary, WeatherProvider fallback) {
        this.networkClient = networkClient;
        this.primary = primary;
        this.fallback = fallback;
    }

    public WeatherData refresh(String cityName, double latitude, double longitude, long nowMillis) {
        return tryProviders(cityName, latitude, longitude, nowMillis, true);
    }

    public WeatherData refreshCached(String cityName, double latitude, double longitude, long nowMillis, long refreshIntervalMillis, String cacheKey) {
        if (cachedData != null && cacheKey.equals(cachedKey) && nowMillis - cachedAtMillis < refreshIntervalMillis) {
            lastSuccessful = cachedData;
            lastError = "";
            return cachedData;
        }
        WeatherData data = tryProviders(cityName, latitude, longitude, nowMillis, true);
        if (data != null && !data.stale) {
            cachedData = data;
            cachedKey = cacheKey;
            cachedAtMillis = nowMillis;
        }
        return data;
    }

    public WeatherData refreshForTest(String cityName, double latitude, double longitude, long nowMillis) {
        return tryProviders(cityName, latitude, longitude, nowMillis, false);
    }

    public WeatherData lastSuccessful() {
        return lastSuccessful;
    }

    public String lastError() {
        return lastError;
    }

    private WeatherData tryProviders(String cityName, double latitude, double longitude, long nowMillis, boolean fetchNetwork) {
        try {
            WeatherData data = fetch(primary, cityName, latitude, longitude, nowMillis, fetchNetwork);
            lastSuccessful = data;
            lastError = "";
            return data;
        } catch (Exception primaryError) {
            try {
                WeatherData data = fetch(fallback, cityName, latitude, longitude, nowMillis, fetchNetwork);
                lastSuccessful = data;
                lastError = "Primary failed: " + primaryError.getMessage();
                return data;
            } catch (Exception fallbackError) {
                lastError = "Weather failed: " + primaryError.getMessage() + "; fallback: " + fallbackError.getMessage();
                return staleCopy(lastSuccessful);
            }
        }
    }

    private WeatherData fetch(WeatherProvider provider, String cityName, double latitude, double longitude, long nowMillis, boolean fetchNetwork) throws Exception {
        String body = fetchNetwork ? networkClient.get(provider.buildUrl(latitude, longitude)) : "";
        return provider.parse(cityName, body, nowMillis);
    }

    private static WeatherData staleCopy(WeatherData data) {
        if (data == null) {
            return null;
        }
        return new WeatherData(data.providerName, data.cityName, data.updatedAtMillis, true, data.temperatureC, data.feelsLikeC, data.weatherCode, data.descriptionRu, data.precipitationMm, data.windSpeed, data.windDirection, data.forecast);
    }
}
