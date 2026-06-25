package com.sstpnk.wclock.render;

import androidx.test.core.app.ApplicationProvider;

import com.sstpnk.wclock.weather.ForecastDay;
import com.sstpnk.wclock.weather.WeatherData;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ClockWeatherCollageViewTest {
    @Test
    public void forecastTemperatureDoesNotDuplicateEqualValues() throws Exception {
        ClockWeatherCollageView view = new ClockWeatherCollageView(ApplicationProvider.getApplicationContext());
        Method method = ClockWeatherCollageView.class.getDeclaredMethod("forecastTemperature", ForecastDay.class);
        method.setAccessible(true);

        String equal = (String) method.invoke(view, new ForecastDay("2026-06-25", 3, "\u041f\u0430\u0441\u043c\u0443\u0440\u043d\u043e", 17.0, 17.0, 0, 0.0));
        String different = (String) method.invoke(view, new ForecastDay("2026-06-26", 3, "\u041f\u0430\u0441\u043c\u0443\u0440\u043d\u043e", 15.0, 19.0, 0, 0.0));

        assertEquals("17\u00b0", equal);
        assertEquals("19\u00b0/15\u00b0", different);
    }

    @Test
    public void weatherStatusUsesReadableLoadingAndErrorText() throws Exception {
        ClockWeatherCollageView view = new ClockWeatherCollageView(ApplicationProvider.getApplicationContext());
        Method visibleStatus = ClockWeatherCollageView.class.getDeclaredMethod("visibleWeatherStatusText", long.class);
        Method friendlyStatus = ClockWeatherCollageView.class.getDeclaredMethod("userFriendlyWeatherStatus", String.class);
        visibleStatus.setAccessible(true);
        friendlyStatus.setAccessible(true);

        view.setWeatherData(new WeatherData("test", "\u041c\u043e\u0441\u043a\u0432\u0430", 1000L, false, 1.0, 1.0, 0, "\u042f\u0441\u043d\u043e", 0.0, 0.0, 0, Collections.<ForecastDay>emptyList()));
        view.setWeatherStatus("\u0417\u0430\u043f\u0440\u043e\u0441 \u043f\u043e\u0433\u043e\u0434\u044b");

        assertEquals("\u0417\u0430\u043f\u0440\u043e\u0441 \u043f\u043e\u0433\u043e\u0434\u044b", visibleStatus.invoke(view, System.currentTimeMillis()));
        assertEquals("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u043e\u0433\u043e\u0434\u044b: timeout", friendlyStatus.invoke(view, "Weather failed: timeout"));
    }
}
