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
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.sstpnk.wclock.BuildConfig;
import com.sstpnk.wclock.util.CrashReporter;
import com.sstpnk.wclock.weather.WeatherRepository;

public final class SettingsActivity extends Activity {
    static final String TAG_LOCATION_COORDINATES_ROW = "location_coordinates_row";
    static final String TAG_BRIGHTNESS_ROW = "brightness_row";
    static final String TAG_WEATHER_KEYS_ROW = "weather_keys_row";
    static final String TAG_PHOTO_SETTINGS_GROUP = "photo_settings_group";
    static final String TAG_CLOCK_SETTINGS_GROUP = "clock_settings_group";
    static final String TAG_WEATHER_SETTINGS_GROUP = "weather_settings_group";
    static final String TAG_BRIGHTNESS_AUTO_RANGE_ROW = "brightness_auto_range_row";

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
    private EditText framePanSpeed;
    private EditText weatherApiKey;
    private EditText openWeatherApiKey;
    private EditText autoBrightnessMin;
    private EditText autoBrightnessMax;
    private EditText dayBrightness;
    private EditText eveningBrightness;
    private EditText nightBrightness;
    private EditText panelBackgroundAlpha;
    private CheckBox collageEnabled;
    private CheckBox showClock;
    private CheckBox showWeather;
    private CheckBox showForecast;
    private CheckBox showSeconds;
    private CheckBox autoBrightnessEnabled;
    private RadioGroup photoDisplayMode;
    private RadioGroup photoOrderMode;
    private Spinner locationMode;
    private Spinner weatherProvider;
    private Spinner weatherIconStyle;
    private Spinner weatherRefresh;
    private LinearLayout photoSettingsGroup;
    private LinearLayout clockSettingsGroup;
    private LinearLayout weatherSettingsGroup;
    private LinearLayout brightnessAutoRangeRow;
    private LinearLayout weatherKeysRow;
    private LinearLayout weatherApiColumn;
    private LinearLayout openWeatherColumn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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
        root.setFocusableInTouchMode(true);
        root.requestFocus();
        scrollView.addView(root);

        root.addView(label("Настройки WClock", 28));
        root.addView(label("Сборка " + BuildConfig.BUILD_NUMBER, 14));
        root.addView(label("Лог последнего краша: " + CrashReporter.latestCrashLog(this).getAbsolutePath(), 12));

