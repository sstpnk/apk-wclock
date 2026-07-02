package com.sstpnk.wclock.render;

import android.os.Handler;
import android.os.Looper;

import com.sstpnk.wclock.settings.SettingsRepository;
import com.sstpnk.wclock.weather.WeatherData;
import com.sstpnk.wclock.weather.WeatherRepository;

public final class RenderController {
    static final long FRAME_DELAY_MS = 33L;
    private static final long WEATHER_STATUS_MIN_VISIBLE_MS = 1500L;

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

    public void forceRefreshNow() {
        final SettingsRepository.Settings settings = settingsRepository.load();
        lastWeatherRefresh = System.currentTimeMillis();
        refreshWeather(settings, lastWeatherRefresh);
    }

    private void updateViewState() {
        final SettingsRepository.Settings settings = settingsRepository.load();
        view.setPhotoSource(settings.photoFolderPath, settings.photoFolderUri);
        view.setDisplaySettings(settings.collageEnabled, settings.showClock, settings.showWeather, settings.showForecast, settings.photoDisplayMode, settings.photoOrderMode, settings.maxVisiblePhotos, settings.photoChangeSeconds, settings.framePanSpeedPxPerSecond, settings.showSeconds, settings.weatherIconStyle, settings.clockPanelBackgroundAlpha, settings.weatherPanelBackgroundAlpha);
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
        final long statusShownAt = System.currentTimeMillis();
        view.setWeatherStatus("\u0417\u0430\u043f\u0440\u043e\u0441 \u043f\u043e\u0433\u043e\u0434\u044b");
        view.invalidate();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                long refreshMillis = settings.weatherRefreshMinutes * 60L * 1000L;
                String cacheKey = settings.weatherProvider + "|" + settings.cityName + "|" + settings.latitude + "|" + settings.longitude;
                final WeatherData data = weatherRepository.refreshCached(settings.cityName, settings.latitude, settings.longitude, now, refreshMillis, cacheKey);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        long delay = weatherCompletionDelayMillis(statusShownAt, System.currentTimeMillis());
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                weatherRefreshRunning = false;
                                view.setWeatherData(data);
                                view.setWeatherStatus(weatherStatusAfterRefresh(data, weatherRepository.lastError()));
                                view.invalidate();
                            }
                        }, delay);
                    }
                });
            }
        }, "wclock-weather-refresh");
        thread.setDaemon(true);
        thread.start();
    }

    static long weatherCompletionDelayMillis(long statusShownAtMillis, long nowMillis) {
        long elapsed = Math.max(0L, nowMillis - statusShownAtMillis);
        return Math.max(0L, WEATHER_STATUS_MIN_VISIBLE_MS - elapsed);
    }

    static String weatherStatusAfterRefresh(WeatherData data, String lastError) {
        return data == null || data.stale ? lastError : "";
    }
}
