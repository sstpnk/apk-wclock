package com.sstpnk.wclock.settings;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.CheckBox;

public final class SettingsActivity extends Activity {
    private SettingsRepository repository;
    private SettingsRepository.Settings settings;
    private TextView folderValue;
    private EditText city;
    private EditText latitude;
    private EditText longitude;
    private EditText maxPhotos;
    private EditText photoInterval;
    private EditText weatherProvider;
    private EditText weatherIconStyle;
    private EditText weatherApiKey;
    private EditText openWeatherApiKey;
    private CheckBox showSeconds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new SettingsRepository(this);
        settings = repository.load();
        setContentView(buildContent());
        requestPhotoPermissionIfNeeded();
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        scrollView.addView(root);

        TextView title = label("Настройки WClock", 28);
        root.addView(title);

        folderValue = label("Папка: " + valueOrDash(settings.photoFolderPath), 18);
        root.addView(folderValue);
        Button folder = button("Выбрать папку");
        folder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(SettingsActivity.this, FileBrowserActivity.class), 10);
            }
        });
        root.addView(folder);

        city = edit("Город", settings.cityName);
        latitude = edit("Широта", Double.toString(settings.latitude));
        longitude = edit("Долгота", Double.toString(settings.longitude));
        root.addView(city);
        root.addView(latitude);
        root.addView(longitude);

        TextView collageTitle = label("Коллаж", 18);
        root.addView(collageTitle);
        maxPhotos = edit("Фото одновременно (1-50)", Integer.toString(settings.maxVisiblePhotos));
        photoInterval = edit("Интервал появления фото, сек", Integer.toString(settings.photoChangeSeconds));
        root.addView(maxPhotos);
        root.addView(photoInterval);

        TextView clockTitle = label("Часы", 18);
        root.addView(clockTitle);
        showSeconds = new CheckBox(this);
        showSeconds.setText("Показывать секунды");
        showSeconds.setTextSize(18);
        showSeconds.setChecked(settings.showSeconds);
        showSeconds.setFocusable(true);
        root.addView(showSeconds);

        TextView weatherTitle = label("Погода", 18);
        root.addView(weatherTitle);
        weatherProvider = edit("Источник: open-meteo / met-norway / weatherapi / openweather", settings.weatherProvider);
        weatherIconStyle = edit("Иконки погоды: outline / flat", settings.weatherIconStyle);
        weatherApiKey = edit("WeatherAPI.com ключ (если выбран)", settings.weatherApiKey);
        openWeatherApiKey = edit("OpenWeather ключ (если выбран)", settings.openWeatherApiKey);
        root.addView(weatherProvider);
        root.addView(weatherIconStyle);
        root.addView(weatherApiKey);
        root.addView(openWeatherApiKey);

        TextView diagnostics = label("Open-Meteo и MET Norway работают без ключа. WeatherAPI.com и OpenWeather требуют ключ.", 16);
        root.addView(diagnostics);

        Button save = button("Сохранить");
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAndFinish();
            }
        });
        root.addView(save);
        return scrollView;
    }

    private TextView label(String text, int sp) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setPadding(0, 12, 0, 12);
        return view;
    }

    private EditText edit(String hint, String value) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setText(value);
        edit.setSingleLine(true);
        edit.setTextSize(20);
        return edit;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(20);
        button.setGravity(Gravity.CENTER);
        button.setFocusable(true);
        return button;
    }

    private String valueOrDash(String value) {
        return value == null || value.length() == 0 ? "не выбрана" : value;
    }

    private void saveAndFinish() {
        settings.cityName = city.getText().toString();
        settings.latitude = parseDouble(latitude.getText().toString(), settings.latitude);
        settings.longitude = parseDouble(longitude.getText().toString(), settings.longitude);
        settings.maxVisiblePhotos = parseInt(maxPhotos.getText().toString(), settings.maxVisiblePhotos);
        settings.photoChangeSeconds = parseInt(photoInterval.getText().toString(), settings.photoChangeSeconds);
        settings.showSeconds = showSeconds.isChecked();
        settings.weatherProvider = weatherProvider.getText().toString();
        settings.weatherIconStyle = weatherIconStyle.getText().toString();
        settings.weatherApiKey = weatherApiKey.getText().toString();
        settings.openWeatherApiKey = openWeatherApiKey.getText().toString();
        repository.save(settings);
        finish();
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10 && resultCode == RESULT_OK && data != null) {
            settings.photoFolderPath = data.getStringExtra("path");
            folderValue.setText("Папка: " + valueOrDash(settings.photoFolderPath));
        }
    }

    private void requestPhotoPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 23) {
            return;
        }
        String permission = Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, 21);
        }
    }
}
