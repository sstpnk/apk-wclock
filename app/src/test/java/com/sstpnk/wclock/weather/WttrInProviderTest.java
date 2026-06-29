package com.sstpnk.wclock.weather;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WttrInProviderTest {
    @Test
    public void buildUrlUsesEncodedCoordinateSeparator() {
        assertEquals("http://wttr.in/55.75580%2C37.61730?format=j1", new WttrInProvider().buildUrl(55.7558, 37.6173));
    }

    @Test
    public void parsesCurrentAndForecastFromHttpFallbackJson() throws Exception {
        String json = "{"
                + "\"current_condition\":[{\"temp_C\":\"14\",\"FeelsLikeC\":\"12\",\"weatherCode\":\"116\",\"precipMM\":\"0.1\",\"windspeedKmph\":\"8\",\"winddirDegree\":\"270\",\"humidity\":\"70\",\"pressure\":\"1012\"}],"
                + "\"weather\":[{\"date\":\"2026-06-25\",\"mintempC\":\"10\",\"maxtempC\":\"16\",\"hourly\":[{\"weatherCode\":\"116\",\"chanceofrain\":\"20\"},{\"weatherCode\":\"176\",\"chanceofrain\":\"55\"}]}]"
                + "}";

        WeatherData data = new WttrInProvider().parse("Москва", json, 1000L);

        assertEquals("wttr.in", data.providerName);
        assertEquals(14.0, data.temperatureC, 0.01);
        assertEquals(12.0, data.feelsLikeC, 0.01);
        assertEquals(2, data.weatherCode);
        assertEquals(1, data.forecast.size());
        assertEquals(61, data.forecast.get(0).weatherCode);
        assertEquals(55, data.forecast.get(0).precipitationProbability);
    }
}
