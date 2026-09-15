package com.sstpnk.wclock.weather;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class YandexWeatherProvider implements WeatherProvider {
    private final String apiKey;

    public YandexWeatherProvider(String apiKey) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    public String name() {
        return "Яндекс Погода";
    }

    @Override
    public String buildUrl(double latitude, double longitude) {
        if (apiKey.length() == 0) {
            throw new IllegalStateException("Yandex Weather key is empty");
        }
        return String.format(Locale.US,
                "https://api.weather.yandex.ru/v2/forecast?lat=%.5f&lon=%.5f&lang=ru_RU&limit=5&hours=false&extra=false",
                latitude, longitude);
    }

    @Override
    public Map<String, String> headers() {
        return Collections.singletonMap("X-Yandex-Weather-Key", apiKey);
    }

    @Override
    public WeatherData parse(String cityName, String body, long updatedAtMillis) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONObject fact = root.getJSONObject("fact");
        String condition = fact.optString("condition", "partly-cloudy");
        int currentCode = codeFromCondition(condition, fact.optInt("prec_type", 0));
        List<ForecastDay> forecast = new ArrayList<ForecastDay>();
        JSONArray days = root.optJSONArray("forecasts");
        if (days != null) {
            int count = Math.min(5, days.length());
            for (int i = 0; i < count; i++) {
                JSONObject dayRoot = days.getJSONObject(i);
                JSONObject parts = dayRoot.optJSONObject("parts");
                JSONObject day = part(parts, "day_short", "day");
                JSONObject night = part(parts, "night_short", "night");
                int code = currentCode;
                if (day != null) {
                    code = codeFromCondition(day.optString("condition", condition), day.optInt("prec_type", 0));
                }
                double maxTemp = tempMax(day, night);
                double minTemp = tempMin(day, night, maxTemp);
                forecast.add(new ForecastDay(
                        dayRoot.optString("date"),
                        code,
                        minTemp,
                        maxTemp,
                        day == null ? 0 : day.optInt("prec_prob", 0)));
            }
        }
        WeatherData data = new WeatherData(
                name(),
                cityName,
                updatedAtMillis,
                false,
                fact.getDouble("temp"),
                currentCode,
                description(condition),
                forecast);
        if (forecast.size() > 0) {
            data.todayMinTempC = forecast.get(0).minTempC;
            data.todayMaxTempC = forecast.get(0).maxTempC;
            data.precipitationProbability = forecast.get(0).precipitationProbability;
        }
        data.humidityPercent = fact.optInt("humidity", 0);
        data.pressureHpa = fact.optDouble("pressure_mm", 0.0) * 1.33322;
        return data;
    }

    private static JSONObject part(JSONObject parts, String preferred, String fallback) {
        if (parts == null) {
            return null;
        }
        JSONObject value = parts.optJSONObject(preferred);
        if (value == null) {
            value = parts.optJSONObject(fallback);
        }
        return value;
    }

    private static double tempMax(JSONObject day, JSONObject night) {
        if (day != null) {
            if (day.has("temp_max")) return day.optDouble("temp_max");
            if (day.has("temp")) return day.optDouble("temp");
        }
        if (night != null) {
            if (night.has("temp_max")) return night.optDouble("temp_max");
            if (night.has("temp")) return night.optDouble("temp");
        }
        return 0.0;
    }

    private static double tempMin(JSONObject day, JSONObject night, double fallback) {
        if (night != null) {
            if (night.has("temp_min")) return night.optDouble("temp_min");
            if (night.has("temp")) return night.optDouble("temp");
        }
        if (day != null && day.has("temp_min")) {
            return day.optDouble("temp_min");
        }
        return fallback;
    }

    private static int codeFromCondition(String condition, int precType) {
        if (condition == null) return 2;
        if (condition.indexOf("thunderstorm") >= 0) return 95;
        if (condition.indexOf("hail") >= 0) return 95;
        if (condition.indexOf("snow") >= 0 || precType == 3) return 75;
        if (condition.indexOf("rain") >= 0 || condition.indexOf("showers") >= 0 || precType == 1 || precType == 2) return 63;
        if (condition.indexOf("overcast") >= 0 || condition.indexOf("cloudy") >= 0) return 3;
        if (condition.indexOf("clear") >= 0) return 0;
        return 2;
    }

    private static String description(String condition) {
        if (condition == null) return "Переменная облачность";
        if (condition.indexOf("thunderstorm") >= 0) return "Гроза";
        if (condition.indexOf("hail") >= 0) return "Град";
        if (condition.indexOf("snow") >= 0) return "Снег";
        if (condition.indexOf("rain") >= 0 || condition.indexOf("showers") >= 0) return "Дождь";
        if (condition.indexOf("overcast") >= 0) return "Пасмурно";
        if (condition.indexOf("cloudy") >= 0) return "Облачно";
        if (condition.indexOf("clear") >= 0) return "Ясно";
        return "Переменная облачность";
    }
}
