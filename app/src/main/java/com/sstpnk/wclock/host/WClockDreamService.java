package com.sstpnk.wclock.host;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.service.dreams.DreamService;

import com.sstpnk.wclock.brightness.AmbientBrightnessMapper;
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
        ClockWeatherCollageView view = new ClockWeatherCollageView(this);
        settingsRepository = new SettingsRepository(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        renderController = new RenderController(view, settingsRepository, createWeatherRepository(settingsRepository.load()));
        setContentView(view);
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
