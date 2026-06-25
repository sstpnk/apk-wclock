package com.sstpnk.wclock.render;

import androidx.test.core.app.ApplicationProvider;

import com.sstpnk.wclock.weather.ForecastDay;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ClockWeatherCollageViewTest {
    @Test
    public void forecastTemperatureDoesNotDuplicateEqualValues() throws Exception {
        ClockWeatherCollageView view = new ClockWeatherCollageView(ApplicationProvider.getApplicationContext());
        Method method = ClockWeatherCollageView.class.getDeclaredMethod("forecastTemperature", ForecastDay.class);
        method.setAccessible(true);

        String equal = (String) method.invoke(view, new ForecastDay("2026-06-25", 3, "Пасмурно", 17.0, 17.0, 0, 0.0));
        String different = (String) method.invoke(view, new ForecastDay("2026-06-26", 3, "Пасмурно", 15.0, 19.0, 0, 0.0));

        assertEquals("17°", equal);
        assertEquals("19°/15°", different);
    }
}
