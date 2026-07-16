package com.sstpnk.wclock.weather;

import org.junit.Test;

import java.io.InputStream;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;

public class OpenMeteoProviderTest {
    @Test
    public void parsesCurrentAndFiveDayForecast() throws Exception {
        String json = readResource("open_meteo_forecast.json");
        WeatherData data = OpenMeteoProvider.parseBody("Москва", json, 1000L);

        assertEquals("Open-Meteo", data.providerName);
        assertEquals("Москва", data.cityName);
        assertEquals(21.4, data.temperatureC, 0.01);
        assertEquals("Переменная облачность", data.descriptionRu);
        assertEquals(5, data.forecast.size());
        assertEquals("2026-06-24", data.forecast.get(1).date);
    }

    private static String readResource(String name) {
        InputStream stream = OpenMeteoProviderTest.class.getClassLoader().getResourceAsStream(name);
        Scanner scanner = new Scanner(stream, "UTF-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }
}
