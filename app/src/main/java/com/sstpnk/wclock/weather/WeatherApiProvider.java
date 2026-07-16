package com.sstpnk.wclock.weather;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WeatherApiProvider implements WeatherProvider {
    private final String apiKey;

    public WeatherApiProvider(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public String name() {
        return "WeatherAPI.com";
    }

    @Override
    public String buildUrl(double latitude, double longitude) {
        if (apiKey.length() == 0) {
            throw new IllegalStateException("WeatherAPI.com key is empty");
        }
        return String.format(Locale.US,
                "https://api.weatherapi.com/v1/forecast.json?key=%s&q=%.5f,%.5f&days=6&aqi=no&alerts=no&lang=ru",
                apiKey, latitude, longitude);
    }

    @Override
    public WeatherData parse(String cityName, String body, long updatedAtMillis) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONObject current = root.getJSONObject("current");
        JSONArray days = root.getJSONObject("forecast").getJSONArray("forecastday");
        List<ForecastDay> forecast = new ArrayList<ForecastDay>();
        int count = Math.min(5, days.length());
        for (int i = 0; i < count; i++) {
            JSONObject dayRoot = days.getJSONObject(i);
            JSONObject day = dayRoot.getJSONObject("day");
            int code = mapConditionCode(day.getJSONObject("condition").optInt("code", 1003));
            forecast.add(new ForecastDay(
                    dayRoot.getString("date"),
                    code,
                    day.getDouble("mintemp_c"),
                    day.getDouble("maxtemp_c"),
                    day.optInt("daily_chance_of_rain", 0)));
        }
        int currentCode = mapConditionCode(current.getJSONObject("condition").optInt("code", 1003));
        WeatherData data = new WeatherData(
                "WeatherAPI.com",
                cityName,
                updatedAtMillis,
                false,
                current.getDouble("temp_c"),
                currentCode,
                current.getJSONObject("condition").optString("text", WeatherCodeMapper.openMeteoDescription(currentCode)),
                forecast);
        if (forecast.size() > 0) {
            data.todayMinTempC = forecast.get(0).minTempC;
            data.todayMaxTempC = forecast.get(0).maxTempC;
            data.precipitationProbability = forecast.get(0).precipitationProbability;
        }
        data.humidityPercent = current.optInt("humidity", 0);
        data.pressureHpa = current.optDouble("pressure_mb", 0.0);
        return data;
    }

    private int mapConditionCode(int code) {
        if (code == 1000) return 0;
        if (code == 1030 || code == 1135 || code == 1147) return 45;
        if (code >= 1063 && code <= 1201) return 63;
        if (code >= 1204 && code <= 1237) return 75;
        if (code >= 1273) return 95;
        return 2;
    }
}
