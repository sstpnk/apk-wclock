package com.sstpnk.wclock.weather;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WeatherCodeMapperTest {
    @Test
    public void mapsOpenMeteoCodesToRussianText() {
        assertEquals("Ясно", WeatherCodeMapper.openMeteoDescription(0));
        assertEquals("Переменная облачность", WeatherCodeMapper.openMeteoDescription(2));
        assertEquals("Туман", WeatherCodeMapper.openMeteoDescription(45));
        assertEquals("Дождь", WeatherCodeMapper.openMeteoDescription(63));
        assertEquals("Снег", WeatherCodeMapper.openMeteoDescription(75));
        assertEquals("Гроза", WeatherCodeMapper.openMeteoDescription(95));
        assertEquals("Неизвестно", WeatherCodeMapper.openMeteoDescription(999));
    }
}
