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
    public final double feelsLikeC;
    public final int weatherCode;
    public final String descriptionRu;
    public final double precipitationMm;
    public final double windSpeed;
    public final int windDirection;
    public final List<ForecastDay> forecast;
    public double todayMinTempC;
    public double todayMaxTempC;
    public int humidityPercent;
    public double pressureHpa;
    public int precipitationProbability;

    public WeatherData(String providerName, String cityName, long updatedAtMillis, boolean stale, double temperatureC, double feelsLikeC, int weatherCode, String descriptionRu, double precipitationMm, double windSpeed, int windDirection, List<ForecastDay> forecast) {
        this.providerName = providerName;
        this.cityName = cityName;
        this.updatedAtMillis = updatedAtMillis;
        this.stale = stale;
        this.temperatureC = temperatureC;
        this.feelsLikeC = feelsLikeC;
        this.weatherCode = weatherCode;
        this.descriptionRu = descriptionRu;
        this.precipitationMm = precipitationMm;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.forecast = Collections.unmodifiableList(new ArrayList<ForecastDay>(forecast));
    }
}
