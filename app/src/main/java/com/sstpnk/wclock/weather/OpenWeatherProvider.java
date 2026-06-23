package com.sstpnk.wclock.weather;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OpenWeatherProvider implements WeatherProvider {
    private final String apiKey;

    public OpenWeatherProvider(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public String name() {
        return "OpenWeather";
    }

    @Override
    public String buildUrl(double latitude, double longitude) {
        if (apiKey.length() == 0) {
            throw new IllegalStateException("OpenWeather key is empty");
        }
        return String.format(Locale.US,
                "https://api.openweathermap.org/data/3.0/onecall?lat=%.5f&lon=%.5f&appid=%s&units=metric&lang=ru",
                latitude, longitude, apiKey);
    }

    @Override
    public WeatherData parse(String cityName, String body, long updatedAtMillis) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONObject current = root.getJSONObject("current");
        JSONObject currentWeather = current.getJSONArray("weather").getJSONObject(0);
        JSONArray daily = root.getJSONArray("daily");
        List<ForecastDay> forecast = new ArrayList<ForecastDay>();
        int count = Math.min(5, daily.length());
        for (int i = 0; i < count; i++) {
            JSONObject day = daily.getJSONObject(i);
            JSONObject temp = day.getJSONObject("temp");
            JSONObject weather = day.getJSONArray("weather").getJSONObject(0);
            int code = mapWeatherId(weather.optInt("id", 800));
            forecast.add(new ForecastDay(
                    Long.toString(day.optLong("dt", 0L)),
                    code,
                    weather.optString("description", WeatherCodeMapper.openMeteoDescription(code)),
                    temp.optDouble("night", temp.optDouble("min", 0.0)),
                    temp.optDouble("day", temp.optDouble("max", 0.0)),
                    (int) Math.round(day.optDouble("pop", 0.0) * 100.0),
                    day.optDouble("wind_speed", 0.0)));
        }
        int currentCode = mapWeatherId(currentWeather.optInt("id", 800));
        WeatherData data = new WeatherData(
                "OpenWeather",
                cityName,
                updatedAtMillis,
                false,
                current.getDouble("temp"),
                current.optDouble("feels_like", current.getDouble("temp")),
                currentCode,
                currentWeather.optString("description", WeatherCodeMapper.openMeteoDescription(currentCode)),
                current.optDouble("rain", 0.0),
                current.optDouble("wind_speed", 0.0),
                current.optInt("wind_deg", 0),
                forecast);
        if (forecast.size() > 0) {
            data.todayMinTempC = forecast.get(0).minTempC;
            data.todayMaxTempC = forecast.get(0).maxTempC;
            data.precipitationProbability = forecast.get(0).precipitationProbability;
        }
        data.humidityPercent = current.optInt("humidity", 0);
        data.pressureHpa = current.optDouble("pressure", 0.0);
        return data;
    }

    private int mapWeatherId(int id) {
        if (id >= 200 && id < 300) return 95;
        if (id >= 300 && id < 600) return 63;
        if (id >= 600 && id < 700) return 75;
        if (id >= 700 && id < 800) return 45;
        if (id == 800) return 0;
        return 2;
    }
}