        root.addView(section("Коллаж"));
        collageEnabled = checkbox("Включить коллаж", settings.collageEnabled);
        root.addView(collageEnabled);
        photoSettingsGroup = new LinearLayout(this);
        photoSettingsGroup.setOrientation(LinearLayout.VERTICAL);
        photoSettingsGroup.setTag(TAG_PHOTO_SETTINGS_GROUP);
        folderValue = label("Папка: " + valueOrDash(settings.photoFolderPath), 16);
        photoSettingsGroup.addView(folderValue);
        Button folder = button("Выбрать папку");
        folder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFolderPicker();
            }
        });
        photoSettingsGroup.addView(folder);

        LinearLayout photoModes = row();
        LinearLayout photoModeColumn = column();
        photoModeColumn.addView(fieldLabel("Режим показа фотографий"));
        photoDisplayMode = new RadioGroup(this);
        photoDisplayMode.setOrientation(RadioGroup.HORIZONTAL);
        photoDisplayMode.addView(radio(MODE_PHOTOWALL, "Фотостена"));
        photoDisplayMode.addView(radio(MODE_FRAME, "Фоторамка"));
        photoDisplayMode.check("frame".equals(settings.photoDisplayMode) ? MODE_FRAME : MODE_PHOTOWALL);
        photoModeColumn.addView(photoDisplayMode);
        photoModes.addView(photoModeColumn);

        LinearLayout photoOrderColumn = column();
        photoOrderColumn.addView(fieldLabel("Порядок выбора фотографий"));
        photoOrderMode = new RadioGroup(this);
        photoOrderMode.setOrientation(RadioGroup.HORIZONTAL);
        photoOrderMode.addView(radio(ORDER_RANDOM, "Случайно"));
        photoOrderMode.addView(radio(ORDER_SEQUENTIAL, "По порядку"));
        photoOrderMode.check("sequential".equals(settings.photoOrderMode) ? ORDER_SEQUENTIAL : ORDER_RANDOM);
        photoOrderColumn.addView(photoOrderMode);
        photoModes.addView(photoOrderColumn);
        photoSettingsGroup.addView(photoModes);

        LinearLayout photoNumbers = row();
        LinearLayout maxPhotosColumn = column();
        maxPhotosColumn.addView(fieldLabel("Количество фото на экране"));
        maxPhotos = edit("1-50", Integer.toString(settings.maxVisiblePhotos));
        maxPhotosColumn.addView(maxPhotos);
        photoNumbers.addView(maxPhotosColumn);
        LinearLayout intervalColumn = column();
        intervalColumn.addView(fieldLabel("Интервал смены фото, сек"));
        photoInterval = edit("1-60", Integer.toString(settings.photoChangeSeconds));
        intervalColumn.addView(photoInterval);
        photoNumbers.addView(intervalColumn);
        LinearLayout speedColumn = column();
        speedColumn.addView(fieldLabel("Скорость панорамы, px/с"));
        framePanSpeed = edit("4-48", Integer.toString(settings.framePanSpeedPxPerSecond));
        speedColumn.addView(framePanSpeed);
        photoNumbers.addView(speedColumn);
        photoSettingsGroup.addView(photoNumbers);
        root.addView(photoSettingsGroup);

        root.addView(section("Локация"));
        LinearLayout locationRow = row();
        LinearLayout modeColumn = column();
        modeColumn.addView(fieldLabel("Источник локации"));
        locationMode = spinner(new String[]{"Координаты", "Город"}, "city".equals(settings.locationMode) ? 1 : 0);
        modeColumn.addView(locationMode);
        locationRow.addView(modeColumn);
        LinearLayout cityColumn = column();
        cityColumn.addView(fieldLabel("Город для подписи"));
        city = edit("Москва", settings.cityName);
        cityColumn.addView(city);
        locationRow.addView(cityColumn);
        root.addView(locationRow);

        LinearLayout coordinatesRow = row();
        coordinatesRow.setTag(TAG_LOCATION_COORDINATES_ROW);
        LinearLayout latitudeColumn = column();
        latitudeColumn.addView(fieldLabel("Широта для прогноза"));
        latitude = edit("55.7558", Double.toString(settings.latitude));
        latitudeColumn.addView(latitude);
        coordinatesRow.addView(latitudeColumn);
        LinearLayout longitudeColumn = column();
        longitudeColumn.addView(fieldLabel("Долгота для прогноза"));
        longitude = edit("37.6173", Double.toString(settings.longitude));
        longitudeColumn.addView(longitude);
        coordinatesRow.addView(longitudeColumn);
        root.addView(coordinatesRow);

        root.addView(section("Часы"));
        showClock = checkbox("Показывать часы", settings.showClock);
        root.addView(showClock);
        clockSettingsGroup = new LinearLayout(this);
        clockSettingsGroup.setOrientation(LinearLayout.VERTICAL);
        clockSettingsGroup.setTag(TAG_CLOCK_SETTINGS_GROUP);
        LinearLayout clockRow = row();
        LinearLayout secondsColumn = column();
        showSeconds = checkbox("Показывать секунды", settings.showSeconds);
        secondsColumn.addView(showSeconds);
        clockRow.addView(secondsColumn);
        clockSettingsGroup.addView(clockRow);
        root.addView(clockSettingsGroup);
        LinearLayout alphaRow = row();
        LinearLayout alphaColumn = column();
        alphaColumn.addView(fieldLabel("Прозрачность подложек, 0.0-0.85"));
        panelBackgroundAlpha = edit("0.56", Float.toString(settings.panelBackgroundAlpha));
        alphaColumn.addView(panelBackgroundAlpha);
        alphaRow.addView(alphaColumn);
        root.addView(alphaRow);

        root.addView(section("Яркость"));
        root.addView(label("Автояркость использует минимум и максимум как границы датчика. Если автояркость выключена, применяются фиксированные значения день/вечер/ночь.", 12));
        LinearLayout brightnessRow = row();
        brightnessRow.setTag(TAG_BRIGHTNESS_ROW);
        LinearLayout autoBrightnessColumn = column();
        autoBrightnessEnabled = checkbox("Автояркость по датчику", settings.autoBrightnessEnabled);
        autoBrightnessColumn.addView(autoBrightnessEnabled);
        brightnessRow.addView(autoBrightnessColumn);
        brightnessAutoRangeRow = row();
        brightnessAutoRangeRow.setTag(TAG_BRIGHTNESS_AUTO_RANGE_ROW);
        LinearLayout minColumn = column();
        minColumn.addView(fieldLabel("Минимальная яркость авто, 0.05-1.0"));
        autoBrightnessMin = edit("0.08", Float.toString(settings.autoBrightnessMin));
        minColumn.addView(autoBrightnessMin);
        brightnessAutoRangeRow.addView(minColumn);
        LinearLayout maxColumn = column();
        maxColumn.addView(fieldLabel("Максимальная яркость авто, 0.05-1.0"));
        autoBrightnessMax = edit("0.90", Float.toString(settings.autoBrightnessMax));
        maxColumn.addView(autoBrightnessMax);
        brightnessAutoRangeRow.addView(maxColumn);
        brightnessRow.addView(brightnessAutoRangeRow);
        root.addView(brightnessRow);
        LinearLayout scheduleRow = row();
        LinearLayout dayColumn = column();
        dayColumn.addView(fieldLabel("Фиксированная яркость днём"));
        dayBrightness = edit("0.85", Float.toString(settings.dayBrightness));
        dayColumn.addView(dayBrightness);
        scheduleRow.addView(dayColumn);
        LinearLayout eveningColumn = column();
        eveningColumn.addView(fieldLabel("Фиксированная яркость вечером"));
        eveningBrightness = edit("0.45", Float.toString(settings.eveningBrightness));
        eveningColumn.addView(eveningBrightness);
        scheduleRow.addView(eveningColumn);
        LinearLayout nightColumn = column();
        nightColumn.addView(fieldLabel("Фиксированная яркость ночью"));
        nightBrightness = edit("0.12", Float.toString(settings.nightBrightness));
        nightColumn.addView(nightBrightness);
        scheduleRow.addView(nightColumn);
        root.addView(scheduleRow);

        root.addView(section("Погода"));
        showWeather = checkbox("Показывать погоду", settings.showWeather);
        root.addView(showWeather);
        weatherSettingsGroup = new LinearLayout(this);
        weatherSettingsGroup.setOrientation(LinearLayout.VERTICAL);
        weatherSettingsGroup.setTag(TAG_WEATHER_SETTINGS_GROUP);
        LinearLayout weatherControls = row();
        LinearLayout providerColumn = column();
        providerColumn.addView(fieldLabel("Источник погоды"));
        weatherProvider = spinner(new String[]{"Автоматически", "WeatherAPI.com", "OpenWeather"}, providerIndex(settings.weatherProvider));
        providerColumn.addView(weatherProvider);
        weatherControls.addView(providerColumn);
        LinearLayout refreshColumn = column();
        refreshColumn.addView(fieldLabel("Частота обновления погоды"));
        weatherRefresh = spinner(new String[]{"15 мин", "30 мин", "1 час", "3 часа", "6 часов", "12 часов"}, refreshIndex(settings.weatherRefreshMinutes));
        refreshColumn.addView(weatherRefresh);
        weatherControls.addView(refreshColumn);
        LinearLayout iconColumn = column();
        iconColumn.addView(fieldLabel("Стиль погодных иконок"));
        weatherIconStyle = spinner(new String[]{"Контурные", "Цветные"}, iconStyleIndex(settings.weatherIconStyle));
        iconColumn.addView(weatherIconStyle);
        weatherControls.addView(iconColumn);
        weatherSettingsGroup.addView(weatherControls);
        showForecast = checkbox("Показывать прогноз на следующие 5 дней", settings.showForecast);
        weatherSettingsGroup.addView(showForecast);
        weatherSettingsGroup.addView(label("Последняя попытка обновления погоды: " + valueOrDash(WeatherRepository.lastDiagnosticsText()), 12));

        weatherKeysRow = row();
        weatherKeysRow.setTag(TAG_WEATHER_KEYS_ROW);
        weatherApiColumn = column();
        weatherApiColumn.addView(fieldLabel("WeatherAPI.com ключ"));
        weatherApiKey = edit("если выбран WeatherAPI.com", settings.weatherApiKey);
        weatherApiColumn.addView(weatherApiKey);
        weatherKeysRow.addView(weatherApiColumn);
        openWeatherColumn = column();
        openWeatherColumn.addView(fieldLabel("OpenWeather ключ"));
        openWeatherApiKey = edit("если выбран OpenWeather", settings.openWeatherApiKey);
        openWeatherColumn.addView(openWeatherApiKey);
        weatherKeysRow.addView(openWeatherColumn);
        weatherSettingsGroup.addView(weatherKeysRow);
        updateWeatherKeyVisibility();
        weatherProvider.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateWeatherKeyVisibility();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateWeatherKeyVisibility();
            }
        });

        root.addView(weatherSettingsGroup);
        attachDependentVisibilityHandlers();
        updateDependentVisibility();

        Button save = button("Сохранить");
        save.setPadding(0, dp(10), 0, dp(10));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        saveParams.setMargins(0, dp(28), 0, 0);
        save.setLayoutParams(saveParams);
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

    private void updateWeatherKeyVisibility() {
        if (weatherKeysRow == null || weatherProvider == null) {
            return;
        }
        int selected = weatherProvider.getSelectedItemPosition();
        weatherKeysRow.setVisibility(selected == 0 ? View.GONE : View.VISIBLE);
        weatherApiColumn.setVisibility(selected == 1 ? View.VISIBLE : View.GONE);
        openWeatherColumn.setVisibility(selected == 2 ? View.VISIBLE : View.GONE);
    }

    private void attachDependentVisibilityHandlers() {
        CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                updateDependentVisibility();
            }
        };
        collageEnabled.setOnCheckedChangeListener(listener);
        showClock.setOnCheckedChangeListener(listener);
        showWeather.setOnCheckedChangeListener(listener);
        autoBrightnessEnabled.setOnCheckedChangeListener(listener);
    }

    private void updateDependentVisibility() {
        if (photoSettingsGroup != null) {
            photoSettingsGroup.setVisibility(collageEnabled.isChecked() ? View.VISIBLE : View.GONE);
        }
        if (clockSettingsGroup != null) {
            clockSettingsGroup.setVisibility(showClock.isChecked() ? View.VISIBLE : View.GONE);
        }
        if (weatherSettingsGroup != null) {
            weatherSettingsGroup.setVisibility(showWeather.isChecked() ? View.VISIBLE : View.GONE);
        }
        if (brightnessAutoRangeRow != null) {
            brightnessAutoRangeRow.setVisibility(autoBrightnessEnabled.isChecked() ? View.VISIBLE : View.GONE);
        }
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
        edit.setFocusableInTouchMode(true);
        return edit;
    }

    private Spinner spinner(String[] values, int selectedIndex) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Math.max(0, Math.min(values.length - 1, selectedIndex)));
        spinner.setFocusable(true);
        return spinner;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBaselineAligned(false);
        return row;
    }

    private LinearLayout column() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(0, 0, dp(14), 0);
        column.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        return column;
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
        return value == null || value.length() == 0 ? "не выбрано" : value;
    }

    private void saveAndFinish() {
        settings.collageEnabled = collageEnabled.isChecked();
        settings.showClock = showClock.isChecked();
        settings.showWeather = showWeather.isChecked();
        settings.showForecast = showForecast.isChecked();
        settings.photoDisplayMode = photoDisplayMode.getCheckedRadioButtonId() == MODE_FRAME ? "frame" : "photowall";
        settings.photoOrderMode = photoOrderMode.getCheckedRadioButtonId() == ORDER_SEQUENTIAL ? "sequential" : "random";
        settings.cityName = city.getText().toString();
        settings.latitude = parseDouble(latitude.getText().toString(), settings.latitude);
        settings.longitude = parseDouble(longitude.getText().toString(), settings.longitude);
        settings.maxVisiblePhotos = parseInt(maxPhotos.getText().toString(), settings.maxVisiblePhotos);
        settings.photoChangeSeconds = parseInt(photoInterval.getText().toString(), settings.photoChangeSeconds);
        settings.framePanSpeedPxPerSecond = parseInt(framePanSpeed.getText().toString(), settings.framePanSpeedPxPerSecond);
        settings.showSeconds = showSeconds.isChecked();
        settings.locationMode = locationMode.getSelectedItemPosition() == 1 ? "city" : "coordinates";
        settings.weatherProvider = providerValue(weatherProvider.getSelectedItemPosition());
        settings.weatherRefreshMinutes = refreshValue(weatherRefresh.getSelectedItemPosition());
        settings.weatherIconStyle = iconStyleValue(weatherIconStyle.getSelectedItemPosition());
        settings.weatherApiKey = weatherApiKey.getText().toString();
        settings.openWeatherApiKey = openWeatherApiKey.getText().toString();
        settings.autoBrightnessEnabled = autoBrightnessEnabled.isChecked();
        settings.autoBrightnessMin = parseFloat(autoBrightnessMin.getText().toString(), settings.autoBrightnessMin);
        settings.autoBrightnessMax = parseFloat(autoBrightnessMax.getText().toString(), settings.autoBrightnessMax);
        settings.dayBrightness = parseFloat(dayBrightness.getText().toString(), settings.dayBrightness);
        settings.eveningBrightness = parseFloat(eveningBrightness.getText().toString(), settings.eveningBrightness);
        settings.nightBrightness = parseFloat(nightBrightness.getText().toString(), settings.nightBrightness);
        settings.panelBackgroundAlpha = parseFloat(panelBackgroundAlpha.getText().toString(), settings.panelBackgroundAlpha);
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

    private int providerIndex(String value) {
        if ("weatherapi".equals(value)) {
            return 1;
        }
        if ("openweather".equals(value)) {
            return 2;
        }
        return 0;
    }

    private String providerValue(int index) {
        if (index == 1) {
            return "weatherapi";
        }
        if (index == 2) {
            return "openweather";
        }
        return "open-meteo";
    }

    private int iconStyleIndex(String value) {
        return "flat".equals(value) ? 1 : 0;
    }

    private String iconStyleValue(int index) {
        return index == 1 ? "flat" : "outline";
    }

    private int refreshIndex(int minutes) {
        if (minutes <= 15) {
            return 0;
        }
        if (minutes <= 30) {
            return 1;
        }
        if (minutes <= 60) {
            return 2;
        }
        if (minutes <= 180) {
            return 3;
        }
        if (minutes <= 360) {
            return 4;
        }
        return 5;
    }

    private int refreshValue(int index) {
        if (index == 0) {
            return 15;
        }
        if (index == 2) {
            return 60;
        }
        if (index == 3) {
            return 180;
        }
        if (index == 4) {
            return 360;
        }
        if (index == 5) {
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

    private float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
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
