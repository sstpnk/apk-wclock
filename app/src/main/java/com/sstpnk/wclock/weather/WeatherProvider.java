package com.sstpnk.wclock.weather;

public interface WeatherProvider {
    String name();

    String buildUrl(double latitude, double longitude);

    WeatherData parse(String cityName, String body, long updatedAtMillis) throws Exception;
}
