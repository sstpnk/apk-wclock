package com.sstpnk.wclock.render;

import android.graphics.RectF;

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
import static org.junit.Assert.assertTrue;

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

    @Test
    public void landscapePanelsAreBottomAlignedAndWeatherIsRightAligned() throws Exception {
        ClockWeatherCollageView view = new ClockWeatherCollageView(ApplicationProvider.getApplicationContext());
        Method clockPanelMethod = ClockWeatherCollageView.class.getDeclaredMethod("clockPanel", int.class, int.class);
        Method weatherPanelMethod = ClockWeatherCollageView.class.getDeclaredMethod("weatherPanel", int.class, int.class, RectF.class);
        clockPanelMethod.setAccessible(true);
        weatherPanelMethod.setAccessible(true);

        RectF clockPanel = (RectF) clockPanelMethod.invoke(view, 1040, 768);
        RectF weatherPanel = (RectF) weatherPanelMethod.invoke(view, 1040, 768, clockPanel);

        assertEquals(clockPanel.bottom, weatherPanel.bottom, 0.01f);
        assertTrue("Panels should sit close to the bottom edge", clockPanel.bottom >= 768 - 26);
        assertTrue("Clock panel should not keep excess vertical padding", clockPanel.height() <= 145);
        assertEquals("Weather panel should remain right aligned", 1040 - 24, weatherPanel.right, 1.0f);
        assertTrue("Weather panel should not overlap the clock panel", weatherPanel.left >= clockPanel.right + 24);
        assertTrue("Weather panel should be visibly larger for readability", weatherPanel.width() >= 1040 * 0.50f);
        assertTrue("Weather panel should grow vertically for larger text and forecast cards", weatherPanel.height() >= 768 * 0.36f);
    }

    @Test
    public void burnInShiftKeepsLandscapePanelsBottomAligned() throws Exception {
        ClockWeatherCollageView view = new ClockWeatherCollageView(ApplicationProvider.getApplicationContext());
        Method clockPanelMethod = ClockWeatherCollageView.class.getDeclaredMethod("clockPanel", int.class, int.class);
        Method weatherPanelMethod = ClockWeatherCollageView.class.getDeclaredMethod("weatherPanel", int.class, int.class, RectF.class);
        clockPanelMethod.setAccessible(true);
        weatherPanelMethod.setAccessible(true);

        view.setBurnInZoneIndex(3);
        RectF clockPanel = (RectF) clockPanelMethod.invoke(view, 1040, 768);
        RectF weatherPanel = (RectF) weatherPanelMethod.invoke(view, 1040, 768, clockPanel);

        assertEquals(clockPanel.bottom, weatherPanel.bottom, 0.01f);
        assertTrue("Burn-in shift should remain near the bottom edge", clockPanel.bottom >= 768 - 38);
    }

    @Test
    public void clockPanelShrinksWhenSecondsAreHidden() throws Exception {
        ClockWeatherCollageView view = new ClockWeatherCollageView(ApplicationProvider.getApplicationContext());
        Method clockPanelMethod = ClockWeatherCollageView.class.getDeclaredMethod("clockPanel", int.class, int.class);
        clockPanelMethod.setAccessible(true);

        view.setDisplaySettings(true, "photowall", "random", 18, 5, false, "outline", 0.56f);
        RectF withoutSeconds = (RectF) clockPanelMethod.invoke(view, 1040, 768);
        view.setDisplaySettings(true, "photowall", "random", 18, 5, true, "outline", 0.56f);
        RectF withSeconds = (RectF) clockPanelMethod.invoke(view, 1040, 768);

        assertTrue("Clock panel without seconds should not reserve the seconds area", withoutSeconds.width() + 60.0f < withSeconds.width());
    }

    @Test
    public void weatherPanelShrinksWhenForecastIsHidden() throws Exception {
        ClockWeatherCollageView view = new ClockWeatherCollageView(ApplicationProvider.getApplicationContext());
        Method clockPanelMethod = ClockWeatherCollageView.class.getDeclaredMethod("clockPanel", int.class, int.class);
        Method weatherPanelMethod = ClockWeatherCollageView.class.getDeclaredMethod("weatherPanel", int.class, int.class, RectF.class);
        clockPanelMethod.setAccessible(true);
        weatherPanelMethod.setAccessible(true);

        RectF clockPanel = (RectF) clockPanelMethod.invoke(view, 1040, 768);
        RectF withForecast = (RectF) weatherPanelMethod.invoke(view, 1040, 768, clockPanel);
        view.setDisplaySettings(true, true, true, false, "photowall", "random", 18, 5, 20, false, "outline", 0.56f);
        RectF withoutForecast = (RectF) weatherPanelMethod.invoke(view, 1040, 768, clockPanel);

        assertTrue("Weather panel without five-day forecast should not keep the forecast-sized empty area", withoutForecast.height() + 70.0f < withForecast.height());
        assertEquals("Weather panel should remain bottom aligned with the clock", clockPanel.bottom, withoutForecast.bottom, 0.01f);
    }

    @Test
    public void weatherDetailTextAlignsWithHeaderTextAndUsesSlightlySmallerFont() throws Exception {
        ClockWeatherCollageView view = new ClockWeatherCollageView(ApplicationProvider.getApplicationContext());
        Method headerOffset = ClockWeatherCollageView.class.getDeclaredMethod("weatherHeaderTextOffset");
        Method detailOffset = ClockWeatherCollageView.class.getDeclaredMethod("weatherDetailTextOffset");
        Method detailSize = ClockWeatherCollageView.class.getDeclaredMethod("weatherDetailTextSize", int.class);
        Method descriptionSize = ClockWeatherCollageView.class.getDeclaredMethod("weatherDescriptionTextSize", int.class);
        headerOffset.setAccessible(true);
        detailOffset.setAccessible(true);
        detailSize.setAccessible(true);
        descriptionSize.setAccessible(true);

        assertEquals((Float) headerOffset.invoke(view), (Float) detailOffset.invoke(view), 0.01f);
        assertEquals((Float) descriptionSize.invoke(view, 1040), (Float) detailSize.invoke(view, 1040), 1.0f);
        assertTrue("Detail font should be slightly smaller than the previous scaled detail size", (Float) detailSize.invoke(view, 1040) < 19.0f);
    }

    @Test
    public void forecastCardsUseBalancedVerticalGaps() throws Exception {
        ClockWeatherCollageView view = new ClockWeatherCollageView(ApplicationProvider.getApplicationContext());
        Method cardTopMethod = ClockWeatherCollageView.class.getDeclaredMethod("forecastCardTop", RectF.class, float.class, float.class);
        cardTopMethod.setAccessible(true);
        RectF weatherPanel = new RectF(492.0f, 429.0f, 1016.0f, 744.0f);
        float currentWeatherBottom = weatherPanel.top + 150.0f;
        float cardHeight = 113.0f;

        float cardTop = (Float) cardTopMethod.invoke(view, weatherPanel, currentWeatherBottom, cardHeight);

        assertEquals(weatherPanel.bottom - cardTop - cardHeight, cardTop - currentWeatherBottom, 1.0f);
    }

    @Test
    public void weatherHeaderAndDetailsUseConsistentVerticalRhythm() throws Exception {
        ClockWeatherCollageView view = new ClockWeatherCollageView(ApplicationProvider.getApplicationContext());
        Method descriptionBaseline = ClockWeatherCollageView.class.getDeclaredMethod("weatherDescriptionBaselineOffset");
        Method cityBaseline = ClockWeatherCollageView.class.getDeclaredMethod("weatherCityBaselineOffset");
        Method firstDetailBaseline = ClockWeatherCollageView.class.getDeclaredMethod("weatherFirstDetailBaselineOffset");
        Method lineGap = ClockWeatherCollageView.class.getDeclaredMethod("weatherDetailLineGap");
        descriptionBaseline.setAccessible(true);
        cityBaseline.setAccessible(true);
        firstDetailBaseline.setAccessible(true);
        lineGap.setAccessible(true);

        float headerGap = (Float) descriptionBaseline.invoke(view) - (Float) cityBaseline.invoke(view);
        assertEquals((Float) lineGap.invoke(view), headerGap, 0.75f);
        assertTrue("Current weather detail block should sit lower, closer to forecast cards", (Float) firstDetailBaseline.invoke(view) >= 104.0f);
    }

    @Test
    public void weatherDetailsUseWiderLineGap() throws Exception {
        ClockWeatherCollageView view = new ClockWeatherCollageView(ApplicationProvider.getApplicationContext());
        Method method = ClockWeatherCollageView.class.getDeclaredMethod("weatherDetailLineGap");
        method.setAccessible(true);

        assertEquals(24.75f, (Float) method.invoke(view), 0.01f);
    }

    @Test
    public void panelBackgroundAlphaControlsPanelColor() throws Exception {
        ClockWeatherCollageView view = new ClockWeatherCollageView(ApplicationProvider.getApplicationContext());
        Method method = ClockWeatherCollageView.class.getDeclaredMethod("panelColor", float.class);
        method.setAccessible(true);

        assertEquals(0x8F000000, ((Integer) method.invoke(view, 0.56f)).intValue());
        assertEquals(0x00000000, ((Integer) method.invoke(view, -0.2f)).intValue());
        assertEquals(0xD9000000, ((Integer) method.invoke(view, 1.2f)).intValue());
    }
}
