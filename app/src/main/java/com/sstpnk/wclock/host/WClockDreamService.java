package com.sstpnk.wclock.host;

import android.service.dreams.DreamService;

import com.sstpnk.wclock.render.ClockWeatherCollageView;
import com.sstpnk.wclock.render.RenderController;
import com.sstpnk.wclock.settings.SettingsRepository;
import com.sstpnk.wclock.util.CrashReporter;
import com.sstpnk.wclock.util.NetworkClient;
import com.sstpnk.wclock.weather.MetNorwayProvider;
import com.sstpnk.wclock.weather.OpenMeteoProvider;
import com.sstpnk.wclock.weather.OpenWeatherProvider;
import com.sstpnk.wclock.weather.WeatherApiProvider;
import com.sstpnk.wclock.weather.WeatherRepository;
import com.sstpnk.wclock.weather.WttrInProvider;

public final class WClockDreamService extends DreamService {
    private RenderController renderController;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        CrashReporter.install(this);
        setFullscreen(true);
        setScreenBright(false);
        ClockWeatherCollageView view = new ClockWeatherCollageView(this);
        SettingsRepository settingsRepository = new SettingsRepository(this);
        renderController = new RenderController(view, settingsRepository, createWeatherRepository(settingsRepository.load()));
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

    private WeatherRepository createWeatherRepository(SettingsRepository.Settings settings) {
        NetworkClient networkClient = new NetworkClient("WClock/0.1 contact: github.com/sstpnk/apk-wclock", 10000);
        if ("met-norway".equals(settings.weatherProvider)) {
            return new WeatherRepository(networkClient, new MetNorwayProvider(), new OpenMeteoProvider(), new WttrInProvider());
        }
        if ("weatherapi".equals(settings.weatherProvider)) {
            return new WeatherRepository(networkClient, new WeatherApiProvider(settings.weatherApiKey), new OpenMeteoProvider(), new WttrInProvider());
        }
        if ("openweather".equals(settings.weatherProvider)) {
            return new WeatherRepository(networkClient, new OpenWeatherProvider(settings.openWeatherApiKey), new OpenMeteoProvider(), new WttrInProvider());
        }
        if ("wttr-in".equals(settings.weatherProvider)) {
            return new WeatherRepository(networkClient, new WttrInProvider(), new OpenMeteoProvider(), new MetNorwayProvider());
        }
        return new WeatherRepository(networkClient, new OpenMeteoProvider(), new MetNorwayProvider(), new WttrInProvider());
    }
}
