package com.sstpnk.wclock.weather;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OpenMeteoProvider implements WeatherProvider {
    @Override
    public String name() {
        return "Open-Meteo";
    }

    @Override
    public String buildUrl(double latitude, double longitude) {
        return String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.5f&longitude=%.5f&current=temperature_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,wind_direction_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max&timezone=auto&forecast_days=5",
                latitude, longitude);
    }

    @Override
    public WeatherData parse(String cityName, String body, long updatedAtMillis) throws Exception {
        return parseBody(cityName, body, updatedAtMillis);
    }

    public static WeatherData parseBody(String cityName, String body, long updatedAtMillis) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONObject current = root.getJSONObject("current");
        int currentCode = current.getInt("weather_code");
        List<ForecastDay> forecast = new ArrayList<ForecastDay>();
        JSONObject daily = root.getJSONObject("daily");
        JSONArray dates = daily.getJSONArray("time");
        JSONArray codes = daily.getJSONArray("weather_code");
        JSONArray maxTemps = daily.getJSONArray("temperature_2m_max");
        JSONArray minTemps = daily.getJSONArray("temperature_2m_min");
        JSONArray precipitation = daily.getJSONArray("precipitation_probability_max");
        JSONArray wind = daily.getJSONArray("wind_speed_10m_max");
        int count = Math.min(5, dates.length());
        for (int i = 0; i < count; i++) {
            int code = codes.getInt(i);
            forecast.add(new ForecastDay(
                    dates.getString(i),
                    code,
                    WeatherCodeMapper.openMeteoDescription(code),
                    minTemps.getDouble(i),
                    maxTemps.getDouble(i),
                    precipitation.optInt(i, 0),
                    wind.optDouble(i, 0.0)));
        }
        return new WeatherData(
                "Open-Meteo",
                cityName,
                updatedAtMillis,
                false,
                current.getDouble("temperature_2m"),
                current.optDouble("apparent_temperature", current.getDouble("temperature_2m")),
                currentCode,
                WeatherCodeMapper.openMeteoDescription(currentCode),
                current.optDouble("precipitation", 0.0),
                current.optDouble("wind_speed_10m", 0.0),
                current.optInt("wind_direction_10m", 0),
                forecast);
    }
}
