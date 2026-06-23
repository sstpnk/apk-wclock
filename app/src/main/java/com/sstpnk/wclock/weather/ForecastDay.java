package com.sstpnk.wclock.weather;

public final class ForecastDay {
    public final String date;
    public final int weatherCode;
    public final String descriptionRu;
    public final double minTempC;
    public final double maxTempC;
    public final int precipitationProbability;
    public final double windSpeed;

    public ForecastDay(String date, int weatherCode, String descriptionRu, double minTempC, double maxTempC, int precipitationProbability, double windSpeed) {
        this.date = date;
        this.weatherCode = weatherCode;
        this.descriptionRu = descriptionRu;
        this.minTempC = minTempC;
        this.maxTempC = maxTempC;
        this.precipitationProbability = precipitationProbability;
        this.windSpeed = windSpeed;
    }
}
