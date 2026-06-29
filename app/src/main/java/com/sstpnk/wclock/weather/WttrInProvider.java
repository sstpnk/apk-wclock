package com.sstpnk.wclock.weather;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WttrInProvider implements WeatherProvider {
    @Override
    public String name() {
        return "wttr.in";
    }

    @Override
    public String buildUrl(double latitude, double longitude) {
        return String.format(Locale.US, "http://wttr.in/%.5f%%2C%.5f?format=j1", latitude, longitude);
    }

    @Override
    public WeatherData parse(String cityName, String body, long updatedAtMillis) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONObject current = root.getJSONArray("current_condition").getJSONObject(0);
        int currentCode = normalizeCode(parseInt(current.optString("weatherCode"), 3));
        List<ForecastDay> forecast = new ArrayList<ForecastDay>();
        JSONArray weather = root.optJSONArray("weather");
        if (weather != null) {
            int count = Math.min(5, weather.length());
            for (int i = 0; i < count; i++) {
                JSONObject day = weather.getJSONObject(i);
                int code = codeFromHourly(day.optJSONArray("hourly"), currentCode);
                forecast.add(new ForecastDay(
                        day.optString("date"),
                        code,
                        WeatherCodeMapper.openMeteoDescription(code),
                        parseDouble(day.optString("mintempC"), 0.0),
                        parseDouble(day.optString("maxtempC"), 0.0),
                        chanceFromHourly(day.optJSONArray("hourly")),
                        0.0));
            }
        }
        WeatherData data = new WeatherData(
                name(),
                cityName,
                updatedAtMillis,
                false,
                parseDouble(current.optString("temp_C"), 0.0),
                parseDouble(current.optString("FeelsLikeC"), parseDouble(current.optString("temp_C"), 0.0)),
                currentCode,
                WeatherCodeMapper.openMeteoDescription(currentCode),
                parseDouble(current.optString("precipMM"), 0.0),
                parseDouble(current.optString("windspeedKmph"), 0.0),
                parseInt(current.optString("winddirDegree"), 0),
                forecast);
        data.humidityPercent = parseInt(current.optString("humidity"), 0);
        data.pressureHpa = parseDouble(current.optString("pressure"), 0.0);
        if (forecast.size() > 0) {
            data.todayMinTempC = forecast.get(0).minTempC;
            data.todayMaxTempC = forecast.get(0).maxTempC;
            data.precipitationProbability = forecast.get(0).precipitationProbability;
        }
        return data;
    }

    private static int codeFromHourly(JSONArray hourly, int fallback) {
        if (hourly == null || hourly.length() == 0) {
            return fallback;
        }
        JSONObject point = hourly.optJSONObject(hourly.length() / 2);
        if (point == null) {
            return fallback;
        }
        return normalizeCode(parseInt(point.optString("weatherCode"), fallback));
    }

    private static int chanceFromHourly(JSONArray hourly) {
        if (hourly == null || hourly.length() == 0) {
            return 0;
        }
        int max = 0;
        for (int i = 0; i < hourly.length(); i++) {
            JSONObject point = hourly.optJSONObject(i);
            if (point != null) {
                max = Math.max(max, parseInt(point.optString("chanceofrain"), 0));
            }
        }
        return max;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int normalizeCode(int code) {
        if (code == 113) return 0;
        if (code == 116) return 2;
        if (code == 119 || code == 122) return 3;
        if (code == 143 || code == 248 || code == 260) return 45;
        if (code == 176 || code == 263 || code == 266 || code == 281 || code == 284 || code == 293 || code == 296 || code == 299 || code == 302) return 61;
        if (code == 305 || code == 308 || code == 353 || code == 356 || code == 359) return 80;
        if (code == 179 || code == 182 || code == 185 || code == 227 || code == 230 || code == 311 || code == 314 || code == 317 || code == 320 || code == 323 || code == 326 || code == 329 || code == 332 || code == 335 || code == 338 || code == 350 || code == 362 || code == 365 || code == 368 || code == 371 || code == 374 || code == 377) return 75;
        if (code == 200 || code == 386 || code == 389 || code == 392 || code == 395) return 95;
        return code;
    }
}
