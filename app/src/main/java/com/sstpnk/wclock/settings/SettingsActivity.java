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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

public final class SettingsActivity extends Activity {
    private static final int LOCATION_CITY = 101;
    private static final int LOCATION_COORDINATES = 102;
    private static final int PROVIDER_OPEN_METEO = 201;
    private static final int PROVIDER_MET_NORWAY = 202;
    private static final int PROVIDER_WEATHERAPI = 203;
    private static final int PROVIDER_OPENWEATHER = 204;
    private static final int ICON_OUTLINE = 301;
    private static final int ICON_FLAT = 302;

    private SettingsRepository repository;
    private SettingsRepository.Settings settings;
    private TextView folderValue;
    private EditText city;
    private EditText latitude;
    private EditText longitude;
    private EditText maxPhotos;
    private EditText photoInterval;
    private EditText weatherApiKey;
    private EditText openWeatherApiKey;
    private CheckBox collageEnabled;
    private CheckBox showSeconds;
    private RadioGroup locationMode;
    private RadioGroup weatherProvider;
    private RadioGroup weatherIconStyle;

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
        root.setPadding(dp(32), dp(72), dp(32), dp(32));
        scrollView.addView(root);

        root.addView(label("Настройки WClock", 28));

        root.addView(section("Коллаж"));
        collageEnabled = checkbox("Включить коллаж", settings.collageEnabled);
        root.addView(collageEnabled);
        folderValue = label("Папка: " + valueOrDash(settings.photoFolderPath), 16);
        root.addView(folderValue);
        Button folder = button("Выбрать папку");
        folder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(SettingsActivity.this, FileBrowserActivity.class), 10);
            }
        });
        root.addView(folder);
        root.addView(fieldLabel("Количество фотографий на экране"));
        maxPhotos = edit("1-50", Integer.toString(settings.maxVisiblePhotos));
        root.addView(maxPhotos);
        root.addView(fieldLabel("Интервал появления фотографий, сек"));
        photoInterval = edit("5-600", Integer.toString(settings.photoChangeSeconds));
        root.addView(photoInterval);

        root.addView(section("Локация"));
        locationMode = new RadioGroup(this);
        locationMode.setOrientation(RadioGroup.VERTICAL);
        locationMode.addView(radio(LOCATION_CITY, "Город"));
        locationMode.addView(radio(LOCATION_COORDINATES, "Координаты"));
        locationMode.check("city".equals(settings.locationMode) ? LOCATION_CITY : LOCATION_COORDINATES);
        root.addView(locationMode);
        root.addView(fieldLabel("Город для подписи погоды"));
        city = edit("Москва", settings.cityName);
        root.addView(city);
        root.addView(fieldLabel("Широта"));
        latitude = edit("55.7558", Double.toString(settings.latitude));
        root.addView(latitude);
        root.addView(fieldLabel("Долгота"));
        longitude = edit("37.6173", Double.toString(settings.longitude));
        root.addView(longitude);

        root.addView(section("Часы"));
        showSeconds = checkbox("Показывать секунды", settings.showSeconds);
        root.addView(showSeconds);

        root.addView(section("Погода"));
        weatherProvider = new RadioGroup(this);
        weatherProvider.setOrientation(RadioGroup.VERTICAL);
        weatherProvider.addView(radio(PROVIDER_OPEN_METEO, "Open-Meteo, без ключа"));
        weatherProvider.addView(radio(PROVIDER_MET_NORWAY, "MET Norway, без ключа"));
        weatherProvider.addView(radio(PROVIDER_WEATHERAPI, "WeatherAPI.com, нужен ключ"));
        weatherProvider.addView(radio(PROVIDER_OPENWEATHER, "OpenWeather, нужен ключ"));
        weatherProvider.check(providerId(settings.weatherProvider));
        root.addView(weatherProvider);

        root.addView(fieldLabel("WeatherAPI.com ключ"));
        weatherApiKey = edit("если выбран WeatherAPI.com", settings.weatherApiKey);
        root.addView(weatherApiKey);
        root.addView(fieldLabel("OpenWeather ключ"));
        openWeatherApiKey = edit("если выбран OpenWeather", settings.openWeatherApiKey);
        root.addView(openWeatherApiKey);

        root.addView(fieldLabel("Стиль погодных иконок"));
        weatherIconStyle = new RadioGroup(this);
        weatherIconStyle.setOrientation(RadioGroup.HORIZONTAL);
        weatherIconStyle.addView(radio(ICON_OUTLINE, "Контурные"));
        weatherIconStyle.addView(radio(ICON_FLAT, "Цветные"));
        weatherIconStyle.check("flat".equals(settings.weatherIconStyle) ? ICON_FLAT : ICON_OUTLINE);
        root.addView(weatherIconStyle);

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

    private TextView section(String text) {
        TextView view = label(text, 20);
        view.setPadding(0, dp(22), 0, dp(8));
        view.setGravity(Gravity.LEFT);
        return view;
    }

    private TextView fieldLabel(String text) {
        TextView view = label(text, 14);
        view.setPadding(0, dp(10), 0, 0);
        return view;
    }

    private TextView label(String text, int sp) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setPadding(0, dp(6), 0, dp(6));
        return view;
    }

    private CheckBox checkbox(String text, boolean checked) {
        CheckBox view = new CheckBox(this);
        view.setText(text);
        view.setTextSize(18);
        view.setChecked(checked);
        view.setFocusable(true);
        return view;
    }

    private RadioButton radio(int id, String text) {
        RadioButton button = new RadioButton(this);
        button.setId(id);
        button.setText(text);
        button.setTextSize(18);
        button.setFocusable(true);
        return button;
    }

    private EditText edit(String hint, String value) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setText(value);
        edit.setSingleLine(true);
        edit.setTextSize(18);
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
        settings.collageEnabled = collageEnabled.isChecked();
        settings.cityName = city.getText().toString();
        settings.latitude = parseDouble(latitude.getText().toString(), settings.latitude);
        settings.longitude = parseDouble(longitude.getText().toString(), settings.longitude);
        settings.maxVisiblePhotos = parseInt(maxPhotos.getText().toString(), settings.maxVisiblePhotos);
        settings.photoChangeSeconds = parseInt(photoInterval.getText().toString(), settings.photoChangeSeconds);
        settings.showSeconds = showSeconds.isChecked();
        settings.locationMode = locationMode.getCheckedRadioButtonId() == LOCATION_CITY ? "city" : "coordinates";
        settings.weatherProvider = providerValue(weatherProvider.getCheckedRadioButtonId());
        settings.weatherIconStyle = weatherIconStyle.getCheckedRadioButtonId() == ICON_FLAT ? "flat" : "outline";
        settings.weatherApiKey = weatherApiKey.getText().toString();
        settings.openWeatherApiKey = openWeatherApiKey.getText().toString();
        repository.save(settings);
        finish();
    }

    private int providerId(String value) {
        if ("met-norway".equals(value)) {
            return PROVIDER_MET_NORWAY;
        }
        if ("weatherapi".equals(value)) {
            return PROVIDER_WEATHERAPI;
        }
        if ("openweather".equals(value)) {
            return PROVIDER_OPENWEATHER;
        }
        return PROVIDER_OPEN_METEO;
    }

    private String providerValue(int id) {
        if (id == PROVIDER_MET_NORWAY) {
            return "met-norway";
        }
        if (id == PROVIDER_WEATHERAPI) {
            return "weatherapi";
        }
        if (id == PROVIDER_OPENWEATHER) {
            return "openweather";
        }
        return "open-meteo";
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
