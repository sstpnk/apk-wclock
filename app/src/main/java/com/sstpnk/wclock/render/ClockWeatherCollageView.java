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
        RectF primary = overlayLayoutEngine.primaryPanel(burnInZoneIndex, width, height);
        drawPanel(canvas, primary.left, primary.top, primary.right, primary.bottom);
        paint.setColor(Color.WHITE);
        paint.setTextSize(Math.max(56.0f, width * 0.08f));
        canvas.drawText(timeFormat.format(new Date(now)), primary.left + 24, primary.top + 80, paint);
        paint.setTextSize(Math.max(22.0f, width * 0.025f));
        canvas.drawText(dateFormat.format(new Date(now)), primary.left + 26, primary.top + 126, paint);

        drawPanel(canvas, width * 0.58f, 32, width - 32, height * 0.30f);
        paint.setTextSize(Math.max(24.0f, width * 0.025f));
        String weatherLine = weatherData == null
                ? "Погода загружается"
                : weatherData.cityName + "  " + Math.round(weatherData.temperatureC) + "°C";
        String description = weatherData == null ? "" : weatherData.descriptionRu + (weatherData.stale ? " · устарело" : "");
        if (weatherData != null) {
            weatherIconPainter.draw(canvas, weatherData.weatherCode, width * 0.62f, 92, 72);
        }
        canvas.drawText(weatherLine, width * 0.66f, 86, paint);
        canvas.drawText(description, width * 0.60f, 126, paint);
        canvas.drawText(forecastLine(), width * 0.60f, 166, paint);
    }

    private void drawPanel(Canvas canvas, float left, float top, float right, float bottom) {
        panel.set(left, top, right, bottom);
        paint.setColor(0x99000000);
        canvas.drawRoundRect(panel, 8, 8, paint);
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

    @Override
    protected void onDetachedFromWindow() {
        collageEngine.recycle();
        super.onDetachedFromWindow();
    }
}
