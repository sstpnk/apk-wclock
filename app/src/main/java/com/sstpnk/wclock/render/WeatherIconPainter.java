package com.sstpnk.wclock.render;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

public final class WeatherIconPainter {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();

    public void draw(Canvas canvas, int weatherCode, float cx, float cy, float size, String style) {
        boolean flat = "flat".equals(style);
        if (weatherCode == 0 || weatherCode == 1) {
            drawSun(canvas, cx, cy, size, flat);
        } else if (weatherCode >= 95) {
            drawStorm(canvas, cx, cy, size, flat);
        } else if (weatherCode >= 71 && weatherCode <= 86) {
            drawSnow(canvas, cx, cy, size, flat);
        } else if (weatherCode >= 51 && weatherCode <= 82) {
            drawRain(canvas, cx, cy, size, flat);
        } else if (weatherCode == 45 || weatherCode == 48) {
            drawFog(canvas, cx, cy, size, flat);
        } else if (weatherCode == 2) {
            drawPartlyCloudy(canvas, cx, cy, size, flat);
        } else {
            drawCloud(canvas, cx, cy, size, flat);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawSun(Canvas canvas, float cx, float cy, float size, boolean flat) {
        paint.setStyle(flat ? Paint.Style.FILL : Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2.0f, size * 0.055f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(flat ? Color.rgb(255, 199, 51) : Color.argb(230, 255, 220, 110));
        canvas.drawCircle(cx, cy, size * 0.23f, paint);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * i / 4.0;
            float x1 = cx + (float) Math.cos(angle) * size * 0.34f;
            float y1 = cy + (float) Math.sin(angle) * size * 0.34f;
            float x2 = cx + (float) Math.cos(angle) * size * 0.47f;
            float y2 = cy + (float) Math.sin(angle) * size * 0.47f;
            canvas.drawLine(x1, y1, x2, y2, paint);
        }
    }

    private void drawPartlyCloudy(Canvas canvas, float cx, float cy, float size, boolean flat) {
        drawSun(canvas, cx - size * 0.18f, cy - size * 0.14f, size * 0.70f, flat);
        drawCloud(canvas, cx + size * 0.04f, cy + size * 0.05f, size, flat);
    }

    private void drawCloud(Canvas canvas, float cx, float cy, float size, boolean flat) {
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(Math.max(2.0f, size * 0.055f));
        paint.setStyle(flat ? Paint.Style.FILL : Paint.Style.STROKE);
        paint.setColor(flat ? Color.rgb(226, 232, 237) : Color.argb(224, 226, 232, 237));

        path.reset();
        path.moveTo(cx - size * 0.34f, cy + size * 0.12f);
        path.cubicTo(cx - size * 0.42f, cy - size * 0.06f, cx - size * 0.24f, cy - size * 0.24f, cx - size * 0.08f, cy - size * 0.16f);
        path.cubicTo(cx + size * 0.02f, cy - size * 0.38f, cx + size * 0.34f, cy - size * 0.26f, cx + size * 0.33f, cy - size * 0.02f);
        path.cubicTo(cx + size * 0.50f, cy - size * 0.02f, cx + size * 0.50f, cy + size * 0.21f, cx + size * 0.30f, cy + size * 0.21f);
        path.lineTo(cx - size * 0.28f, cy + size * 0.21f);
        path.cubicTo(cx - size * 0.38f, cy + size * 0.21f, cx - size * 0.40f, cy + size * 0.15f, cx - size * 0.34f, cy + size * 0.12f);
        if (flat) {
            path.close();
        }
        canvas.drawPath(path, paint);
    }

    private void drawRain(Canvas canvas, float cx, float cy, float size, boolean flat) {
        drawCloud(canvas, cx, cy - size * 0.12f, size, flat);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(2.0f, size * 0.055f));
        paint.setColor(flat ? Color.rgb(69, 170, 242) : Color.argb(210, 150, 204, 245));
        for (int i = -1; i <= 1; i++) {
            float x = cx + i * size * 0.17f;
            canvas.drawLine(x, cy + size * 0.22f, x - size * 0.06f, cy + size * 0.42f, paint);
        }
    }

    private void drawSnow(Canvas canvas, float cx, float cy, float size, boolean flat) {
        drawCloud(canvas, cx, cy - size * 0.12f, size, flat);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(flat ? Color.WHITE : Color.argb(220, 235, 244, 250));
        for (int i = -1; i <= 1; i++) {
            canvas.drawCircle(cx + i * size * 0.17f, cy + size * 0.33f, size * 0.035f, paint);
        }
    }

    private void drawStorm(Canvas canvas, float cx, float cy, float size, boolean flat) {
        drawRain(canvas, cx, cy, size, flat);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(flat ? Color.rgb(255, 214, 64) : Color.argb(230, 255, 230, 120));
        path.reset();
        path.moveTo(cx + size * 0.02f, cy + size * 0.03f);
        path.lineTo(cx - size * 0.12f, cy + size * 0.30f);
        path.lineTo(cx + size * 0.03f, cy + size * 0.28f);
        path.lineTo(cx - size * 0.06f, cy + size * 0.54f);
        path.lineTo(cx + size * 0.18f, cy + size * 0.20f);
        path.lineTo(cx + size * 0.04f, cy + size * 0.22f);
        path.close();
        canvas.drawPath(path, paint);
    }

    private void drawFog(Canvas canvas, float cx, float cy, float size, boolean flat) {
        drawCloud(canvas, cx, cy - size * 0.16f, size, flat);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(2.0f, size * 0.045f));
        paint.setColor(flat ? Color.rgb(195, 204, 211) : Color.argb(190, 205, 214, 220));
        for (int i = 0; i < 3; i++) {
            float y = cy + size * (0.16f + i * 0.13f);
            rect.set(cx - size * 0.34f, y - size * 0.04f, cx + size * 0.34f, y + size * 0.04f);
            canvas.drawArc(rect, 180, -180, false, paint);
        }
    }
}
