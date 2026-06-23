package com.sstpnk.wclock.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class SettingsActivity extends Activity {
    private SettingsRepository repository;
    private SettingsRepository.Settings settings;
    private TextView folderValue;
    private EditText city;
    private EditText latitude;
    private EditText longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new SettingsRepository(this);
        settings = repository.load();
        setContentView(buildContent());
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

        TextView diagnostics = label("Погода: Open-Meteo основной, MET Norway резервный. Ошибки сети не останавливают часы и коллаж.", 16);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10 && resultCode == RESULT_OK && data != null) {
            settings.photoFolderPath = data.getStringExtra("path");
            folderValue.setText("Папка: " + valueOrDash(settings.photoFolderPath));
        }
    }
}
