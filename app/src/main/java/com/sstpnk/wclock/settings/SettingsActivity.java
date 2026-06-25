package com.sstpnk.wclock.settings;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
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

import com.sstpnk.wclock.BuildConfig;
import com.sstpnk.wclock.util.CrashReporter;
import com.sstpnk.wclock.weather.WeatherRepository;

public final class SettingsActivity extends Activity {
    private static final int LOCATION_CITY = 101;
    private static final int LOCATION_COORDINATES = 102;
    private static final int PROVIDER_OPEN_METEO = 201;
    private static final int PROVIDER_MET_NORWAY = 202;
    private static final int PROVIDER_WEATHERAPI = 203;
    private static final int PROVIDER_OPENWEATHER = 204;
    private static final int PROVIDER_WTTR_IN = 205;
    private static final int ICON_OUTLINE = 301;
    private static final int ICON_FLAT = 302;
    private static final int REFRESH_15 = 401;
    private static final int REFRESH_30 = 402;
    private static final int REFRESH_60 = 403;
    private static final int REFRESH_180 = 404;
    private static final int REFRESH_360 = 405;
    private static final int REFRESH_720 = 406;
    private static final int MODE_PHOTOWALL = 501;
    private static final int MODE_FRAME = 502;
    private static final int ORDER_RANDOM = 601;
    private static final int ORDER_SEQUENTIAL = 602;

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
    private RadioGroup photoDisplayMode;
    private RadioGroup photoOrderMode;
    private RadioGroup locationMode;
    private RadioGroup weatherProvider;
    private RadioGroup weatherIconStyle;
    private RadioGroup weatherRefresh;

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
        root.addView(label("Сборка " + BuildConfig.BUILD_NUMBER, 14));
        root.addView(label("Лог последнего краша: " + CrashReporter.latestCrashLog(this).getAbsolutePath(), 12));

