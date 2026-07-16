package com.sstpnk.wclock.render;

import android.os.Handler;
import android.os.Looper;

import com.sstpnk.wclock.settings.SettingsRepository;
import com.sstpnk.wclock.weather.WeatherData;
import com.sstpnk.wclock.weather.WeatherRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RenderController {
    static final long FRAME_DELAY_MS = 33L;
    private static final long WEATHER_STATUS_MIN_VISIBLE_MS = 1500L;

    private final ClockWeatherCollageView view;
    private final PhotoRenderer photoRenderer;
    private final SettingsSource settingsSource;
    private final WeatherRepository weatherRepository;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService weatherExecutor = Executors.newSingleThreadExecutor();
    private SettingsRepository.Settings cachedSettings;
    private boolean running;
    private boolean weatherRefreshRunning;
    private long lastWeatherRefresh;
    private long lastOverlayInvalidate;

    private final Runnable frame = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            updateViewState();
            if (photoRenderer != null) {
                photoRenderer.renderFrame();
            }
            if (!shouldInvalidateOverlay()) {
                view.postOnAnimation(this);
                return;
            }
            invalidateView();
            view.postOnAnimation(this);
        }
    };

    RenderController(ClockWeatherCollageView view, SettingsSource settingsSource, WeatherRepository weatherRepository) {
        this(view, null, settingsSource, weatherRepository);
    }

    public RenderController(ClockWeatherCollageView view, PhotoRenderer photoRenderer, SettingsRepository settingsRepository, WeatherRepository weatherRepository) {
        this(view, photoRenderer, new SettingsSource() {
            @Override
            public SettingsRepository.Settings load() {
                return settingsRepository.load();
            }
        }, weatherRepository);
    }

    RenderController(ClockWeatherCollageView view, PhotoRenderer photoRenderer, SettingsSource settingsSource, WeatherRepository weatherRepository) {
        this.view = view;
        this.photoRenderer = photoRenderer;
        this.settingsSource = settingsSource;
        this.weatherRepository = weatherRepository;
    }

    public void start() {
        if (running) {
            return;
        }
        refreshSettings();
        running = true;
        handler.post(frame);
    }

    public void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        weatherExecutor.shutdownNow();
        if (photoRenderer != null) {
            photoRenderer.recycle();
        }
    }

    public void forceRefreshNow() {
        final SettingsRepository.Settings settings = settingsOrLoad();
        lastWeatherRefresh = System.currentTimeMillis();
        refreshWeather(settings, lastWeatherRefresh);
    }

    private void updateViewState() {
        updateViewState(System.currentTimeMillis(), true);
    }

    void updateViewStateForTest(long now, boolean allowWeatherRefresh) {
        updateViewState(now, allowWeatherRefresh);
    }

    public void refreshSettings() {
        cachedSettings = settingsSource.load();
    }

    private void updateViewState(long now, boolean allowWeatherRefresh) {
        final SettingsRepository.Settings settings = settingsOrLoad();
        view.setDrawCollage(photoRenderer == null);
        view.setPhotoSource(settings.photoFolderPath, settings.photoFolderUri);
        view.setDisplaySettings(settings.collageEnabled, settings.showClock, settings.showWeather, settings.showForecast, settings.photoDisplayMode, settings.photoOrderMode, settings.maxVisiblePhotos, settings.photoChangeSeconds, settings.framePanSpeedPxPerSecond, settings.showSeconds, settings.weatherIconStyle, settings.clockPanelBackgroundAlpha, settings.weatherPanelBackgroundAlpha);
        if (photoRenderer != null) {
            photoRenderer.setPhotoSource(settings.photoFolderPath, settings.photoFolderUri);
            photoRenderer.setDisplaySettings(settings.collageEnabled, settings.photoDisplayMode, settings.photoOrderMode, settings.maxVisiblePhotos, settings.photoChangeSeconds, settings.framePanSpeedPxPerSecond);
        }
        int intervalMillis = Math.max(1, settings.burnInMinMinutes) * 60 * 1000;
        int zoneIndex = (int) ((now / intervalMillis) % 6);
        view.setBurnInZoneIndex(zoneIndex);
        if (allowWeatherRefresh && !weatherRefreshRunning && now - lastWeatherRefresh > settings.weatherRefreshMinutes * 60L * 1000L) {
            lastWeatherRefresh = now;
            refreshWeather(settings, now);
        }
    }

    private SettingsRepository.Settings settingsOrLoad() {
        if (cachedSettings == null) {
            refreshSettings();
        }
        return cachedSettings;
    }

    private void refreshWeather(final SettingsRepository.Settings settings, final long now) {
        weatherRefreshRunning = true;
        final long statusShownAt = System.currentTimeMillis();
        view.setWeatherStatus("\u0417\u0430\u043f\u0440\u043e\u0441 \u043f\u043e\u0433\u043e\u0434\u044b");
        invalidateView();
        weatherExecutor.execute(new Runnable() {
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
                                invalidateView();
                            }
                        }, delay);
                    }
                });
            }
        });
    }

    static long weatherCompletionDelayMillis(long statusShownAtMillis, long nowMillis) {
        long elapsed = Math.max(0L, nowMillis - statusShownAtMillis);
        return Math.max(0L, WEATHER_STATUS_MIN_VISIBLE_MS - elapsed);
    }

    static String weatherStatusAfterRefresh(WeatherData data, String lastError) {
        return data == null || data.stale ? lastError : "";
    }

    private void invalidateView() {
        lastOverlayInvalidate = System.currentTimeMillis();
        view.postInvalidateOnAnimation();
    }

    private boolean shouldInvalidateOverlay() {
        SettingsRepository.Settings settings = settingsOrLoad();
        if (photoRenderer == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        long interval = overlayInvalidateIntervalMillis(settings);
        return now - lastOverlayInvalidate >= interval;
    }

    static long overlayInvalidateIntervalMillis(SettingsRepository.Settings settings) {
        return settings.showClock ? 1000L : 60000L;
    }

    interface SettingsSource {
        SettingsRepository.Settings load();
    }
}
