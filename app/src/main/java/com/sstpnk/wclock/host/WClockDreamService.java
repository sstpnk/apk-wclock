package com.sstpnk.wclock.host;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.service.dreams.DreamService;
import android.widget.FrameLayout;

import com.sstpnk.wclock.brightness.AmbientBrightnessMapper;
import com.sstpnk.wclock.render.ClockWeatherCollageView;
import com.sstpnk.wclock.render.PhotoImageViewRenderer;
import com.sstpnk.wclock.render.PhotoRenderer;
import com.sstpnk.wclock.render.RenderController;
import com.sstpnk.wclock.settings.SettingsRepository;
import com.sstpnk.wclock.util.CrashReporter;
import com.sstpnk.wclock.weather.WeatherRepository;

public final class WClockDreamService extends DreamService implements SensorEventListener {
    private RenderController renderController;
    private SettingsRepository settingsRepository;
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private boolean lightSensorRegistered;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        CrashReporter.install(this);
        setFullscreen(true);
        setScreenBright(false);
        settingsRepository = new SettingsRepository(this);
        SettingsRepository.Settings settings = settingsRepository.load();
        ClockWeatherCollageView view = new ClockWeatherCollageView(this);
        FrameLayout root = new FrameLayout(this);
        PhotoImageViewRenderer photoView = new PhotoImageViewRenderer(this);
        PhotoRenderer photoRenderer = photoView;
        root.addView(photoView, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(view, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        renderController = new RenderController(view, photoRenderer, settingsRepository, WeatherRepository.create(settings));
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        registerLightSensorIfEnabled();
        if (renderController != null) {
            renderController.start();
        }
    }

    @Override
    public void onDreamingStopped() {
        if (renderController != null) {
            renderController.stop();
        }
        unregisterLightSensor();
        super.onDreamingStopped();
    }

    @Override
    public void onDetachedFromWindow() {
        unregisterLightSensor();
        super.onDetachedFromWindow();
    }

    private void registerLightSensorIfEnabled() {
        SettingsRepository.Settings settings = settingsRepository.load();
        if (!settings.autoBrightnessEnabled || sensorManager == null || lightSensor == null || lightSensorRegistered) {
            return;
        }
        lightSensorRegistered = sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void unregisterLightSensor() {
        if (sensorManager != null && lightSensorRegistered) {
            sensorManager.unregisterListener(this);
            lightSensorRegistered = false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null || event.sensor.getType() != Sensor.TYPE_LIGHT || event.values == null || event.values.length == 0) {
            return;
        }
        if (!settingsRepository.load().autoBrightnessEnabled) {
            setScreenBright(false);
            return;
        }
        setScreenBright(AmbientBrightnessMapper.dreamScreenBrightForLux(event.values[0]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}
