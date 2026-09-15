package com.sstpnk.wclock.weather;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VisualCrossingProvider implements WeatherProvider {
    private final String apiKey;

    public VisualCrossingProvider(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public String name() {
        return "Visual Crossing";
    }

    @Override
    public String buildUrl(double latitude, double longitude) {
        if (apiKey.length() == 0) {
            throw new IllegalStateException("Visual Crossing key is empty");
        }
        return String.format(Locale.US,
                "https://weather.visualcrossing.com/VisualCrossingWebServices/rest/services/timeline/%.5f,%.5f?unitGroup=metric&include=current,days&contentType=json&lang=ru&key=%s",
                latitude, longitude, encode(apiKey));
    }

    @Override
    public WeatherData parse(String cityName, String body, long updatedAtMillis) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONObject current = root.getJSONObject("currentConditions");
        String currentIcon = current.optString("icon", current.optString("conditions", ""));
        int currentCode = codeFromIcon(currentIcon);
        JSONArray days = root.optJSONArray("days");
        List<ForecastDay> forecast = new ArrayList<ForecastDay>();
        if (days != null) {
            int count = Math.min(5, days.length());
            for (int i = 0; i < count; i++) {
                JSONObject day = days.getJSONObject(i);
                forecast.add(new ForecastDay(
                        day.optString("datetime"),
                        codeFromIcon(day.optString("icon", day.optString("conditions", currentIcon))),
                        day.optDouble("tempmin", 0.0),
                        day.optDouble("tempmax", 0.0),
                        (int) Math.round(day.optDouble("precipprob", 0.0))));
            }
        }
        WeatherData data = new WeatherData(
                name(),
                cityName,
                updatedAtMillis,
                false,
                current.getDouble("temp"),
                currentCode,
                description(currentIcon, current.optString("conditions", "")),
                forecast);
        if (forecast.size() > 0) {
            data.todayMinTempC = forecast.get(0).minTempC;
            data.todayMaxTempC = forecast.get(0).maxTempC;
            data.precipitationProbability = forecast.get(0).precipitationProbability;
        }
        data.humidityPercent = (int) Math.round(current.optDouble("humidity", 0.0));
        data.pressureHpa = current.optDouble("pressure", 0.0);
        return data;
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private static int codeFromIcon(String value) {
        if (value == null) return 2;
        String icon = value.toLowerCase(Locale.US);
        if (icon.indexOf("thunder") >= 0) return 95;
        if (icon.indexOf("snow") >= 0 || icon.indexOf("sleet") >= 0) return 75;
        if (icon.indexOf("rain") >= 0 || icon.indexOf("drizzle") >= 0) return 63;
        if (icon.indexOf("fog") >= 0 || icon.indexOf("mist") >= 0) return 45;
        if (icon.indexOf("cloud") >= 0 || icon.indexOf("overcast") >= 0) return 3;
        if (icon.indexOf("clear") >= 0 || icon.indexOf("sun") >= 0) return 0;
        return 2;
    }

    private static String description(String icon, String fallback) {
        int code = codeFromIcon(icon);
        if (code == 95) return "Гроза";
        if (code == 75) return "Снег";
        if (code == 63) return "Дождь";
        if (code == 45) return "Туман";
        if (code == 3) return "Облачно";
        if (code == 0) return "Ясно";
        return fallback == null || fallback.length() == 0 ? "Переменная облачность" : fallback;
    }
}
