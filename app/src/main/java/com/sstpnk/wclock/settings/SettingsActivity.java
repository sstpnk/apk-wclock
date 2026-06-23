package com.sstpnk.wclock.settings;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public final class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView textView = new TextView(this);
        textView.setText("Настройки WClock");
        textView.setTextSize(28.0f);
        textView.setPadding(32, 32, 32, 32);
        textView.setFocusable(true);
        setContentView(textView);
    }
}
