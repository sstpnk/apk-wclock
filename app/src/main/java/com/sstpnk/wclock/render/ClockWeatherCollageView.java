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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class ClockWeatherCollageView extends View {
    private static final float PANEL_RADIUS_DP = 8.0f;
    private static final float EDGE_PADDING_DP = 24.0f;
    private static final float SETTINGS_SIZE_DP = 30.0f;
    private static final float WEATHER_UI_SCALE = 1.5f;

    private final CollageEngine collageEngine;
    private final WeatherIconPainter weatherIconPainter = new WeatherIconPainter();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF panel = new RectF();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMMM", new Locale("ru"));
    private String photoFolderPath = "";
    private String photoFolderUri = "";
    private WeatherData weatherData;
    private int burnInZoneIndex;
    private boolean collageEnabled = true;
    private String photoDisplayMode = "photowall";
    private String photoOrderMode = "random";
    private int maxVisiblePhotos = 18;
    private int photoChangeSeconds = 20;
    private boolean showSeconds;
    private String weatherIconStyle = "outline";
    private float panelBackgroundAlpha = 0.56f;
    private String weatherStatus = "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430 \u043f\u043e\u0433\u043e\u0434\u044b";
    private long weatherStatusMillis;

    public ClockWeatherCollageView(Context context) {
        super(context);
        collageEngine = new CollageEngine(context.getContentResolver());
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    public void setPhotoSource(String path, String uri) {
        this.photoFolderPath = path == null ? "" : path;
        this.photoFolderUri = uri == null ? "" : uri;
    }

    public void setWeatherData(WeatherData weatherData) {
        this.weatherData = weatherData;
    }

    public void setBurnInZoneIndex(int burnInZoneIndex) {
        this.burnInZoneIndex = burnInZoneIndex;
    }

    public void setDisplaySettings(boolean collageEnabled, String photoDisplayMode, int maxVisiblePhotos, int photoChangeSeconds, boolean showSeconds, String weatherIconStyle) {
        setDisplaySettings(collageEnabled, photoDisplayMode, "random", maxVisiblePhotos, photoChangeSeconds, showSeconds, weatherIconStyle);
    }

    public void setDisplaySettings(boolean collageEnabled, String photoDisplayMode, String photoOrderMode, int maxVisiblePhotos, int photoChangeSeconds, boolean showSeconds, String weatherIconStyle) {
        setDisplaySettings(collageEnabled, photoDisplayMode, photoOrderMode, maxVisiblePhotos, photoChangeSeconds, showSeconds, weatherIconStyle, panelBackgroundAlpha);
    }

    public void setDisplaySettings(boolean collageEnabled, String photoDisplayMode, String photoOrderMode, int maxVisiblePhotos, int photoChangeSeconds, boolean showSeconds, String weatherIconStyle, float panelBackgroundAlpha) {
        this.collageEnabled = collageEnabled;
        this.photoDisplayMode = "frame".equals(photoDisplayMode) ? "frame" : "photowall";
        this.photoOrderMode = "sequential".equals(photoOrderMode) ? "sequential" : "random";
        this.maxVisiblePhotos = Math.max(1, Math.min(50, maxVisiblePhotos));
        this.photoChangeSeconds = Math.max(1, photoChangeSeconds);
        this.showSeconds = showSeconds;
        this.weatherIconStyle = "flat".equals(weatherIconStyle) ? "flat" : "outline";
        this.panelBackgroundAlpha = Math.max(0.0f, Math.min(0.85f, panelBackgroundAlpha));
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
        collageEngine.setSource(collageEnabled ? photoFolderPath : "", collageEnabled ? photoFolderUri : "");
        collageEngine.draw(canvas, now, collageEnabled, photoDisplayMode, photoOrderMode, maxVisiblePhotos, photoChangeSeconds);

        RectF clockPanel = clockPanel(width, height);
        drawPanel(canvas, clockPanel.left, clockPanel.top, clockPanel.right, clockPanel.bottom, panelColor(panelBackgroundAlpha));
        drawClock(canvas, clockPanel, now, width);

        if (weatherData != null) {
            RectF weatherPanel = weatherPanel(width, height, clockPanel);
            drawPanel(canvas, weatherPanel.left, weatherPanel.top, weatherPanel.right, weatherPanel.bottom, panelColor(panelBackgroundAlpha));
            drawWeather(canvas, weatherPanel, width);
        }
        drawWeatherStatus(canvas, width, height);

        drawSettingsButton(canvas, width, height);
    }

    public boolean isSettingsButtonHit(float x, float y) {
        float size = dp(SETTINGS_SIZE_DP);
        float margin = dp(EDGE_PADDING_DP);
        return x >= getWidth() - margin - size && x <= getWidth() - margin
                && y >= getHeight() - margin - size && y <= getHeight() - margin;
    }

    private RectF clockPanel(int width, int height) {
        float margin = dp(EDGE_PADDING_DP);
        float clockSize = clamp(width * 0.055f, dp(58), dp(118));
        float contentWidth = clockSize * (showSeconds ? 4.35f : 3.10f) + dp(58);
        if (width > height) {
            float panelWidth = Math.min(width * 0.42f, contentWidth);
            float panelHeight = clamp(height * 0.17f, dp(126), dp(176));
            float left = margin + (burnInZoneIndex % 2) * dp(18);
            float bottom = bottomPanelEdge(height);
            return new RectF(left, bottom - panelHeight, left + panelWidth, bottom);
        }
        float panelWidth = Math.min(width - margin * 2, contentWidth);
        float panelHeight = clamp(height * 0.13f, dp(124), dp(188));
        float weatherHeight = clamp(height * 0.26f, dp(230), dp(340));
        float groupTop = Math.max(margin, height - margin - panelHeight - dp(14) - weatherHeight);
        return new RectF(margin, groupTop, margin + panelWidth, groupTop + panelHeight);
    }

    private RectF weatherPanel(int width, int height, RectF clockPanel) {
        float margin = dp(EDGE_PADDING_DP);
        if (width > height) {
            float panelWidth = clamp(width * 0.60f, dp(540), dp(750));
            float right = width - margin - (burnInZoneIndex % 2) * dp(14);
            float left = Math.max(clockPanel.right + margin, right - panelWidth);
            float panelHeight = clamp(height * 0.41f, dp(285), dp(390));
            float bottom = clockPanel.bottom;
            return new RectF(left, bottom - panelHeight, right, bottom);
        }
        float top = clockPanel.bottom + dp(14);
        float panelHeight = clamp(height * 0.39f, dp(345), dp(510));
        return new RectF(margin, top, width - margin, top + panelHeight);
    }

    private float bottomPanelEdge(int height) {
        return height - dp(EDGE_PADDING_DP) - (burnInZoneIndex / 2) * dp(10);
    }

    private void drawPanel(Canvas canvas, float left, float top, float right, float bottom, int color) {
        panel.set(left, top, right, bottom);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRoundRect(panel, dp(PANEL_RADIUS_DP), dp(PANEL_RADIUS_DP), paint);
    }

    private void drawClock(Canvas canvas, RectF bounds, long now, int width) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);
        String hour = twoDigits(calendar.get(Calendar.HOUR_OF_DAY));
        String minute = twoDigits(calendar.get(Calendar.MINUTE));
        String second = twoDigits(calendar.get(Calendar.SECOND));
        boolean colonVisible = (now / 1000L) % 2L == 0L;

        float clockSize = clamp(width * 0.055f, dp(58), dp(118));
        Paint.FontMetrics metrics = setText(clockSize, 0xE8FFFFFF, true);
        float baseline = bounds.top + dp(16) - metrics.ascent;
        float x = bounds.left + dp(24);
        canvas.drawText(hour, x, baseline, paint);
        x += paint.measureText(hour) + dp(4);

        setText(clockSize * 0.86f, colonVisible ? 0xC8FFFFFF : 0x00FFFFFF, false);
        canvas.drawText(":", x, baseline, paint);
        x += paint.measureText(":") + dp(4);

        setText(clockSize * 0.82f, 0xCFFFFFFF, false);
        canvas.drawText(minute, x, baseline, paint);
        x += paint.measureText(minute) + dp(4);

        if (showSeconds) {
            setText(clockSize * 0.36f, colonVisible ? 0x8FFFFFFF : 0x00FFFFFF, false);
            canvas.drawText(":", x, baseline, paint);
            x += paint.measureText(":") + dp(3);
            setText(clockSize * 0.32f, 0x88FFFFFF, false);
            canvas.drawText(second, x, baseline, paint);
        }

        setText(clamp(width * 0.018f, dp(22), dp(36)), 0x98FFFFFF, false);
        canvas.drawText(dateFormat.format(new Date(now)), bounds.left + dp(26), baseline + dp(24), paint);
    }

    private void drawWeather(Canvas canvas, RectF bounds, int width) {
        float padding = dp(16 * WEATHER_UI_SCALE);
        float left = bounds.left + padding;
        float top = bounds.top + dp(12 * WEATHER_UI_SCALE);
        float iconSize = clamp(width * 0.0375f, dp(48), dp(81));

        weatherIconPainter.draw(canvas, weatherData.weatherCode, left + iconSize * 0.50f, top + iconSize * 0.66f, iconSize, weatherIconStyle);
        setText(clamp(width * 0.024f, dp(27), dp(45)), 0xDFFFFFFF, true);
        canvas.drawText(weatherData.cityName + "  " + Math.round(weatherData.temperatureC) + "\u00b0", left + iconSize + dp(21), top + dp(40.5f), paint);

        setText(clamp(width * 0.0165f, dp(19.5f), dp(30)), 0xB8FFFFFF, false);
        canvas.drawText(weatherData.descriptionRu, left + iconSize + dp(21), top + dp(61.5f), paint);

        float y = top + dp(93);
        if (hasTodayRange()) {
            canvas.drawText("\u0434\u0435\u043d\u044c " + Math.round(weatherData.todayMaxTempC) + "\u00b0 / \u043d\u043e\u0447\u044c " + Math.round(weatherData.todayMinTempC) + "\u00b0", left, y, paint);
            y += weatherDetailLineGap();
        }
        if (weatherData.humidityPercent > 0 || weatherData.pressureHpa > 0.0) {
            canvas.drawText("\u0432\u043b\u0430\u0436\u043d. " + valueOrDash(weatherData.humidityPercent, "%") + "   \u0434\u0430\u0432\u043b. " + pressureText(weatherData.pressureHpa), left, y, paint);
            y += weatherDetailLineGap();
        }
        if (weatherData.precipitationProbability > 0) {
            canvas.drawText("\u043e\u0441\u0430\u0434\u043a\u0438 " + weatherData.precipitationProbability + "%", left, y, paint);
            y += weatherDetailLineGap();
        }

        int count = Math.min(5, weatherData.forecast.size());
        if (count == 0) {
            return;
        }
        float cardGap = dp(9);
        float cardsLeft = left;
        float cardsRight = bounds.right - padding;
        float cardWidth = (cardsRight - cardsLeft - cardGap * (count - 1)) / count;
        float desiredCardHeight = clamp(bounds.height() * 0.36f, dp(99), dp(138));
        float cardTop = Math.max(y + dp(12), bounds.bottom - dp(18) - desiredCardHeight);
        float cardHeight = bounds.bottom - dp(18) - cardTop;
        for (int i = 0; i < count; i++) {
            ForecastDay day = weatherData.forecast.get(i);
            float x = cardsLeft + i * (cardWidth + cardGap);
            drawForecastCard(canvas, day, x, cardTop, cardWidth, cardHeight);
        }
    }

    private void drawForecastCard(Canvas canvas, ForecastDay day, float left, float top, float width, float height) {
        if (height < dp(81)) {
            return;
        }
        drawPanel(canvas, left, top, left + width, top + height, panelColor(panelBackgroundAlpha * 0.55f));
        setText(dp(15), 0xBFFFFFFF, true);
        paint.setTextAlign(Paint.Align.CENTER);
        float centerX = left + width * 0.50f;
        float centerY = top + height * 0.50f;
        canvas.drawText(forecastDate(day.date), centerX, centerY - dp(24), paint);
        weatherIconPainter.draw(canvas, day.weatherCode, centerX, centerY + dp(1.5f), Math.min(width * 0.46f, dp(42)), weatherIconStyle);
        setText(dp(16.5f), 0xD8FFFFFF, false);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(forecastTemperature(day), centerX, centerY + dp(49.5f), paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawSettingsButton(Canvas canvas, int width, int height) {
        float size = dp(SETTINGS_SIZE_DP);
        float margin = dp(EDGE_PADDING_DP);
        float left = width - margin - size;
        float top = height - margin - size;
        panel.set(left, top, left + size, top + size);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x16000000);
        canvas.drawRoundRect(panel, dp(PANEL_RADIUS_DP), dp(PANEL_RADIUS_DP), paint);
        paint.setColor(0x50FFFFFF);
        float barLeft = left + size * 0.28f;
        float barRight = left + size * 0.72f;
        for (int i = 0; i < 3; i++) {
            float y = top + size * (0.34f + i * 0.16f);
            canvas.drawRect(barLeft, y, barRight, y + dp(1.2f), paint);
        }
    }

    private void drawWeatherStatus(Canvas canvas, int width, int height) {
        String text = visibleWeatherStatusText(System.currentTimeMillis());
        if (text.length() == 0) {
            return;
        }
        setText(dp(10), 0x887C838A, false);
        canvas.drawText(text, dp(18), height - dp(18), paint);
    }

    private String visibleWeatherStatusText(long nowMillis) {
        String text = userFriendlyWeatherStatus(weatherStatus);
        if (text.length() == 0 && weatherData == null) {
            return "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430 \u043f\u043e\u0433\u043e\u0434\u044b";
        }
        if (text.startsWith("\u041e\u0448\u0438\u0431\u043a\u0430") && nowMillis - weatherStatusMillis > 60000L) {
            return "";
        }
        return text;
    }

    private String userFriendlyWeatherStatus(String status) {
        if (status == null || status.length() == 0) {
            return "";
        }
        if (status.indexOf("failed") >= 0 || status.indexOf("Failed") >= 0 || status.indexOf("timeout") >= 0 || status.indexOf("Exception") >= 0) {
            String detail = status
                    .replace("Weather failed:", "")
                    .replace("Primary failed:", "")
                    .trim();
            if (detail.length() > 96) {
                detail = detail.substring(0, 96);
            }
            return detail.length() == 0
                    ? "\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u043e\u0433\u043e\u0434\u044b"
                    : "\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u043e\u0433\u043e\u0434\u044b: " + detail;
        }
        return status;
    }

    private boolean hasTodayRange() {
        return Math.abs(weatherData.todayMaxTempC - weatherData.todayMinTempC) >= 0.5;
    }

    private String valueOrDash(int value, String suffix) {
        return value <= 0 ? "--" : value + suffix;
    }

    private String pressureText(double value) {
        return value <= 0.0 ? "--" : Math.round(value * 0.750062) + " \u043c\u043c \u0440\u0442. \u0441\u0442.";
    }

    private float weatherDetailLineGap() {
        return dp(16.5f * WEATHER_UI_SCALE);
    }

    private int panelColor(float alphaFraction) {
        int alpha = Math.round(255.0f * Math.max(0.0f, Math.min(0.85f, alphaFraction)));
        return (alpha << 24);
    }

    private Paint.FontMetrics setText(float size, int color, boolean bold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setFakeBoldText(bold);
        paint.setTextSize(size);
        paint.setTextAlign(Paint.Align.LEFT);
        return paint.getFontMetrics();
    }

    private String twoDigits(int value) {
        return value < 10 ? "0" + value : Integer.toString(value);
    }

    private String forecastDate(String value) {
        if (value != null && value.length() >= 10) {
            return value.substring(8, 10) + "." + value.substring(5, 7);
        }
        return value == null ? "" : value;
    }

    private String forecastTemperature(ForecastDay day) {
        long max = Math.round(day.maxTempC);
        long min = Math.round(day.minTempC);
        if (Math.abs(day.maxTempC - day.minTempC) < 0.5) {
            return max + "\u00b0";
        }
        return max + "\u00b0/" + min + "\u00b0";
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
