package com.sstpnk.wclock.render;

import android.os.Handler;
import android.os.Looper;

public final class RenderController {
    private static final long FRAME_DELAY_MS = 1000L;

    private final ClockWeatherCollageView view;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running;

    private final Runnable frame = new Runnable() {
        @Override
        public void run() {
            if (!running) {
                return;
            }
            view.invalidate();
            handler.postDelayed(this, FRAME_DELAY_MS);
        }
    };

    public RenderController(ClockWeatherCollageView view) {
        this.view = view;
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        handler.post(frame);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(frame);
    }
}
