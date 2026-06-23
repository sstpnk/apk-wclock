package com.sstpnk.wclock.render;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import com.sstpnk.wclock.collage.CollageEngine;
import com.sstpnk.wclock.weather.ForecastDay;
import com.sstpnk.wclock.weather.WeatherData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ClockWeatherCollageView extends View {
    private static final float PANEL_RADIUS_DP = 8.0f;
    private static final float EDGE_PADDING_DP = 24.0f;
    private static final float SETTINGS_SIZE_DP = 56.0f;

    private final CollageEngine collageEngine = new CollageEngine();
    private final OverlayLayoutEngine overlayLayoutEngine = new OverlayLayoutEngine();
    private final WeatherIconPainter weatherIconPainter = new WeatherIconPainter();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF panel = new RectF();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMMM", new Locale("ru"));
    private String photoFolderPath = "";
    private WeatherData weatherData;
    private int burnInZoneIndex;

    public ClockWeatherCollageView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void setPhotoFolderPath(String path) {
        this.photoFolderPath = path == null ? "" : path;
    }

    public void setWeatherData(WeatherData weatherData) {
        this.weatherData = weatherData;
    }

    public void setBurnInZoneIndex(int burnInZoneIndex) {
        this.burnInZoneIndex = burnInZoneIndex;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        collageEngine.setFolder(photoFolderPath, 8, width, height);
        collageEngine.draw(canvas, System.currentTimeMillis());

        long now = System.currentTimeMillis();
        RectF primary = overlayLayoutEngine.primaryPanel(burnInZoneIndex, width, height, density());
        drawPanel(canvas, primary.left, primary.top, primary.right, primary.bottom);
        float clockSize = clamp(width * 0.060f, dp(60), dp(112));
        float dateSize = clamp(width * 0.021f, dp(22), dp(38));
        Paint.FontMetrics clockMetrics = setText(clockSize, Color.WHITE, true);
        float clockBaseline = primary.top + dp(22) - clockMetrics.ascent;
        paint.setColor(Color.WHITE);
        canvas.drawText(timeFormat.format(new Date(now)), primary.left + dp(24), clockBaseline, paint);
        setText(dateSize, Color.WHITE, false);
        canvas.drawText(dateFormat.format(new Date(now)), primary.left + dp(26), clockBaseline + dp(44), paint);

        RectF weatherPanel = weatherPanel(width, height);
        drawPanel(canvas, weatherPanel.left, weatherPanel.top, weatherPanel.right, weatherPanel.bottom);
        setText(clamp(width * 0.018f, dp(18), dp(32)), Color.WHITE, false);
        String weatherLine = weatherData == null
                ? "Погода загружается"
                : weatherData.cityName + "  " + Math.round(weatherData.temperatureC) + "°C";
        String description = weatherData == null ? "" : weatherData.descriptionRu + (weatherData.stale ? " · устарело" : "");
        float iconSize = clamp(width * 0.035f, dp(40), dp(72));
        float weatherLeft = weatherPanel.left + dp(24);
        if (weatherData != null) {
            weatherIconPainter.draw(canvas, weatherData.weatherCode, weatherLeft + iconSize * 0.5f, weatherPanel.top + dp(52), iconSize);
        }
        float textLeft = weatherData == null ? weatherLeft : weatherLeft + iconSize + dp(18);
        canvas.drawText(weatherLine, textLeft, weatherPanel.top + dp(48), paint);
        setText(clamp(width * 0.014f, dp(16), dp(26)), Color.WHITE, false);
        canvas.drawText(description, textLeft, weatherPanel.top + dp(84), paint);
        canvas.drawText(forecastLine(), weatherLeft, weatherPanel.top + dp(126), paint);

        drawSettingsButton(canvas, width, height);
    }

    private void drawPanel(Canvas canvas, float left, float top, float right, float bottom) {
        panel.set(left, top, right, bottom);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x8F000000);
        canvas.drawRoundRect(panel, dp(PANEL_RADIUS_DP), dp(PANEL_RADIUS_DP), paint);
    }

    public boolean isSettingsButtonHit(float x, float y) {
        float size = dp(SETTINGS_SIZE_DP);
        float margin = dp(EDGE_PADDING_DP);
        return x >= getWidth() - margin - size && x <= getWidth() - margin
                && y >= getHeight() - margin - size && y <= getHeight() - margin;
    }

    private RectF weatherPanel(int width, int height) {
        float margin = dp(EDGE_PADDING_DP);
        float panelWidth = clamp(width * 0.42f, dp(420), dp(840));
        float panelHeight = clamp(height * 0.20f, dp(150), dp(240));
        return new RectF(width - margin - panelWidth, margin, width - margin, margin + panelHeight);
    }

    private void drawSettingsButton(Canvas canvas, int width, int height) {
        float size = dp(SETTINGS_SIZE_DP);
        float margin = dp(EDGE_PADDING_DP);
        float left = width - margin - size;
        float top = height - margin - size;
        panel.set(left, top, left + size, top + size);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x66000000);
        canvas.drawRoundRect(panel, dp(PANEL_RADIUS_DP), dp(PANEL_RADIUS_DP), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(Color.WHITE);
        float cx = panel.centerX();
        float cy = panel.centerY();
        canvas.drawCircle(cx, cy, size * 0.18f, paint);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * i / 4.0;
            float sx = cx + (float) Math.cos(angle) * size * 0.27f;
            float sy = cy + (float) Math.sin(angle) * size * 0.27f;
            float ex = cx + (float) Math.cos(angle) * size * 0.34f;
            float ey = cy + (float) Math.sin(angle) * size * 0.34f;
            canvas.drawLine(sx, sy, ex, ey, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private String forecastLine() {
        if (weatherData == null || weatherData.forecast.size() == 0) {
            return "5 дней: -- -- -- -- --";
        }
        StringBuilder builder = new StringBuilder("5 дней:");
        int count = Math.min(5, weatherData.forecast.size());
        for (int i = 0; i < count; i++) {
            ForecastDay day = weatherData.forecast.get(i);
            builder.append(' ').append(Math.round(day.minTempC)).append('/').append(Math.round(day.maxTempC));
        }
        return builder.toString();
    }

    private Paint.FontMetrics setText(float size, int color, boolean bold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setFakeBoldText(bold);
        paint.setTextSize(size);
        return paint.getFontMetrics();
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float dp(float value) {
        return value * density();
    }

    private float density() {
        return getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDetachedFromWindow() {
        collageEngine.recycle();
        super.onDetachedFromWindow();
    }
}
