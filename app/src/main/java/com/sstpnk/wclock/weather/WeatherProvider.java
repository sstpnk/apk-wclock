package com.sstpnk.wclock.weather;

import java.util.Collections;
import java.util.Map;

public interface WeatherProvider {
    String name();

    String buildUrl(double latitude, double longitude);

    default Map<String, String> headers() {
        return Collections.emptyMap();
    }

    WeatherData parse(String cityName, String body, long updatedAtMillis) throws Exception;
}