        root.addView(section("Коллаж"));
        collageEnabled = checkbox("Включить коллаж", settings.collageEnabled);
        root.addView(collageEnabled);
        folderValue = label("Папка: " + valueOrDash(settings.photoFolderPath), 16);
        root.addView(folderValue);
        Button folder = button("Выбрать папку");
        folder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFolderPicker();
            }
        });
        root.addView(folder);
        root.addView(fieldLabel("Режим фотографий"));
        photoDisplayMode = new RadioGroup(this);
        photoDisplayMode.setOrientation(RadioGroup.HORIZONTAL);
        photoDisplayMode.addView(radio(MODE_PHOTOWALL, "Фотостена"));
        photoDisplayMode.addView(radio(MODE_FRAME, "Фоторамка"));
        photoDisplayMode.check("frame".equals(settings.photoDisplayMode) ? MODE_FRAME : MODE_PHOTOWALL);
        root.addView(photoDisplayMode);
        root.addView(fieldLabel("Порядок выбора фотографий"));
        photoOrderMode = new RadioGroup(this);
        photoOrderMode.setOrientation(RadioGroup.HORIZONTAL);
        photoOrderMode.addView(radio(ORDER_RANDOM, "Случайно"));
        photoOrderMode.addView(radio(ORDER_SEQUENTIAL, "Последовательно"));
        photoOrderMode.check("sequential".equals(settings.photoOrderMode) ? ORDER_SEQUENTIAL : ORDER_RANDOM);
        root.addView(photoOrderMode);
        root.addView(fieldLabel("Количество фотографий на экране"));
        maxPhotos = edit("1-50", Integer.toString(settings.maxVisiblePhotos));
        root.addView(maxPhotos);
        root.addView(fieldLabel("Интервал появления фотографий, сек"));
        photoInterval = edit("1-60", Integer.toString(settings.photoChangeSeconds));
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
        weatherProvider.addView(radio(PROVIDER_WTTR_IN, "wttr.in, без ключа, HTTP"));
        weatherProvider.addView(radio(PROVIDER_WEATHERAPI, "WeatherAPI.com, нужен ключ"));
        weatherProvider.addView(radio(PROVIDER_OPENWEATHER, "OpenWeather, нужен ключ"));
        weatherProvider.check(providerId(settings.weatherProvider));
        root.addView(weatherProvider);
        root.addView(label("Последняя попытка: " + valueOrDash(WeatherRepository.lastDiagnosticsText()), 12));

        root.addView(fieldLabel("Частота обновления"));
        weatherRefresh = new RadioGroup(this);
        weatherRefresh.setOrientation(RadioGroup.VERTICAL);
        weatherRefresh.addView(radio(REFRESH_15, "15 минут"));
        weatherRefresh.addView(radio(REFRESH_30, "30 минут"));
        weatherRefresh.addView(radio(REFRESH_60, "1 час"));
        weatherRefresh.addView(radio(REFRESH_180, "3 часа"));
        weatherRefresh.addView(radio(REFRESH_360, "6 часов"));
        weatherRefresh.addView(radio(REFRESH_720, "12 часов"));
        weatherRefresh.check(refreshId(settings.weatherRefreshMinutes));
        root.addView(weatherRefresh);

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
        TextView bottomSpace = label("", 1);
        bottomSpace.setPadding(0, 0, 0, dp(56));
        root.addView(bottomSpace);
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
        settings.photoDisplayMode = photoDisplayMode.getCheckedRadioButtonId() == MODE_FRAME ? "frame" : "photowall";
        settings.photoOrderMode = photoOrderMode.getCheckedRadioButtonId() == ORDER_SEQUENTIAL ? "sequential" : "random";
        settings.cityName = city.getText().toString();
        settings.latitude = parseDouble(latitude.getText().toString(), settings.latitude);
        settings.longitude = parseDouble(longitude.getText().toString(), settings.longitude);
        settings.maxVisiblePhotos = parseInt(maxPhotos.getText().toString(), settings.maxVisiblePhotos);
        settings.photoChangeSeconds = parseInt(photoInterval.getText().toString(), settings.photoChangeSeconds);
        settings.showSeconds = showSeconds.isChecked();
        settings.locationMode = locationMode.getCheckedRadioButtonId() == LOCATION_CITY ? "city" : "coordinates";
        settings.weatherProvider = providerValue(weatherProvider.getCheckedRadioButtonId());
        settings.weatherRefreshMinutes = refreshValue(weatherRefresh.getCheckedRadioButtonId());
        settings.weatherIconStyle = weatherIconStyle.getCheckedRadioButtonId() == ICON_FLAT ? "flat" : "outline";
        settings.weatherApiKey = weatherApiKey.getText().toString();
        settings.openWeatherApiKey = openWeatherApiKey.getText().toString();
        repository.save(settings);
        finish();
    }

    private void openFolderPicker() {
        if (Build.VERSION.SDK_INT >= 21) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
            startActivityForResult(intent, 11);
            return;
        }
        startActivityForResult(new Intent(SettingsActivity.this, FileBrowserActivity.class), 10);
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
        if ("wttr-in".equals(value)) {
            return PROVIDER_WTTR_IN;
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
        if (id == PROVIDER_WTTR_IN) {
            return "wttr-in";
        }
        return "open-meteo";
    }

    private int refreshId(int minutes) {
        if (minutes <= 15) {
            return REFRESH_15;
        }
        if (minutes <= 30) {
            return REFRESH_30;
        }
        if (minutes <= 60) {
            return REFRESH_60;
        }
        if (minutes <= 180) {
            return REFRESH_180;
        }
        if (minutes <= 360) {
            return REFRESH_360;
        }
        return REFRESH_720;
    }

    private int refreshValue(int id) {
        if (id == REFRESH_15) {
            return 15;
        }
        if (id == REFRESH_60) {
            return 60;
        }
        if (id == REFRESH_180) {
            return 180;
        }
        if (id == REFRESH_360) {
            return 360;
        }
        if (id == REFRESH_720) {
            return 720;
        }
        return 30;
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
            settings.photoFolderUri = "";
            folderValue.setText("Папка: " + valueOrDash(settings.photoFolderPath));
        } else if (requestCode == 11 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                try {
                    getContentResolver().takePersistableUriPermission(uri, flags & Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {
                }
                settings.photoFolderUri = uri.toString();
                settings.photoFolderPath = "";
                folderValue.setText("Папка: " + uri.getLastPathSegment());
            }
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
