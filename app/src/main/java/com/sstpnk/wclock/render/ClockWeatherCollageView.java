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
    private static final float SETTINGS_SIZE_DP = 36.0f;

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
    private int maxVisiblePhotos = 18;
    private int photoChangeSeconds = 20;
    private boolean showSeconds;
    private String weatherStatus = "Загрузка погоды";
    private long weatherStatusMillis;

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

    public void setDisplaySettings(int maxVisiblePhotos, int photoChangeSeconds, boolean showSeconds) {
        this.maxVisiblePhotos = Math.max(1, Math.min(50, maxVisiblePhotos));
        this.photoChangeSeconds = Math.max(5, photoChangeSeconds);
        this.showSeconds = showSeconds;
    }

    public void setWeatherStatus(String weatherStatus) {
        this.weatherStatus = weatherStatus == null ? "" : weatherStatus;
        this.weatherStatusMillis = System.currentTimeMillis();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        long now = System.currentTimeMillis();
        collageEngine.setFolder(photoFolderPath);
        collageEngine.draw(canvas, now, maxVisiblePhotos, photoChangeSeconds);

        RectF clockPanel = clockPanel(width, height);
        drawPanel(canvas, clockPanel.left, clockPanel.top, clockPanel.right, clockPanel.bottom, 0x76000000);
        drawClock(canvas, clockPanel, now, width);

        if (weatherData != null) {
            RectF weatherPanel = weatherPanel(width, height, clockPanel);
            drawPanel(canvas, weatherPanel.left, weatherPanel.top, weatherPanel.right, weatherPanel.bottom, 0x78000000);
            drawWeather(canvas, weatherPanel, width);
        } else {
            drawWeatherStatus(canvas, width, height);
        }

        drawSettingsButton(canvas, width, height);
    }

    private void drawPanel(Canvas canvas, float left, float top, float right, float bottom, int color) {
        panel.set(left, top, right, bottom);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRoundRect(panel, dp(PANEL_RADIUS_DP), dp(PANEL_RADIUS_DP), paint);
    }

    public boolean isSettingsButtonHit(float x, float y) {
        float size = dp(SETTINGS_SIZE_DP);
        float margin = dp(EDGE_PADDING_DP);
        return x >= getWidth() - margin - size && x <= getWidth() - margin
                && y >= getHeight() - margin - size && y <= getHeight() - margin;
    }

    private RectF clockPanel(int width, int height) {
        float margin = dp(EDGE_PADDING_DP);
        if (width > height) {
            float panelWidth = clamp(width * 0.32f, dp(470), dp(760));
            float panelHeight = clamp(height * 0.30f, dp(190), dp(280));
            float left = margin + (burnInZoneIndex % 2) * dp(18);
            return new RectF(left, margin, left + panelWidth, margin + panelHeight);
        }
        float panelWidth = width - margin * 2;
        float panelHeight = clamp(height * 0.18f, dp(160), dp(240));
        return new RectF(margin, margin, margin + panelWidth, margin + panelHeight);
    }

    private RectF weatherPanel(int width, int height, RectF clockPanel) {
        float margin = dp(EDGE_PADDING_DP);
        if (width > height) {
            float left = clockPanel.right + margin;
            float right = width - margin;
            float panelHeight = clamp(height * 0.30f, dp(190), dp(285));
            return new RectF(left, margin, right, margin + panelHeight);
        }
        float top = clockPanel.bottom + margin;
        float panelHeight = clamp(height * 0.28f, dp(250), dp(360));
        return new RectF(margin, top, width - margin, top + panelHeight);
    }

    private void drawClock(Canvas canvas, RectF bounds, long now, int width) {
        String time = timeFormat.format(new Date(now));
        String hour = time.substring(0, 2);
        String minute = time.substring(2);
        float clockSize = clamp(width * 0.055f, dp(58), dp(118));
        Paint.FontMetrics metrics = setText(clockSize, Color.WHITE, true);
        float baseline = bounds.top + dp(18) - metrics.ascent;
        float left = bounds.left + dp(24);
        canvas.drawText(hour, left, baseline, paint);
        float hourWidth = paint.measureText(hour);
        setText(clockSize * 0.94f, 0xE8FFFFFF, false);
        canvas.drawText(minute, left + hourWidth + dp(2), baseline, paint);
        if (showSeconds) {
            setText(clockSize * 0.34f, 0xAAFFFFFF, false);
            String seconds = new SimpleDateFormat("ss", Locale.getDefault()).format(new Date(now));
            canvas.drawText(seconds, left + hourWidth + paint.measureText(minute) + dp(18), baseline - clockSize * 0.42f, paint);
        }
        setText(clamp(width * 0.018f, dp(22), dp(36)), 0xB8FFFFFF, false);
        canvas.drawText(dateFormat.format(new Date(now)), left + dp(2), baseline + dp(48), paint);
    }

    private void drawWeather(Canvas canvas, RectF bounds, int width) {
        float left = bounds.left + dp(22);
        float top = bounds.top + dp(22);
        float todayWidth = (bounds.width() - dp(44)) * 0.46f;
        float iconSize = clamp(width * 0.030f, dp(42), dp(72));
        weatherIconPainter.draw(canvas, weatherData.weatherCode, left + iconSize * 0.48f, top + iconSize * 0.48f, iconSize);
        setText(clamp(width * 0.020f, dp(22), dp(36)), 0xE8FFFFFF, false);
        canvas.drawText(weatherData.cityName + "  " + Math.round(weatherData.temperatureC) + "°C", left + iconSize + dp(18), top + dp(30), paint);
        setText(clamp(width * 0.014f, dp(16), dp(24)), 0xB8FFFFFF, false);
        canvas.drawText(weatherData.descriptionRu, left + iconSize + dp(18), top + dp(64), paint);
        canvas.drawText("день " + Math.round(weatherData.todayMaxTempC) + "° / ночь " + Math.round(weatherData.todayMinTempC) + "°", left, top + dp(108), paint);
        canvas.drawText("влажн. " + valueOrDash(weatherData.humidityPercent, "%") + "   давл. " + pressureText(weatherData.pressureHpa), left, top + dp(136), paint);
        canvas.drawText("осадки " + valueOrDash(weatherData.precipitationProbability, "%"), left, top + dp(164), paint);

        int count = Math.min(5, weatherData.forecast.size());
        if (count == 0) {
            return;
        }
        float cardsLeft = bounds.left + dp(22) + todayWidth;
        float cardGap = dp(8);
        float cardWidth = (bounds.right - dp(22) - cardsLeft - cardGap * (count - 1)) / count;
        float cardTop = bounds.top + dp(24);
        float cardHeight = bounds.height() - dp(48);
        for (int i = 0; i < count; i++) {
            ForecastDay day = weatherData.forecast.get(i);
            float x = cardsLeft + i * (cardWidth + cardGap);
            drawForecastCard(canvas, day, x, cardTop, cardWidth, cardHeight);
        }
    }

    private void drawSettingsButton(Canvas canvas, int width, int height) {
        float size = dp(SETTINGS_SIZE_DP);
        float margin = dp(EDGE_PADDING_DP);
        float left = width - margin - size;
        float top = height - margin - size;
        panel.set(left, top, left + size, top + size);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x28000000);
        canvas.drawRoundRect(panel, dp(PANEL_RADIUS_DP), dp(PANEL_RADIUS_DP), paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x77FFFFFF);
        float barLeft = left + size * 0.28f;
        float barRight = left + size * 0.72f;
        for (int i = 0; i < 3; i++) {
            float y = top + size * (0.34f + i * 0.16f);
            canvas.drawRect(barLeft, y, barRight, y + dp(1.4f), paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawForecastCard(Canvas canvas, ForecastDay day, float left, float top, float width, float height) {
        drawPanel(canvas, left, top, left + width, top + height, 0x30000000);
        setText(dp(13), 0xCFFFFFFF, true);
        canvas.drawText(day.date.length() >= 10 ? day.date.substring(5) : day.date, left + dp(8), top + dp(24), paint);
        weatherIconPainter.draw(canvas, day.weatherCode, left + width * 0.50f, top + height * 0.46f, Math.min(width * 0.62f, dp(42)));
        setText(dp(14), 0xE8FFFFFF, false);
        canvas.drawText(Math.round(day.maxTempC) + "°/" + Math.round(day.minTempC) + "°", left + dp(8), top + height - dp(14), paint);
    }

    private void drawWeatherStatus(Canvas canvas, int width, int height) {
        String text = weatherStatus.length() == 0 ? "Загрузка погоды" : weatherStatus;
        if (text.indexOf("failed") >= 0 && System.currentTimeMillis() - weatherStatusMillis > 60000L) {
            return;
        }
        setText(dp(10), 0x554A4F55, false);
        canvas.drawText(text, dp(18), height - dp(18), paint);
    }

    private String valueOrDash(int value, String suffix) {
        return value <= 0 ? "--" : value + suffix;
    }

    private String pressureText(double value) {
        return value <= 0.0 ? "--" : Math.round(value) + " гПа";
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
