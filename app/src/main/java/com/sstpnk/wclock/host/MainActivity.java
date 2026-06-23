package com.sstpnk.wclock.host;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.sstpnk.wclock.render.ClockWeatherCollageView;
import com.sstpnk.wclock.render.RenderController;
import com.sstpnk.wclock.settings.SettingsActivity;

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
        renderController = new RenderController(view);
        setContentView(view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
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
}
