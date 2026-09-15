package com.sstpnk.wclock.weather;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VisualCrossingProviderTest {
    @Test
    public void parsesTimelineResponse() throws Exception {
        String json = "{"
                + "\"currentConditions\":{\"temp\":16.5,\"icon\":\"rain\",\"conditions\":\"Дождь\",\"humidity\":80,\"pressure\":1012},"
                + "\"days\":["
                + "{\"datetime\":\"2026-09-15\",\"icon\":\"rain\",\"tempmin\":12,\"tempmax\":18,\"precipprob\":70},"
                + "{\"datetime\":\"2026-09-16\",\"icon\":\"clear-day\",\"tempmin\":10,\"tempmax\":19,\"precipprob\":5}"
                + "]}";

        WeatherData data = new VisualCrossingProvider("key").parse("Москва", json, 1000L);

        assertEquals("Visual Crossing", data.providerName);
        assertEquals(16.5, data.temperatureC, 0.01);
        assertEquals(63, data.weatherCode);
        assertEquals("2026-09-15", data.forecast.get(0).date);
        assertEquals(12.0, data.todayMinTempC, 0.01);
        assertEquals(18.0, data.todayMaxTempC, 0.01);
        assertEquals(70, data.precipitationProbability);
        assertEquals(80, data.humidityPercent);
        assertEquals(1012.0, data.pressureHpa, 0.01);
    }
}
