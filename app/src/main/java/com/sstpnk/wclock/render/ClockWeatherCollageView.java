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
    private static final float SETTINGS_MARGIN_DP = 16.0f;
    private static final float SETTINGS_SIZE_DP = 38.0f;
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
    private int framePanSpeedPxPerSecond = 20;
    private boolean showClock = true;
    private boolean showWeather = true;
    private boolean showForecast = true;
    private boolean showSeconds;
    private String weatherIconStyle = "outline";
    private float clockPanelBackgroundAlpha = 0.56f;
    private float weatherPanelBackgroundAlpha = 0.56f;
    private String weatherStatus = "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430 \u043f\u043e\u0433\u043e\u0434\u044b";
    private long weatherStatusMillis;
    private boolean drawCollage = true;

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

    public void setDrawCollage(boolean drawCollage) {
        this.drawCollage = drawCollage;
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
        setDisplaySettings(collageEnabled, photoDisplayMode, photoOrderMode, maxVisiblePhotos, photoChangeSeconds, framePanSpeedPxPerSecond, showSeconds, weatherIconStyle, clockPanelBackgroundAlpha);
    }

    public void setDisplaySettings(boolean collageEnabled, String photoDisplayMode, String photoOrderMode, int maxVisiblePhotos, int photoChangeSeconds, boolean showSeconds, String weatherIconStyle, float panelBackgroundAlpha) {
        setDisplaySettings(collageEnabled, photoDisplayMode, photoOrderMode, maxVisiblePhotos, photoChangeSeconds, framePanSpeedPxPerSecond, showSeconds, weatherIconStyle, panelBackgroundAlpha);
    }

    public void setDisplaySettings(boolean collageEnabled, String photoDisplayMode, String photoOrderMode, int maxVisiblePhotos, int photoChangeSeconds, int framePanSpeedPxPerSecond, boolean showSeconds, String weatherIconStyle, float panelBackgroundAlpha) {
        setDisplaySettings(collageEnabled, true, true, true, photoDisplayMode, photoOrderMode, maxVisiblePhotos, photoChangeSeconds, framePanSpeedPxPerSecond, showSeconds, weatherIconStyle, panelBackgroundAlpha);
    }

    public void setDisplaySettings(boolean collageEnabled, boolean showClock, boolean showWeather, boolean showForecast, String photoDisplayMode, String photoOrderMode, int maxVisiblePhotos, int photoChangeSeconds, int framePanSpeedPxPerSecond, boolean showSeconds, String weatherIconStyle, float panelBackgroundAlpha) {
        setDisplaySettings(collageEnabled, showClock, showWeather, showForecast, photoDisplayMode, photoOrderMode, maxVisiblePhotos, photoChangeSeconds, framePanSpeedPxPerSecond, showSeconds, weatherIconStyle, panelBackgroundAlpha, panelBackgroundAlpha);
    }

    public void setDisplaySettings(boolean collageEnabled, boolean showClock, boolean showWeather, boolean showForecast, String photoDisplayMode, String photoOrderMode, int maxVisiblePhotos, int photoChangeSeconds, int framePanSpeedPxPerSecond, boolean showSeconds, String weatherIconStyle, float clockPanelBackgroundAlpha, float weatherPanelBackgroundAlpha) {
        this.collageEnabled = collageEnabled;
        this.showClock = showClock;
        this.showWeather = showWeather;
        this.showForecast = showForecast;
        this.photoDisplayMode = "frame".equals(photoDisplayMode) ? "frame" : "photowall";
        this.photoOrderMode = "sequential".equals(photoOrderMode) ? "sequential" : "random";
        this.maxVisiblePhotos = Math.max(1, Math.min(50, maxVisiblePhotos));
        this.photoChangeSeconds = Math.max(1, photoChangeSeconds);
        this.framePanSpeedPxPerSecond = Math.max(4, Math.min(48, framePanSpeedPxPerSecond));
        this.showSeconds = showSeconds;
        this.weatherIconStyle = "flat".equals(weatherIconStyle) ? "flat" : "outline";
        this.clockPanelBackgroundAlpha = Math.max(0.0f, Math.min(0.85f, clockPanelBackgroundAlpha));
        this.weatherPanelBackgroundAlpha = Math.max(0.0f, Math.min(0.85f, weatherPanelBackgroundAlpha));
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
        if (drawCollage) {
            collageEngine.setSource(collageEnabled ? photoFolderPath : "", collageEnabled ? photoFolderUri : "");
            collageEngine.draw(canvas, now, collageEnabled, photoDisplayMode, photoOrderMode, maxVisiblePhotos, photoChangeSeconds, framePanSpeedPxPerSecond);
        }

        RectF clockPanel = null;
        if (showClock) {
            clockPanel = clockPanel(width, height);
            drawPanel(canvas, clockPanel.left, clockPanel.top, clockPanel.right, clockPanel.bottom, panelColor(clockPanelBackgroundAlpha));
            drawClock(canvas, clockPanel, now, width);
        }

        if (showWeather && weatherData != null) {
            RectF weatherPanel = weatherPanel(width, height, clockPanel);
            drawPanel(canvas, weatherPanel.left, weatherPanel.top, weatherPanel.right, weatherPanel.bottom, panelColor(weatherPanelBackgroundAlpha));
            drawWeather(canvas, weatherPanel, width);
        }
        if (showWeather) {
            drawWeatherStatus(canvas, width, height);
        }

        drawSettingsButton(canvas, width, height);
    }

    public boolean isSettingsButtonHit(float x, float y) {
        RectF bounds = settingsButtonBounds(getWidth(), getHeight());
        return bounds.contains(x, y);
    }

    private RectF clockPanel(int width, int height) {
        float margin = dp(EDGE_PADDING_DP);
        float contentWidth = clockContentWidth(width);
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
        float bottom = clockPanel == null ? bottomPanelEdge(height) : clockPanel.bottom;
        if (width > height) {
            float panelWidth = showForecast ? clamp(width * 0.60f, dp(540), dp(750)) : clamp(width * 0.43f, dp(390), dp(560));
            float right = width - margin - (burnInZoneIndex % 2) * dp(14);
            float left = clockPanel == null ? right - panelWidth : Math.max(clockPanel.right + margin, right - panelWidth);
            float panelHeight = showForecast ? clamp(height * 0.41f, dp(285), dp(390)) : clamp(height * 0.23f, dp(168), dp(225));
            return new RectF(left, bottom - panelHeight, right, bottom);
        }
        float panelHeight = showForecast ? clamp(height * 0.39f, dp(345), dp(510)) : clamp(height * 0.21f, dp(190), dp(285));
        float top = clockPanel == null ? bottom - panelHeight : clockPanel.bottom + dp(14);
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

        float clockSize = clockSize(width);
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
        float textLeft = left + weatherHeaderTextOffset();

        weatherIconPainter.draw(canvas, weatherData.weatherCode, left + iconSize * 0.50f, top + iconSize * 0.66f, iconSize, weatherIconStyle);
        setText(clamp(width * 0.024f, dp(27), dp(45)), 0xDFFFFFFF, true);
        canvas.drawText(weatherData.cityName + "  " + Math.round(weatherData.temperatureC) + "\u00b0", textLeft, top + weatherCityBaselineOffset(), paint);

        setText(weatherDescriptionTextSize(width), 0xB8FFFFFF, false);
        canvas.drawText(weatherData.descriptionRu, textLeft, top + weatherDescriptionBaselineOffset(), paint);

        setText(weatherDetailTextSize(width), 0xAFFFFFFF, false);
        float detailLeft = left + weatherDetailTextOffset();
        float y = top + weatherFirstDetailBaselineOffset();
        if (hasTodayRange()) {
            canvas.drawText("\u0434\u0435\u043d\u044c " + Math.round(weatherData.todayMaxTempC) + "\u00b0 / \u043d\u043e\u0447\u044c " + Math.round(weatherData.todayMinTempC) + "\u00b0", detailLeft, y, paint);
            y += weatherDetailLineGap();
        }
        if (weatherData.humidityPercent > 0 || weatherData.pressureHpa > 0.0) {
            canvas.drawText("\u0432\u043b\u0430\u0436\u043d. " + valueOrDash(weatherData.humidityPercent, "%") + "   \u0434\u0430\u0432\u043b. " + pressureText(weatherData.pressureHpa), detailLeft, y, paint);
            y += weatherDetailLineGap();
        }
        if (weatherData.precipitationProbability > 0) {
            canvas.drawText("\u043e\u0441\u0430\u0434\u043a\u0438 " + weatherData.precipitationProbability + "%", detailLeft, y, paint);
            y += weatherDetailLineGap();
        }

        int count = showForecast ? Math.min(5, weatherData.forecast.size()) : 0;
        if (count == 0) {
            return;
        }
        float cardGap = dp(9);
        float cardsLeft = left;
        float cardsRight = bounds.right - padding;
        float cardWidth = (cardsRight - cardsLeft - cardGap * (count - 1)) / count;
        float desiredCardHeight = clamp(bounds.height() * 0.36f, dp(99), dp(138));
        float cardTop = forecastCardTop(bounds, y, desiredCardHeight);
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
        drawPanel(canvas, left, top, left + width, top + height, panelColor(weatherPanelBackgroundAlpha * 0.55f));
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
        RectF bounds = settingsButtonBounds(width, height);
        float size = bounds.width();
        float left = bounds.left;
        float top = bounds.top;
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

    private RectF settingsButtonBounds(int width, int height) {
        float size = dp(SETTINGS_SIZE_DP);
        float margin = dp(SETTINGS_MARGIN_DP);
        return new RectF(width - margin - size, height - margin - size, width - margin, height - margin);
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

    private float weatherCityBaselineOffset() {
        return dp(40.5f);
    }

    private float weatherDescriptionBaselineOffset() {
        return weatherCityBaselineOffset() + weatherDetailLineGap();
    }

    private float weatherFirstDetailBaselineOffset() {
        return dp(106);
    }

    private float clockContentWidth(int width) {
        float clockSize = clockSize(width);
        float timeWidth = clockTimeWidth(clockSize) + dp(48);
        float dateWidth = clockDateWidth(width) + dp(52);
        if (showSeconds) {
            dateWidth += dp(46);
        }
        return Math.max(timeWidth, dateWidth);
    }

    private float clockSize(int width) {
        float minSizeDp = showSeconds ? 48.0f : 58.0f;
        return clamp(width * 0.055f, dp(minSizeDp), dp(118));
    }

    private float clockTimeWidth(float clockSize) {
        float width = 0.0f;
        setText(clockSize, 0xE8FFFFFF, true);
        width += paint.measureText("88") + dp(4);
        setText(clockSize * 0.86f, 0xC8FFFFFF, false);
        width += paint.measureText(":") + dp(4);
        setText(clockSize * 0.82f, 0xCFFFFFFF, false);
        width += paint.measureText("88");
        if (showSeconds) {
            width += dp(4);
            setText(clockSize * 0.36f, 0x8FFFFFFF, false);
            width += paint.measureText(":") + dp(3);
            setText(clockSize * 0.32f, 0x88FFFFFF, false);
            width += paint.measureText("88");
        }
        return width;
    }

    private float clockDateWidth(int width) {
        setText(clamp(width * 0.018f, dp(22), dp(36)), 0x98FFFFFF, false);
        return paint.measureText(dateFormat.format(new Date()));
    }

    private float weatherHeaderTextOffset() {
        return clamp(getWidth() * 0.0375f, dp(48), dp(81)) + dp(21);
    }

    private float weatherDetailTextOffset() {
        return weatherHeaderTextOffset();
    }

    private float weatherDescriptionTextSize(int width) {
        return clamp(width * 0.0165f, dp(19.5f), dp(30));
    }

    private float weatherDetailTextSize(int width) {
        return weatherDescriptionTextSize(width) * 0.96f;
    }

    private float forecastCardTop(RectF bounds, float currentWeatherBottom, float cardHeight) {
        float bottomGap = dp(18);
        float balancedTop = bounds.bottom - bottomGap - cardHeight;
        float centeredTop = currentWeatherBottom + (bounds.bottom - currentWeatherBottom - cardHeight) * 0.50f;
        return Math.min(balancedTop, Math.max(currentWeatherBottom + bottomGap, centeredTop));
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
