package com.sstpnk.wclock.host;

import android.service.dreams.DreamService;

import com.sstpnk.wclock.render.ClockWeatherCollageView;
import com.sstpnk.wclock.render.RenderController;

public final class WClockDreamService extends DreamService {
    private RenderController renderController;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setFullscreen(true);
        setScreenBright(false);
        ClockWeatherCollageView view = new ClockWeatherCollageView(this);
        renderController = new RenderController(view);
        setContentView(view);
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        if (renderController != null) {
            renderController.start();
        }
    }

    @Override
    public void onDreamingStopped() {
        if (renderController != null) {
            renderController.stop();
        }
        super.onDreamingStopped();
    }
}
