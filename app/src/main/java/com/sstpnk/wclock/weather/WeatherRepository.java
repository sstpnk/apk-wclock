package com.sstpnk.wclock.weather;

import com.sstpnk.wclock.util.NetworkClient;

import java.util.ArrayList;
import java.util.List;

public final class WeatherRepository {
    private final NetworkClient networkClient;
    private final List<WeatherProvider> providers = new ArrayList<WeatherProvider>();
    private WeatherData lastSuccessful;
    private String lastError = "";
    private static WeatherData cachedData;
    private static String cachedKey = "";
    private static long cachedAtMillis;

    public WeatherRepository(NetworkClient networkClient, WeatherProvider primary, WeatherProvider fallback, WeatherProvider... extraFallbacks) {
        this.networkClient = networkClient;
        this.providers.add(primary);
        this.providers.add(fallback);
        if (extraFallbacks != null) {
            for (WeatherProvider provider : extraFallbacks) {
                if (provider != null) {
                    this.providers.add(provider);
                }
            }
        }
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
        StringBuilder errors = new StringBuilder();
        for (int i = 0; i < providers.size(); i++) {
            WeatherProvider provider = providers.get(i);
            try {
                WeatherData data = fetch(provider, cityName, latitude, longitude, nowMillis, fetchNetwork);
                lastSuccessful = data;
                lastError = i == 0 ? "" : errors.toString();
                return data;
            } catch (Exception error) {
                if (errors.length() > 0) {
                    errors.append("; ");
                }
                errors.append(provider.name()).append(": ").append(error.getMessage());
            }
        }
        lastError = "Weather failed: " + errors.toString();
        return staleCopy(lastSuccessful);
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
