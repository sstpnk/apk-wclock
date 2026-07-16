package com.sstpnk.wclock.weather;

import com.sstpnk.wclock.settings.SettingsRepository;
import com.sstpnk.wclock.util.NetworkClient;

import java.util.ArrayList;
import java.util.List;

public final class WeatherRepository {
    private final NetworkClient networkClient;
    private final List<WeatherProvider> providers = new ArrayList<WeatherProvider>();
    private WeatherData lastSuccessful;
    private String lastError = "";
    private String lastDiagnostics = "";
    private static String lastDiagnosticsText = "";
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

    public static WeatherRepository create(SettingsRepository.Settings settings) {
        NetworkClient networkClient = new NetworkClient("WClock/0.1 contact: github.com/sstpnk/apk-wclock", 10000);
        if ("met-norway".equals(settings.weatherProvider)) {
            return new WeatherRepository(networkClient, new MetNorwayProvider(), new OpenMeteoProvider(), new WttrInProvider());
        }
        if ("weatherapi".equals(settings.weatherProvider)) {
            return new WeatherRepository(networkClient, new WeatherApiProvider(settings.weatherApiKey), new OpenMeteoProvider(), new WttrInProvider());
        }
        if ("openweather".equals(settings.weatherProvider)) {
            return new WeatherRepository(networkClient, new OpenWeatherProvider(settings.openWeatherApiKey), new OpenMeteoProvider(), new WttrInProvider());
        }
        if ("wttr-in".equals(settings.weatherProvider)) {
            return new WeatherRepository(networkClient, new WttrInProvider(), new OpenMeteoProvider(), new MetNorwayProvider());
        }
        return new WeatherRepository(networkClient, new OpenMeteoProvider(), new MetNorwayProvider(), new WttrInProvider());
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

    public String lastError() {
        return lastError;
    }

    public String lastDiagnostics() {
        return lastDiagnostics;
    }

    public static String lastDiagnosticsText() {
        return lastDiagnosticsText;
    }

    private WeatherData tryProviders(String cityName, double latitude, double longitude, long nowMillis, boolean fetchNetwork) {
        StringBuilder errors = new StringBuilder();
        StringBuilder diagnostics = new StringBuilder();
        for (int i = 0; i < providers.size(); i++) {
            WeatherProvider provider = providers.get(i);
            try {
                WeatherData data = fetch(provider, cityName, latitude, longitude, nowMillis, fetchNetwork);
                lastSuccessful = data;
                lastError = i == 0 ? "" : errors.toString();
                appendDiagnostic(diagnostics, provider.name(), "OK");
                rememberDiagnostics(diagnostics.toString());
                return data;
            } catch (Exception error) {
                appendDiagnostic(diagnostics, provider.name(), errorText(error));
                if (errors.length() > 0) {
                    errors.append("; ");
                }
                errors.append(provider.name()).append(": ").append(errorText(error));
            }
        }
        lastError = "Weather failed: " + errors.toString();
        rememberDiagnostics(diagnostics.toString());
        return staleCopy(lastSuccessful);
    }

    private void rememberDiagnostics(String value) {
        lastDiagnostics = value == null ? "" : value;
        lastDiagnosticsText = lastDiagnostics;
    }

    private static void appendDiagnostic(StringBuilder builder, String providerName, String result) {
        if (builder.length() > 0) {
            builder.append("; ");
        }
        builder.append(providerName).append(": ").append(result == null || result.length() == 0 ? "error" : result);
    }

    private static String errorText(Exception error) {
        if (error == null) {
            return "error";
        }
        String message = error.getMessage();
        if (message != null && message.trim().length() > 0) {
            return message.trim();
        }
        return error.getClass().getSimpleName();
    }

    private WeatherData fetch(WeatherProvider provider, String cityName, double latitude, double longitude, long nowMillis, boolean fetchNetwork) throws Exception {
        String body = fetchNetwork ? networkClient.get(provider.buildUrl(latitude, longitude)) : "";
        return provider.parse(cityName, body, nowMillis);
    }

    private static WeatherData staleCopy(WeatherData data) {
        if (data == null) {
            return null;
        }
        return new WeatherData(data.providerName, data.cityName, data.updatedAtMillis, true, data.temperatureC, data.weatherCode, data.descriptionRu, data.forecast);
    }
}
