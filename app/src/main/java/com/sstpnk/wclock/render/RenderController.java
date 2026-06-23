package com.sstpnk.wclock.render;

import android.os.Handler;
import android.os.Looper;

import com.sstpnk.wclock.settings.SettingsRepository;
import com.sstpnk.wclock.weather.WeatherData;
import com.sstpnk.wclock.weather.WeatherRepository;

public final class RenderController {
    private static final long FRAME_DELAY_MS = 1000L;

    private final ClockWeatherCollageView view;
    private final SettingsRepository settingsRepository;
    private final WeatherRepository weatherRepository;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running;
    private boolean weatherRefreshRunning;
    private long lastWeatherRefresh;

    private final Runnable frame = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            updateViewState();
            view.invalidate();
            handler.postDelayed(this, FRAME_DELAY_MS);
        }
    };

    public RenderController(ClockWeatherCollageView view, SettingsRepository settingsRepository, WeatherRepository weatherRepository) {
        this.view = view;
        this.settingsRepository = settingsRepository;
        this.weatherRepository = weatherRepository;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        handler.post(frame);
    }

    public void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void updateViewState() {
        final SettingsRepository.Settings settings = settingsRepository.load();
        view.setPhotoFolderPath(settings.photoFolderPath);
        view.setDisplaySettings(settings.maxVisiblePhotos, settings.photoChangeSeconds, settings.showSeconds);
        long now = System.currentTimeMillis();
        int intervalMillis = Math.max(1, settings.burnInMinMinutes) * 60 * 1000;
        int zoneIndex = (int) ((now / intervalMillis) % 6);
        view.setBurnInZoneIndex(zoneIndex);
        if (!weatherRefreshRunning && now - lastWeatherRefresh > settings.weatherRefreshMinutes * 60L * 1000L) {
            lastWeatherRefresh = now;
            refreshWeather(settings, now);
        }
    }

    private void refreshWeather(final SettingsRepository.Settings settings, final long now) {
        weatherRefreshRunning = true;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final WeatherData data = weatherRepository.refresh(settings.cityName, settings.latitude, settings.longitude, now);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        weatherRefreshRunning = false;
                        view.setWeatherData(data);
                        view.setWeatherStatus(data == null ? weatherRepository.lastError() : "");
                        view.invalidate();
                    }
                });
            }
        }, "wclock-weather-refresh");
        thread.setDaemon(true);
        thread.start();
    }
}
