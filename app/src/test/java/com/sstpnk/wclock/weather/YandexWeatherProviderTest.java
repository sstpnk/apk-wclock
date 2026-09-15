package com.sstpnk.wclock.weather;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class YandexWeatherProviderTest {
    @Test
    public void parsesForecastResponse() throws Exception {
        String json = "{"
                + "\"fact\":{\"temp\":17,\"condition\":\"light-rain\",\"prec_type\":1,\"humidity\":72,\"pressure_mm\":746},"
                + "\"forecasts\":["
                + "{\"date\":\"2026-09-15\",\"parts\":{\"day_short\":{\"temp\":18,\"temp_min\":14,\"condition\":\"rain\",\"prec_prob\":60},\"night_short\":{\"temp\":12}}},"
                + "{\"date\":\"2026-09-16\",\"parts\":{\"day_short\":{\"temp\":20,\"temp_min\":15,\"condition\":\"clear\",\"prec_prob\":5}}}"
                + "]}";

        WeatherData data = new YandexWeatherProvider("key").parse("Москва", json, 1000L);

        assertEquals("Яндекс Погода", data.providerName);
        assertEquals(17.0, data.temperatureC, 0.01);
        assertEquals(63, data.weatherCode);
        assertEquals(2, data.forecast.size());
        assertEquals("2026-09-15", data.forecast.get(0).date);
        assertEquals(12.0, data.forecast.get(0).minTempC, 0.01);
        assertEquals(18.0, data.forecast.get(0).maxTempC, 0.01);
        assertEquals(60, data.precipitationProbability);
        assertEquals(72, data.humidityPercent);
    }

    @Test
    public void sendsKeyInHeader() {
        assertEquals("secret", new YandexWeatherProvider("secret").headers().get("X-Yandex-Weather-Key"));
    }
}
