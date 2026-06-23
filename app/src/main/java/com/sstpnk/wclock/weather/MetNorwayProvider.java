package com.sstpnk.wclock.weather;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        List<ForecastDay> forecast = new ArrayList<ForecastDay>();
        int count = Math.min(5, timeseries.length());
        for (int i = 0; i < count; i++) {
            JSONObject point = timeseries.getJSONObject(i);
            JSONObject details = point.getJSONObject("data").getJSONObject("instant").getJSONObject("details");
            int pointCode = code;
            JSONObject pointNext = point.getJSONObject("data").optJSONObject("next_6_hours");
            if (pointNext != null) {
                pointCode = codeFromSymbol(pointNext.getJSONObject("summary").optString("symbol_code", ""));
            }
            double temp = details.getDouble("air_temperature");
            forecast.add(new ForecastDay(
                    point.getString("time").substring(0, 10),
                    pointCode,
                    WeatherCodeMapper.openMeteoDescription(pointCode),
                    temp,
                    temp,
                    0,
                    details.optDouble("wind_speed", 0.0)));
        }
        return new WeatherData(
                "MET Norway",
                cityName,
                updatedAtMillis,
                false,
                instant.getDouble("air_temperature"),
                instant.getDouble("air_temperature"),
                code,
                WeatherCodeMapper.openMeteoDescription(code),
                0.0,
                instant.optDouble("wind_speed", 0.0),
                (int) instant.optDouble("wind_from_direction", 0.0),
                forecast);
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
