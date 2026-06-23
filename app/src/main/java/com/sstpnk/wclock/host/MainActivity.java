package com.sstpnk.wclock.host;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.sstpnk.wclock.brightness.BrightnessController;
import com.sstpnk.wclock.brightness.BrightnessSchedule;
import com.sstpnk.wclock.render.ClockWeatherCollageView;
import com.sstpnk.wclock.render.RenderController;
import com.sstpnk.wclock.settings.SettingsActivity;
import com.sstpnk.wclock.settings.SettingsRepository;
import com.sstpnk.wclock.util.NetworkClient;
import com.sstpnk.wclock.weather.MetNorwayProvider;
import com.sstpnk.wclock.weather.OpenMeteoProvider;
import com.sstpnk.wclock.weather.WeatherRepository;

import java.util.Calendar;

public final class MainActivity extends Activity {
    private RenderController renderController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUi();

        ClockWeatherCollageView view = new ClockWeatherCollageView(this);
        renderController = new RenderController(view, new SettingsRepository(this), createWeatherRepository());
        setContentView(view);
        requestPhotoPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        applyConfiguredBrightness();
        renderController.start();
    }

    @Override
    protected void onPause() {
        renderController.stop();
        super.onPause();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_ENTER) {
            startActivity(new Intent(this, SettingsActivity.class));
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

    private WeatherRepository createWeatherRepository() {
        NetworkClient networkClient = new NetworkClient("WClock/0.1 contact: github.com/sstpnk/apk-wclock", 10000);
        return new WeatherRepository(networkClient, new OpenMeteoProvider(), new MetNorwayProvider());
    }

    private void applyConfiguredBrightness() {
        SettingsRepository.Settings settings = new SettingsRepository(this).load();
        Calendar calendar = Calendar.getInstance();
        BrightnessSchedule schedule = new BrightnessSchedule(7, 19, 23, settings.dayBrightness, settings.eveningBrightness, settings.nightBrightness);
        new BrightnessController().apply(getWindow(), schedule.brightnessForHour(calendar.get(Calendar.HOUR_OF_DAY)));
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
