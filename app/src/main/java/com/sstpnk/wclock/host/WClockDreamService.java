package com.sstpnk.wclock.host;

import android.service.dreams.DreamService;

import com.sstpnk.wclock.render.ClockWeatherCollageView;
import com.sstpnk.wclock.render.RenderController;
import com.sstpnk.wclock.settings.SettingsRepository;
import com.sstpnk.wclock.util.NetworkClient;
import com.sstpnk.wclock.weather.MetNorwayProvider;
import com.sstpnk.wclock.weather.OpenMeteoProvider;
import com.sstpnk.wclock.weather.WeatherRepository;

public final class WClockDreamService extends DreamService {
    private RenderController renderController;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setFullscreen(true);
        setScreenBright(false);
        ClockWeatherCollageView view = new ClockWeatherCollageView(this);
        renderController = new RenderController(view, new SettingsRepository(this), createWeatherRepository());
        setContentView(view);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        if (renderController != null) {
            renderController.start();
        }
    }

    @Override
    public void onDreamingStopped() {
        if (renderController != null) {
            renderController.stop();
        }
        super.onDreamingStopped();
    }

    private WeatherRepository createWeatherRepository() {
        NetworkClient networkClient = new NetworkClient("WClock/0.1 contact: github.com/sstpnk/apk-wclock", 10000);
        return new WeatherRepository(networkClient, new OpenMeteoProvider(), new MetNorwayProvider());
    }
}
