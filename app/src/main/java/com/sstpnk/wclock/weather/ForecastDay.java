package com.sstpnk.wclock.weather;

public final class ForecastDay {
    public final String date;
    public final int weatherCode;
    public final double minTempC;
    public final double maxTempC;
    public final int precipitationProbability;

    public ForecastDay(String date, int weatherCode, double minTempC, double maxTempC, int precipitationProbability) {
        this.date = date;
        this.weatherCode = weatherCode;
        this.minTempC = minTempC;
        this.maxTempC = maxTempC;
        this.precipitationProbability = precipitationProbability;
    }
}
