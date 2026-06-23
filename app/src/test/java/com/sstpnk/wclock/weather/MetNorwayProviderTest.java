package com.sstpnk.wclock.weather;

import org.junit.Test;

import java.io.InputStream;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;

public class MetNorwayProviderTest {
    @Test
    public void parsesCompactForecastCurrentPoint() throws Exception {
        String json = readResource("met_norway_forecast.json");
        WeatherData data = new MetNorwayProvider().parse("Москва", json, 1000L);

        assertEquals("MET Norway", data.providerName);
        assertEquals(18.2, data.temperatureC, 0.01);
        assertEquals("Дождь", data.descriptionRu);
        assertEquals(2, data.forecast.size());
    }

    private static String readResource(String name) {
        InputStream stream = MetNorwayProviderTest.class.getClassLoader().getResourceAsStream(name);
        Scanner scanner = new Scanner(stream, "UTF-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }
}
