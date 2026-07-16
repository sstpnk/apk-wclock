package com.sstpnk.wclock.weather;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WeatherData {
    public final String providerName;
    public final String cityName;
    public final long updatedAtMillis;
    public final boolean stale;
    public final double temperatureC;
    public final int weatherCode;
    public final String descriptionRu;
    public final List<ForecastDay> forecast;
    public double todayMinTempC;
    public double todayMaxTempC;
    public int humidityPercent;
    public double pressureHpa;
    public int precipitationProbability;

    public WeatherData(String providerName, String cityName, long updatedAtMillis, boolean stale, double temperatureC, int weatherCode, String descriptionRu, List<ForecastDay> forecast) {
        this.providerName = providerName;
        this.cityName = cityName;
        this.updatedAtMillis = updatedAtMillis;
        this.stale = stale;
        this.temperatureC = temperatureC;
        this.weatherCode = weatherCode;
        this.descriptionRu = descriptionRu;
        this.forecast = Collections.unmodifiableList(new ArrayList<ForecastDay>(forecast));
    }
}
