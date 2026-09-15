package com.sstpnk.wclock.weather;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TomorrowIoProvider implements WeatherProvider {
    private final String apiKey;

    public TomorrowIoProvider(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public String name() {
        return "Tomorrow.io";
    }

    @Override
    public String buildUrl(double latitude, double longitude) {
        if (apiKey.length() == 0) {
            throw new IllegalStateException("Tomorrow.io key is empty");
        }
        return String.format(Locale.US,
                "https://api.tomorrow.io/v4/weather/forecast?location=%.5f,%.5f&timesteps=1h,1d&units=metric&apikey=%s",
                latitude, longitude, encode(apiKey));
    }

    @Override
    public WeatherData parse(String cityName, String body, long updatedAtMillis) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONObject timelines = root.getJSONObject("timelines");
        JSONArray hourly = timelines.optJSONArray("hourly");
        JSONObject currentValues = firstValues(hourly);
        if (currentValues == null) {
            throw new IllegalArgumentException("Tomorrow.io hourly forecast is empty");
        }
        int currentCode = codeFromTomorrow(currentValues.optInt("weatherCode", 1101));
        JSONArray daily = timelines.optJSONArray("daily");
        List<ForecastDay> forecast = new ArrayList<ForecastDay>();
        if (daily != null) {
            int count = Math.min(5, daily.length());
            for (int i = 0; i < count; i++) {
                JSONObject dayRoot = daily.getJSONObject(i);
                JSONObject values = dayRoot.getJSONObject("values");
                int rawCode = optInt(values, 1101, "weatherCodeFullDay", "weatherCodeDay", "weatherCodeMax", "weatherCode");
                forecast.add(new ForecastDay(
                        dateOnly(dayRoot.optString("time")),
                        codeFromTomorrow(rawCode),
                        optDouble(values, 0.0, "temperatureMin", "temperatureMinAvg", "temperatureAvg"),
                        optDouble(values, 0.0, "temperatureMax", "temperatureMaxAvg", "temperatureAvg"),
                        (int) Math.round(optDouble(values, 0.0, "precipitationProbabilityMax", "precipitationProbabilityAvg"))));
            }
        }
        WeatherData data = new WeatherData(
                name(),
                cityName,
                updatedAtMillis,
                false,
                currentValues.getDouble("temperature"),
                currentCode,
                WeatherCodeMapper.openMeteoDescription(currentCode),
                forecast);
        if (forecast.size() > 0) {
            data.todayMinTempC = forecast.get(0).minTempC;
            data.todayMaxTempC = forecast.get(0).maxTempC;
            data.precipitationProbability = forecast.get(0).precipitationProbability;
        }
        data.humidityPercent = (int) Math.round(currentValues.optDouble("humidity", 0.0));
        data.pressureHpa = optDouble(currentValues, 0.0, "pressureSeaLevel", "pressureSurfaceLevel");
        return data;
    }

    private static JSONObject firstValues(JSONArray timeline) {
        if (timeline == null || timeline.length() == 0) {
            return null;
        }
        JSONObject first = timeline.optJSONObject(0);
        return first == null ? null : first.optJSONObject("values");
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private static String dateOnly(String value) {
        if (value != null && value.length() >= 10) {
            return value.substring(0, 10);
        }
        return value == null ? "" : value;
    }

    private static int optInt(JSONObject object, int fallback, String... names) {
        for (String name : names) {
            if (object.has(name)) {
                return object.optInt(name, fallback);
            }
        }
        return fallback;
    }

    private static double optDouble(JSONObject object, double fallback, String... names) {
        for (String name : names) {
            if (object.has(name)) {
                return object.optDouble(name, fallback);
            }
        }
        return fallback;
    }

    private static int codeFromTomorrow(int code) {
        if (code >= 10000) {
            code = code / 10;
        }
        if (code == 1000 || code == 1100) return 0;
        if (code == 1101) return 2;
        if (code == 1102 || code == 1001) return 3;
        if (code >= 2000 && code < 3000) return 45;
        if (code >= 4000 && code < 5000) return 63;
        if (code >= 5000 && code < 7000) return 75;
        if (code >= 8000) return 95;
        return 2;
    }
}
