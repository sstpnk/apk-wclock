package com.sstpnk.wclock.weather;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TomorrowIoProviderTest {
    @Test
    public void parsesForecastResponse() throws Exception {
        String json = "{"
                + "\"timelines\":{"
                + "\"hourly\":[{\"time\":\"2026-09-15T12:00:00Z\",\"values\":{\"temperature\":15.2,\"weatherCode\":4001,\"humidity\":76,\"pressureSeaLevel\":1010}}],"
                + "\"daily\":["
                + "{\"time\":\"2026-09-15T00:00:00Z\",\"values\":{\"temperatureMin\":11,\"temperatureMax\":17,\"weatherCodeFullDay\":4001,\"precipitationProbabilityMax\":65}},"
                + "{\"time\":\"2026-09-16T00:00:00Z\",\"values\":{\"temperatureMin\":10,\"temperatureMax\":18,\"weatherCodeFullDay\":1000,\"precipitationProbabilityMax\":0}}"
                + "]}}";

        WeatherData data = new TomorrowIoProvider("key").parse("Москва", json, 1000L);

        assertEquals("Tomorrow.io", data.providerName);
        assertEquals(15.2, data.temperatureC, 0.01);
        assertEquals(63, data.weatherCode);
        assertEquals("2026-09-15", data.forecast.get(0).date);
        assertEquals(11.0, data.todayMinTempC, 0.01);
        assertEquals(17.0, data.todayMaxTempC, 0.01);
        assertEquals(65, data.precipitationProbability);
        assertEquals(76, data.humidityPercent);
        assertEquals(1010.0, data.pressureHpa, 0.01);
    }
}
