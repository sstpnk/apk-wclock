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

    @Test
    public void groupsHourlyTimeseriesIntoDistinctForecastDays() throws Exception {
        WeatherData data = new MetNorwayProvider().parse("Moscow", hourlyForecastJson(), 1000L);

        assertEquals(2, data.forecast.size());
        assertEquals("2026-06-29", data.forecast.get(0).date);
        assertEquals(16.0, data.forecast.get(0).minTempC, 0.01);
        assertEquals(22.0, data.forecast.get(0).maxTempC, 0.01);
        assertEquals("2026-06-30", data.forecast.get(1).date);
        assertEquals(19.0, data.forecast.get(1).minTempC, 0.01);
        assertEquals(24.0, data.forecast.get(1).maxTempC, 0.01);
    }

    private static String readResource(String name) {
        InputStream stream = MetNorwayProviderTest.class.getClassLoader().getResourceAsStream(name);
        Scanner scanner = new Scanner(stream, "UTF-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    private static String hourlyForecastJson() {
        return "{"
                + "\"properties\":{\"timeseries\":["
                + point("2026-06-29T00:00:00Z", 16.0, "clearsky_night")
                + ","
                + point("2026-06-29T12:00:00Z", 22.0, "partlycloudy_day")
                + ","
                + point("2026-06-29T18:00:00Z", 20.0, "clearsky_night")
                + ","
                + point("2026-06-30T00:00:00Z", 19.0, "cloudy")
                + ","
                + point("2026-06-30T12:00:00Z", 24.0, "rain")
                + "]}}";
    }

    private static String point(String time, double temperature, String symbol) {
        return "{"
                + "\"time\":\"" + time + "\","
                + "\"data\":{"
                + "\"instant\":{\"details\":{\"air_temperature\":" + temperature + ",\"wind_speed\":2.0,\"wind_from_direction\":180}},"
                + "\"next_1_hours\":{\"summary\":{\"symbol_code\":\"" + symbol + "\"}},"
                + "\"next_6_hours\":{\"summary\":{\"symbol_code\":\"" + symbol + "\"}}"
                + "}}";
    }
}
