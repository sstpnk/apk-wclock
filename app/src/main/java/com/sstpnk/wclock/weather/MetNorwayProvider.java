package com.sstpnk.wclock.weather;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MetNorwayProvider implements WeatherProvider {
    @Override
    public String name() {
        return "MET Norway";
    }

    @Override
    public String buildUrl(double latitude, double longitude) {
        return String.format(Locale.US,
                "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.5f&lon=%.5f",
                latitude, longitude);
    }

    @Override
    public WeatherData parse(String cityName, String body, long updatedAtMillis) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONArray timeseries = root.getJSONObject("properties").getJSONArray("timeseries");
        JSONObject first = timeseries.getJSONObject(0);
        JSONObject instant = first.getJSONObject("data").getJSONObject("instant").getJSONObject("details");
        JSONObject next = first.getJSONObject("data").optJSONObject("next_1_hours");
        String symbol = "";
        if (next != null) {
            symbol = next.getJSONObject("summary").optString("symbol_code", "");
        }
        int code = codeFromSymbol(symbol);
        Map<String, DailyForecast> dailyForecasts = new LinkedHashMap<String, DailyForecast>();
        for (int i = 0; i < timeseries.length(); i++) {
            JSONObject point = timeseries.getJSONObject(i);
            JSONObject details = point.getJSONObject("data").getJSONObject("instant").getJSONObject("details");
            int pointCode = code;
            JSONObject pointNext = point.getJSONObject("data").optJSONObject("next_12_hours");
            if (pointNext == null) {
                pointNext = point.getJSONObject("data").optJSONObject("next_6_hours");
            }
            if (pointNext == null) {
                pointNext = point.getJSONObject("data").optJSONObject("next_1_hours");
            }
            if (pointNext != null) {
                pointCode = codeFromSymbol(pointNext.getJSONObject("summary").optString("symbol_code", ""));
            }
            double temp = details.getDouble("air_temperature");
            String date = point.getString("time").substring(0, 10);
            DailyForecast day = dailyForecasts.get(date);
            if (day == null) {
                day = new DailyForecast(date);
                dailyForecasts.put(date, day);
            }
            day.add(temp, pointCode, details.optDouble("wind_speed", 0.0));
        }
        List<ForecastDay> forecast = new ArrayList<ForecastDay>();
        for (DailyForecast day : dailyForecasts.values()) {
            if (forecast.size() >= 5) {
                break;
            }
            forecast.add(day.toForecastDay());
        }
        WeatherData data = new WeatherData(
                "MET Norway",
                cityName,
                updatedAtMillis,
                false,
                instant.getDouble("air_temperature"),
                code,
                WeatherCodeMapper.openMeteoDescription(code),
                forecast);
        if (forecast.size() > 0) {
            data.todayMinTempC = forecast.get(0).minTempC;
            data.todayMaxTempC = forecast.get(0).maxTempC;
        }
        data.humidityPercent = (int) instant.optDouble("relative_humidity", 0.0);
        data.pressureHpa = instant.optDouble("air_pressure_at_sea_level", 0.0);
        return data;
    }

    private static final class DailyForecast {
        private final String date;
        private double minTemp = Double.MAX_VALUE;
        private double maxTemp = -Double.MAX_VALUE;
        private int weatherCode = 2;
        private double maxWindSpeed;
        private boolean hasWeatherCode;

        DailyForecast(String date) {
            this.date = date;
        }

        void add(double temperature, int code, double windSpeed) {
            minTemp = Math.min(minTemp, temperature);
            maxTemp = Math.max(maxTemp, temperature);
            maxWindSpeed = Math.max(maxWindSpeed, windSpeed);
            if (!hasWeatherCode || severity(code) >= severity(weatherCode)) {
                weatherCode = code;
                hasWeatherCode = true;
            }
        }

        ForecastDay toForecastDay() {
            return new ForecastDay(
                    date,
                    weatherCode,
                    minTemp,
                    maxTemp,
                    0);
        }
    }

    private static int severity(int code) {
        if (code >= 95) return 5;
        if (code >= 61) return 4;
        if (code >= 45) return 3;
        if (code >= 3) return 2;
        if (code >= 2) return 1;
        return 0;
    }

    private static int codeFromSymbol(String symbol) {
        if (symbol == null) return 3;
        if (symbol.indexOf("clearsky") >= 0) return 0;
        if (symbol.indexOf("cloudy") >= 0) return 3;
        if (symbol.indexOf("rain") >= 0) return 63;
        if (symbol.indexOf("snow") >= 0) return 75;
        if (symbol.indexOf("thunder") >= 0) return 95;
        if (symbol.indexOf("fog") >= 0) return 45;
        return 2;
    }
}
