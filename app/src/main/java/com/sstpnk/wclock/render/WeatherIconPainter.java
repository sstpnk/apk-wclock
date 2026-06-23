package com.sstpnk.wclock.render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

public final class WeatherIconPainter {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    public void draw(Canvas canvas, int weatherCode, float cx, float cy, float size) {
        if (weatherCode == 0 || weatherCode == 1) {
            drawSun(canvas, cx, cy, size);
        } else if (weatherCode >= 61 && weatherCode <= 82) {
            drawRain(canvas, cx, cy, size);
        } else if (weatherCode >= 71 && weatherCode <= 86) {
            drawSnow(canvas, cx, cy, size);
        } else if (weatherCode >= 95) {
            drawStorm(canvas, cx, cy, size);
        } else if (weatherCode == 45 || weatherCode == 48) {
            drawFog(canvas, cx, cy, size);
        } else {
            drawCloud(canvas, cx, cy, size);
        }
    }

    private void drawSun(Canvas canvas, float cx, float cy, float size) {
        paint.setColor(Color.rgb(255, 202, 40));
        canvas.drawCircle(cx, cy, size * 0.28f, paint);
    }

    private void drawCloud(Canvas canvas, float cx, float cy, float size) {
        paint.setColor(Color.rgb(224, 230, 234));
        canvas.drawCircle(cx - size * 0.18f, cy, size * 0.20f, paint);
        canvas.drawCircle(cx, cy - size * 0.10f, size * 0.26f, paint);
        canvas.drawCircle(cx + size * 0.24f, cy, size * 0.18f, paint);
        oval.set(cx - size * 0.38f, cy, cx + size * 0.42f, cy + size * 0.24f);
        canvas.drawRect(oval, paint);
    }

    private void drawRain(Canvas canvas, float cx, float cy, float size) {
        drawCloud(canvas, cx, cy - size * 0.08f, size);
        paint.setColor(Color.rgb(77, 171, 245));
        paint.setStrokeWidth(Math.max(2.0f, size * 0.04f));
        for (int i = -1; i <= 1; i++) {
            float x = cx + i * size * 0.16f;
            canvas.drawLine(x, cy + size * 0.20f, x - size * 0.05f, cy + size * 0.42f, paint);
        }
    }

    private void drawSnow(Canvas canvas, float cx, float cy, float size) {
        drawCloud(canvas, cx, cy - size * 0.08f, size);
        paint.setColor(Color.WHITE);
        for (int i = -1; i <= 1; i++) {
            canvas.drawCircle(cx + i * size * 0.16f, cy + size * 0.34f, size * 0.035f, paint);
        }
    }

    private void drawStorm(Canvas canvas, float cx, float cy, float size) {
        drawRain(canvas, cx, cy, size);
        paint.setColor(Color.rgb(255, 238, 88));
        paint.setStrokeWidth(Math.max(3.0f, size * 0.05f));
        canvas.drawLine(cx, cy + size * 0.10f, cx - size * 0.10f, cy + size * 0.30f, paint);
        canvas.drawLine(cx - size * 0.10f, cy + size * 0.30f, cx + size * 0.08f, cy + size * 0.28f, paint);
        canvas.drawLine(cx + size * 0.08f, cy + size * 0.28f, cx - size * 0.04f, cy + size * 0.50f, paint);
    }

    private void drawFog(Canvas canvas, float cx, float cy, float size) {
        drawCloud(canvas, cx, cy - size * 0.12f, size);
        paint.setColor(Color.rgb(210, 214, 218));
        paint.setStrokeWidth(Math.max(2.0f, size * 0.035f));
        canvas.drawLine(cx - size * 0.36f, cy + size * 0.24f, cx + size * 0.36f, cy + size * 0.24f, paint);
        canvas.drawLine(cx - size * 0.28f, cy + size * 0.38f, cx + size * 0.28f, cy + size * 0.38f, paint);
    }
}
