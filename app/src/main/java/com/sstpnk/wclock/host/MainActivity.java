package com.sstpnk.wclock.host;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.sstpnk.wclock.brightness.AmbientBrightnessMapper;
import com.sstpnk.wclock.brightness.BrightnessController;
import com.sstpnk.wclock.brightness.BrightnessSchedule;
import com.sstpnk.wclock.render.ClockWeatherCollageView;
import com.sstpnk.wclock.render.RenderController;
import com.sstpnk.wclock.settings.SettingsActivity;
import com.sstpnk.wclock.settings.SettingsRepository;
import com.sstpnk.wclock.util.CrashReporter;
import com.sstpnk.wclock.util.NetworkClient;
import com.sstpnk.wclock.weather.MetNorwayProvider;
import com.sstpnk.wclock.weather.OpenMeteoProvider;
import com.sstpnk.wclock.weather.OpenWeatherProvider;
import com.sstpnk.wclock.weather.WeatherApiProvider;
import com.sstpnk.wclock.weather.WeatherRepository;
import com.sstpnk.wclock.weather.WttrInProvider;

import java.util.Calendar;

public final class MainActivity extends Activity implements SensorEventListener {
    private RenderController renderController;
    private ClockWeatherCollageView clockView;
    private SettingsRepository settingsRepository;
    private String weatherRepositorySignature = "";
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private boolean lightSensorRegistered;
    private final BrightnessController brightnessController = new BrightnessController();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CrashReporter.install(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUi();

        clockView = new ClockWeatherCollageView(this);
        clockView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP && clockView.isSettingsButtonHit(event.getX(), event.getY())) {
                    openSettings();
                    return true;
                }
                return true;
            }
        });
        settingsRepository = new SettingsRepository(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        resetRenderController(settingsRepository.load());
        setContentView(clockView);
        requestPhotoPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        SettingsRepository.Settings settings = settingsRepository.load();
        applyConfiguredBrightness(settings);
        if (!weatherRepositorySignature.equals(weatherSignature(settings))) {
            resetRenderController(settings);
            renderController.forceRefreshNow();
        }
        renderController.start();
    }

    @Override
    protected void onPause() {
        unregisterLightSensor();
        renderController.stop();
        super.onPause();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_ENTER) {
            openSettings();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
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

    private void resetRenderController(SettingsRepository.Settings settings) {
        if (renderController != null) {
            renderController.stop();
        }
        weatherRepositorySignature = weatherSignature(settings);
        renderController = new RenderController(clockView, settingsRepository, createWeatherRepository(settings));
    }

    private String weatherSignature(SettingsRepository.Settings settings) {
        return settings.weatherProvider
                + "|" + settings.weatherApiKey
                + "|" + settings.openWeatherApiKey
                + "|" + settings.cityName
                + "|" + settings.latitude
                + "|" + settings.longitude
                + "|" + settings.weatherRefreshMinutes;
    }

    private void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    private void applyConfiguredBrightness(SettingsRepository.Settings settings) {
        if (settings.autoBrightnessEnabled && registerLightSensor()) {
            return;
        }
        applyScheduledBrightness(settings);
    }

    private void applyScheduledBrightness(SettingsRepository.Settings settings) {
        Calendar calendar = Calendar.getInstance();
        BrightnessSchedule schedule = new BrightnessSchedule(7, 19, 23, settings.dayBrightness, settings.eveningBrightness, settings.nightBrightness);
        brightnessController.apply(getWindow(), schedule.brightnessForHour(calendar.get(Calendar.HOUR_OF_DAY)));
    }

    private boolean registerLightSensor() {
        if (sensorManager == null || lightSensor == null) {
            return false;
        }
        if (!lightSensorRegistered) {
            lightSensorRegistered = sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        return lightSensorRegistered;
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
        SettingsRepository.Settings settings = settingsRepository.load();
        if (!settings.autoBrightnessEnabled) {
            return;
        }
        AmbientBrightnessMapper mapper = new AmbientBrightnessMapper(settings.autoBrightnessMin, settings.autoBrightnessMax);
        brightnessController.apply(getWindow(), mapper.brightnessForLux(event.values[0]));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void requestPhotoPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }
        String permission = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, 20);
        }
    }
}
