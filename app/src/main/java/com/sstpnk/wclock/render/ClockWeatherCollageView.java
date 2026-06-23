package com.sstpnk.wclock.render;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ClockWeatherCollageView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF panel = new RectF();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMMM", new Locale("ru"));

    public ClockWeatherCollageView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        canvas.drawColor(Color.rgb(16, 18, 20));

        paint.setColor(Color.rgb(48, 72, 84));
        for (int i = 0; i < 7; i++) {
            float left = (i * width / 7.0f) - width * 0.05f;
            float top = (i % 3) * height * 0.22f + height * 0.10f;
            panel.set(left, top, left + width * 0.34f, top + height * 0.28f);
            canvas.save();
            canvas.rotate((i - 3) * 4.0f, panel.centerX(), panel.centerY());
            canvas.drawRoundRect(panel, 8, 8, paint);
            canvas.restore();
        }

        long now = System.currentTimeMillis();
        drawPanel(canvas, 32, 32, width * 0.45f, height * 0.25f);
        paint.setColor(Color.WHITE);
        paint.setTextSize(Math.max(56.0f, width * 0.08f));
        canvas.drawText(timeFormat.format(new Date(now)), 56, 112, paint);
        paint.setTextSize(Math.max(22.0f, width * 0.025f));
        canvas.drawText(dateFormat.format(new Date(now)), 58, 158, paint);

        drawPanel(canvas, width * 0.58f, 32, width - 32, height * 0.30f);
        paint.setTextSize(Math.max(24.0f, width * 0.025f));
        canvas.drawText("Москва  +0°C", width * 0.60f, 86, paint);
        canvas.drawText("Погода загружается", width * 0.60f, 126, paint);
        canvas.drawText("5 дней: -- -- -- -- --", width * 0.60f, 166, paint);
    }

    private void drawPanel(Canvas canvas, float left, float top, float right, float bottom) {
        panel.set(left, top, right, bottom);
        paint.setColor(0x99000000);
        canvas.drawRoundRect(panel, 8, 8, paint);
    }
}
