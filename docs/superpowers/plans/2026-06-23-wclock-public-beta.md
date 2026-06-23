# WClock Public Beta Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first public beta of WClock: an Android 4.4 compatible fullscreen and DreamService clock/weather/photo-collage app without Google Play Services.

**Architecture:** One APK exposes two hosts, `MainActivity` and `WClockDreamService`, that share one render/data core. Rendering is Canvas-based, weather is provider-based with Open-Meteo primary and MET Norway fallback, settings are persisted in `SharedPreferences`, and runtime code stays on platform Android APIs available on API 19.

**Tech Stack:** Java, Android Gradle Plugin, minSdk 19, compileSdk 35, JUnit 4 local tests, platform `SharedPreferences`, `HttpURLConnection`, `org.json`, Android Canvas.

---

## Reference Spec

Implement against:

- `docs/superpowers/specs/2026-06-23-wclock-prd-architecture-roadmap.md`

Hard constraints:

- Android 4.4 support is strict: `minSdkVersion 19`.
- No Google Play Services dependency.
- No runtime AndroidX dependency.
- No Yandex Weather.
- Both fullscreen activity and `DreamService` must exist.
- Weather must work without API keys.
- Text UI must be usable with touch and D-pad.

## File Structure

Create this project structure:

```text
settings.gradle
build.gradle
gradle.properties
README.md
docs/BETA_CHECKLIST.md
app/build.gradle
app/src/main/AndroidManifest.xml
app/src/main/java/com/sstpnk/wclock/host/MainActivity.java
app/src/main/java/com/sstpnk/wclock/host/WClockDreamService.java
app/src/main/java/com/sstpnk/wclock/render/ClockWeatherCollageView.java
app/src/main/java/com/sstpnk/wclock/render/RenderController.java
app/src/main/java/com/sstpnk/wclock/render/OverlayLayoutEngine.java
app/src/main/java/com/sstpnk/wclock/render/BurnInController.java
app/src/main/java/com/sstpnk/wclock/render/WeatherIconPainter.java
app/src/main/java/com/sstpnk/wclock/collage/BitmapLoader.java
app/src/main/java/com/sstpnk/wclock/collage/CollageEngine.java
app/src/main/java/com/sstpnk/wclock/collage/CollageLayout.java
app/src/main/java/com/sstpnk/wclock/collage/PhotoItem.java
app/src/main/java/com/sstpnk/wclock/collage/PhotoScanner.java
app/src/main/java/com/sstpnk/wclock/weather/ForecastDay.java
app/src/main/java/com/sstpnk/wclock/weather/MetNorwayProvider.java
app/src/main/java/com/sstpnk/wclock/weather/OpenMeteoProvider.java
app/src/main/java/com/sstpnk/wclock/weather/WeatherCodeMapper.java
app/src/main/java/com/sstpnk/wclock/weather/WeatherData.java
app/src/main/java/com/sstpnk/wclock/weather/WeatherProvider.java
app/src/main/java/com/sstpnk/wclock/weather/WeatherRepository.java
app/src/main/java/com/sstpnk/wclock/settings/FileBrowserActivity.java
app/src/main/java/com/sstpnk/wclock/settings/SettingsActivity.java
app/src/main/java/com/sstpnk/wclock/settings/SettingsRepository.java
app/src/main/java/com/sstpnk/wclock/brightness/BrightnessController.java
app/src/main/java/com/sstpnk/wclock/brightness/BrightnessSchedule.java
app/src/main/java/com/sstpnk/wclock/util/NetworkClient.java
app/src/main/java/com/sstpnk/wclock/util/TimeProvider.java
app/src/main/res/values/colors.xml
app/src/main/res/values/strings.xml
app/src/main/res/xml/wclock_dream.xml
app/src/test/java/com/sstpnk/wclock/brightness/BrightnessScheduleTest.java
app/src/test/java/com/sstpnk/wclock/collage/PhotoScannerTest.java
app/src/test/java/com/sstpnk/wclock/render/BurnInControllerTest.java
app/src/test/java/com/sstpnk/wclock/settings/SettingsRepositoryTest.java
app/src/test/java/com/sstpnk/wclock/weather/OpenMeteoProviderTest.java
app/src/test/java/com/sstpnk/wclock/weather/WeatherCodeMapperTest.java
app/src/test/java/com/sstpnk/wclock/weather/WeatherRepositoryTest.java
app/src/test/resources/open_meteo_forecast.json
app/src/test/resources/met_norway_forecast.json
```

Responsibility boundaries:

- `host`: Android lifecycle and window flags only.
- `render`: drawing, animation ticks, overlay placement, burn-in motion.
- `collage`: file discovery, bitmap loading, photo layout, photo animation.
- `weather`: provider parsing, fallback, cache, display weather model.
- `settings`: persisted configuration and setup UI.
- `brightness`: schedule calculation and applying window brightness.
- `util`: small platform adapters with no product decisions.

## Verification Commands

Use these repeatedly:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath
```

Expected dependency check:

- No dependency group or artifact containing `play-services`.
- No runtime dependency on `androidx`.

---

### Task 1: Project Foundation

**Files:**

- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `gradle.properties`
- Create: `app/build.gradle`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/xml/wclock_dream.xml`

- [ ] **Step 1: Create Gradle settings**

Write `settings.gradle`:

```groovy
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = 'apk-wclock'
include ':app'
```

Write root `build.gradle`:

```groovy
plugins {
    id 'com.android.application' version '8.5.2' apply false
}
```

Write `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=false
android.enableJetifier=false
```

- [ ] **Step 2: Create Android app build file**

Write `app/build.gradle`:

```groovy
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.sstpnk.wclock'
    compileSdk 35

    defaultConfig {
        applicationId 'com.sstpnk.wclock'
        minSdk 19
        targetSdk 19
        versionCode 1
        versionName '0.1.0-beta1'
        testInstrumentationRunner 'android.test.InstrumentationTestRunner'
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    testImplementation 'junit:junit:4.13.2'
}
```

- [ ] **Step 3: Create manifest and resources**

Write `app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:theme="@style/AppTheme">

        <activity
            android:name=".settings.FileBrowserActivity"
            android:screenOrientation="sensor"
            android:exported="false" />

        <activity
            android:name=".settings.SettingsActivity"
            android:screenOrientation="sensor"
            android:exported="false" />

        <activity
            android:name=".host.MainActivity"
            android:screenOrientation="sensor"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".host.WClockDreamService"
            android:exported="true"
            android:label="@string/app_name"
            android:permission="android.permission.BIND_DREAM_SERVICE">
            <intent-filter>
                <action android:name="android.service.dreams.DreamService" />
            </intent-filter>
            <meta-data
                android:name="android.service.dream"
                android:resource="@xml/wclock_dream" />
        </service>
    </application>
</manifest>
```

Write `app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">WClock</string>
    <string name="settings_title">Настройки WClock</string>
    <string name="dream_settings_title">WClock</string>
</resources>
```

Write `app/src/main/res/values/colors.xml`:

```xml
<resources>
    <color name="black">#000000</color>
    <color name="white">#FFFFFF</color>
    <color name="panel_dark">#99000000</color>
</resources>
```

Write `app/src/main/res/xml/wclock_dream.xml`:

```xml
<dream xmlns:android="http://schemas.android.com/apk/res/android"
    android:settingsActivity="com.sstpnk.wclock.settings.SettingsActivity" />
```

- [ ] **Step 4: Add minimal platform theme**

Add to `app/src/main/res/values/styles.xml`:

```xml
<resources>
    <style name="AppTheme" parent="@android:style/Theme.Holo.NoActionBar">
        <item name="android:windowNoTitle">true</item>
    </style>
</resources>
```

- [ ] **Step 5: Build and inspect dependencies**

Run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath
```

Expected:

- Unit test task succeeds or reports no tests.
- Debug APK builds.
- Dependency output contains no `play-services`.
- Dependency output contains no `androidx`.

- [ ] **Step 6: Commit foundation**

Run:

```powershell
git add settings.gradle build.gradle gradle.properties app
git commit -m "chore: scaffold Android app"
```

---

### Task 2: Core Models, Time, Settings Defaults

**Files:**

- Create: `app/src/main/java/com/sstpnk/wclock/util/TimeProvider.java`
- Create: `app/src/main/java/com/sstpnk/wclock/settings/SettingsRepository.java`
- Create: `app/src/test/java/com/sstpnk/wclock/settings/SettingsRepositoryTest.java`

- [ ] **Step 1: Write settings defaults test**

Create `SettingsRepositoryTest.java`:

```java
package com.sstpnk.wclock.settings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SettingsRepositoryTest {
    @Test
    public void defaultsAreValidForFirstLaunch() {
        SettingsRepository.Settings settings = SettingsRepository.Settings.defaults();

        assertEquals("Москва", settings.cityName);
        assertEquals(55.7558, settings.latitude, 0.0001);
        assertEquals(37.6173, settings.longitude, 0.0001);
        assertEquals(30, settings.weatherRefreshMinutes);
        assertEquals(8, settings.maxVisiblePhotos);
        assertTrue(settings.burnInMinMinutes >= 5);
        assertTrue(settings.burnInMaxMinutes <= 15);
        assertTrue(settings.nightOverlayAlpha >= 0.0f);
        assertTrue(settings.nightOverlayAlpha <= 1.0f);
    }

    @Test
    public void coordinateValidationRejectsInvalidValues() {
        assertTrue(SettingsRepository.isValidLatitude(55.75));
        assertTrue(SettingsRepository.isValidLongitude(37.61));
        assertTrue(!SettingsRepository.isValidLatitude(120.0));
        assertTrue(!SettingsRepository.isValidLongitude(220.0));
    }
}
```

- [ ] **Step 2: Run test and confirm failure**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.sstpnk.wclock.settings.SettingsRepositoryTest
```

Expected:

- Fails because `SettingsRepository` does not exist.

- [ ] **Step 3: Implement time provider**

Create `TimeProvider.java`:

```java
package com.sstpnk.wclock.util;

public interface TimeProvider {
    long nowMillis();

    final class SystemTimeProvider implements TimeProvider {
        @Override
        public long nowMillis() {
            return System.currentTimeMillis();
        }
    }
}
```

- [ ] **Step 4: Implement settings repository model and validation**

Create `SettingsRepository.java`:

```java
package com.sstpnk.wclock.settings;

import android.content.Context;
import android.content.SharedPreferences;

public final class SettingsRepository {
    private static final String PREFS = "wclock_settings";

    private final SharedPreferences prefs;

    public SettingsRepository(Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public Settings load() {
        Settings defaults = Settings.defaults();
        Settings settings = new Settings();
        settings.photoFolderPath = prefs.getString("photoFolderPath", defaults.photoFolderPath);
        settings.cityName = prefs.getString("cityName", defaults.cityName);
        settings.latitude = doubleFromPrefs("latitude", defaults.latitude);
        settings.longitude = doubleFromPrefs("longitude", defaults.longitude);
        settings.weatherRefreshMinutes = prefs.getInt("weatherRefreshMinutes", defaults.weatherRefreshMinutes);
        settings.maxVisiblePhotos = prefs.getInt("maxVisiblePhotos", defaults.maxVisiblePhotos);
        settings.photoChangeSeconds = prefs.getInt("photoChangeSeconds", defaults.photoChangeSeconds);
        settings.motionIntensity = prefs.getFloat("motionIntensity", defaults.motionIntensity);
        settings.burnInMinMinutes = prefs.getInt("burnInMinMinutes", defaults.burnInMinMinutes);
        settings.burnInMaxMinutes = prefs.getInt("burnInMaxMinutes", defaults.burnInMaxMinutes);
        settings.dayBrightness = prefs.getFloat("dayBrightness", defaults.dayBrightness);
        settings.eveningBrightness = prefs.getFloat("eveningBrightness", defaults.eveningBrightness);
        settings.nightBrightness = prefs.getFloat("nightBrightness", defaults.nightBrightness);
        settings.nightOverlayAlpha = prefs.getFloat("nightOverlayAlpha", defaults.nightOverlayAlpha);
        return settings.normalized();
    }

    public void save(Settings settings) {
        Settings safe = settings.normalized();
        prefs.edit()
                .putString("photoFolderPath", safe.photoFolderPath)
                .putString("cityName", safe.cityName)
                .putString("latitude", Double.toString(safe.latitude))
                .putString("longitude", Double.toString(safe.longitude))
                .putInt("weatherRefreshMinutes", safe.weatherRefreshMinutes)
                .putInt("maxVisiblePhotos", safe.maxVisiblePhotos)
                .putInt("photoChangeSeconds", safe.photoChangeSeconds)
                .putFloat("motionIntensity", safe.motionIntensity)
                .putInt("burnInMinMinutes", safe.burnInMinMinutes)
                .putInt("burnInMaxMinutes", safe.burnInMaxMinutes)
                .putFloat("dayBrightness", safe.dayBrightness)
                .putFloat("eveningBrightness", safe.eveningBrightness)
                .putFloat("nightBrightness", safe.nightBrightness)
                .putFloat("nightOverlayAlpha", safe.nightOverlayAlpha)
                .apply();
    }

    private double doubleFromPrefs(String key, double defaultValue) {
        try {
            return Double.parseDouble(prefs.getString(key, Double.toString(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static boolean isValidLatitude(double value) {
        return value >= -90.0 && value <= 90.0;
    }

    public static boolean isValidLongitude(double value) {
        return value >= -180.0 && value <= 180.0;
    }

    static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    static float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class Settings {
        public String photoFolderPath;
        public String cityName;
        public double latitude;
        public double longitude;
        public int weatherRefreshMinutes;
        public int maxVisiblePhotos;
        public int photoChangeSeconds;
        public float motionIntensity;
        public int burnInMinMinutes;
        public int burnInMaxMinutes;
        public float dayBrightness;
        public float eveningBrightness;
        public float nightBrightness;
        public float nightOverlayAlpha;

        public static Settings defaults() {
            Settings settings = new Settings();
            settings.photoFolderPath = "";
            settings.cityName = "Москва";
            settings.latitude = 55.7558;
            settings.longitude = 37.6173;
            settings.weatherRefreshMinutes = 30;
            settings.maxVisiblePhotos = 8;
            settings.photoChangeSeconds = 60;
            settings.motionIntensity = 0.35f;
            settings.burnInMinMinutes = 5;
            settings.burnInMaxMinutes = 15;
            settings.dayBrightness = 0.85f;
            settings.eveningBrightness = 0.45f;
            settings.nightBrightness = 0.12f;
            settings.nightOverlayAlpha = 0.45f;
            return settings;
        }

        Settings normalized() {
            Settings safe = new Settings();
            safe.photoFolderPath = photoFolderPath == null ? "" : photoFolderPath;
            safe.cityName = cityName == null || cityName.trim().length() == 0 ? "Москва" : cityName.trim();
            safe.latitude = isValidLatitude(latitude) ? latitude : 55.7558;
            safe.longitude = isValidLongitude(longitude) ? longitude : 37.6173;
            safe.weatherRefreshMinutes = clampInt(weatherRefreshMinutes, 10, 240);
            safe.maxVisiblePhotos = clampInt(maxVisiblePhotos, 3, 16);
            safe.photoChangeSeconds = clampInt(photoChangeSeconds, 20, 600);
            safe.motionIntensity = clampFloat(motionIntensity, 0.0f, 1.0f);
            safe.burnInMinMinutes = clampInt(burnInMinMinutes, 5, 15);
            safe.burnInMaxMinutes = clampInt(burnInMaxMinutes, safe.burnInMinMinutes, 15);
            safe.dayBrightness = clampFloat(dayBrightness, 0.05f, 1.0f);
            safe.eveningBrightness = clampFloat(eveningBrightness, 0.05f, 1.0f);
            safe.nightBrightness = clampFloat(nightBrightness, 0.02f, 1.0f);
            safe.nightOverlayAlpha = clampFloat(nightOverlayAlpha, 0.0f, 0.85f);
            return safe;
        }
    }
}
```

- [ ] **Step 5: Run tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.sstpnk.wclock.settings.SettingsRepositoryTest
```

Expected:

- `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit settings core**

Run:

```powershell
git add app/src/main/java/com/sstpnk/wclock/util/TimeProvider.java app/src/main/java/com/sstpnk/wclock/settings/SettingsRepository.java app/src/test/java/com/sstpnk/wclock/settings/SettingsRepositoryTest.java
git commit -m "feat: add settings defaults"
```

---

### Task 3: Hosts And Shared Display Skeleton

**Files:**

- Create: `app/src/main/java/com/sstpnk/wclock/host/MainActivity.java`
- Create: `app/src/main/java/com/sstpnk/wclock/host/WClockDreamService.java`
- Create: `app/src/main/java/com/sstpnk/wclock/render/ClockWeatherCollageView.java`
- Create: `app/src/main/java/com/sstpnk/wclock/render/RenderController.java`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Implement shared render view placeholder**

Create `ClockWeatherCollageView.java`:

```java
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
```

- [ ] **Step 2: Implement render controller**

Create `RenderController.java`:

```java
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
```

- [ ] **Step 3: Implement fullscreen host**

Create `MainActivity.java`:

```java
package com.sstpnk.wclock.host;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.sstpnk.wclock.render.ClockWeatherCollageView;
import com.sstpnk.wclock.render.RenderController;
import com.sstpnk.wclock.settings.SettingsActivity;

public final class MainActivity extends Activity {
    private RenderController renderController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUi();

        ClockWeatherCollageView view = new ClockWeatherCollageView(this);
        renderController = new RenderController(view);
        setContentView(view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUi();
        renderController.start();
    }

    @Override
    protected void onPause() {
        renderController.stop();
        super.onPause();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_ENTER) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
}
```

- [ ] **Step 4: Implement DreamService host**

Create `WClockDreamService.java`:

```java
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
```

- [ ] **Step 5: Add temporary settings activity**

Create `SettingsActivity.java`:

```java
package com.sstpnk.wclock.settings;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public final class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView textView = new TextView(this);
        textView.setText("Настройки WClock");
        textView.setTextSize(28.0f);
        textView.setPadding(32, 32, 32, 32);
        textView.setFocusable(true);
        setContentView(textView);
    }
}
```

- [ ] **Step 6: Build debug APK**

Run:

```powershell
.\gradlew.bat assembleDebug
```

Expected:

- `BUILD SUCCESSFUL`.
- APK contains `MainActivity` and `WClockDreamService`.

- [ ] **Step 7: Commit hosts**

Run:

```powershell
git add app/src/main/java/com/sstpnk/wclock/host app/src/main/java/com/sstpnk/wclock/render app/src/main/java/com/sstpnk/wclock/settings/SettingsActivity.java app/src/main/AndroidManifest.xml app/src/main/res
git commit -m "feat: add fullscreen and dream hosts"
```

---

### Task 4: Weather Models, Code Mapping, And Provider Parsing

**Files:**

- Create: `app/src/main/java/com/sstpnk/wclock/weather/ForecastDay.java`
- Create: `app/src/main/java/com/sstpnk/wclock/weather/WeatherData.java`
- Create: `app/src/main/java/com/sstpnk/wclock/weather/WeatherProvider.java`
- Create: `app/src/main/java/com/sstpnk/wclock/weather/WeatherCodeMapper.java`
- Create: `app/src/main/java/com/sstpnk/wclock/weather/OpenMeteoProvider.java`
- Create: `app/src/main/java/com/sstpnk/wclock/weather/MetNorwayProvider.java`
- Create: `app/src/test/java/com/sstpnk/wclock/weather/WeatherCodeMapperTest.java`
- Create: `app/src/test/java/com/sstpnk/wclock/weather/OpenMeteoProviderTest.java`
- Create: `app/src/test/resources/open_meteo_forecast.json`

- [ ] **Step 1: Write weather code mapping tests**

Create `WeatherCodeMapperTest.java`:

```java
package com.sstpnk.wclock.weather;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WeatherCodeMapperTest {
    @Test
    public void mapsOpenMeteoCodesToRussianText() {
        assertEquals("Ясно", WeatherCodeMapper.openMeteoDescription(0));
        assertEquals("Переменная облачность", WeatherCodeMapper.openMeteoDescription(2));
        assertEquals("Туман", WeatherCodeMapper.openMeteoDescription(45));
        assertEquals("Дождь", WeatherCodeMapper.openMeteoDescription(63));
        assertEquals("Снег", WeatherCodeMapper.openMeteoDescription(75));
        assertEquals("Гроза", WeatherCodeMapper.openMeteoDescription(95));
        assertEquals("Неизвестно", WeatherCodeMapper.openMeteoDescription(999));
    }
}
```

- [ ] **Step 2: Add Open-Meteo fixture and parser test**

Create `open_meteo_forecast.json`:

```json
{
  "current": {
    "time": "2026-06-23T10:00",
    "temperature_2m": 21.4,
    "apparent_temperature": 20.8,
    "precipitation": 0.0,
    "weather_code": 2,
    "wind_speed_10m": 3.2,
    "wind_direction_10m": 180
  },
  "daily": {
    "time": ["2026-06-23", "2026-06-24", "2026-06-25", "2026-06-26", "2026-06-27"],
    "weather_code": [2, 61, 3, 0, 80],
    "temperature_2m_max": [23.0, 20.0, 24.0, 25.0, 22.0],
    "temperature_2m_min": [14.0, 13.0, 15.0, 16.0, 14.0],
    "precipitation_probability_max": [10, 80, 20, 0, 70],
    "wind_speed_10m_max": [4.0, 6.0, 3.0, 2.0, 5.0]
  }
}
```

Create `OpenMeteoProviderTest.java`:

```java
package com.sstpnk.wclock.weather;

import org.junit.Test;

import java.io.InputStream;
import java.util.Scanner;

import static org.junit.Assert.assertEquals;

public class OpenMeteoProviderTest {
    @Test
    public void parsesCurrentAndFiveDayForecast() throws Exception {
        String json = readResource("open_meteo_forecast.json");
        WeatherData data = OpenMeteoProvider.parseBody("Москва", json, 1000L);

        assertEquals("Open-Meteo", data.providerName);
        assertEquals("Москва", data.cityName);
        assertEquals(21.4, data.temperatureC, 0.01);
        assertEquals("Переменная облачность", data.descriptionRu);
        assertEquals(5, data.forecast.size());
        assertEquals("2026-06-24", data.forecast.get(1).date);
        assertEquals("Дождь", data.forecast.get(1).descriptionRu);
    }

    private static String readResource(String name) {
        InputStream stream = OpenMeteoProviderTest.class.getClassLoader().getResourceAsStream(name);
        Scanner scanner = new Scanner(stream, "UTF-8").useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }
}
```

- [ ] **Step 3: Run tests and confirm failure**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.sstpnk.wclock.weather.*
```

Expected:

- Fails because weather classes do not exist.

- [ ] **Step 4: Implement weather data models and mapper**

Create `ForecastDay.java`:

```java
package com.sstpnk.wclock.weather;

public final class ForecastDay {
    public final String date;
    public final int weatherCode;
    public final String descriptionRu;
    public final double minTempC;
    public final double maxTempC;
    public final int precipitationProbability;
    public final double windSpeed;

    public ForecastDay(String date, int weatherCode, String descriptionRu, double minTempC, double maxTempC, int precipitationProbability, double windSpeed) {
        this.date = date;
        this.weatherCode = weatherCode;
        this.descriptionRu = descriptionRu;
        this.minTempC = minTempC;
        this.maxTempC = maxTempC;
        this.precipitationProbability = precipitationProbability;
        this.windSpeed = windSpeed;
    }
}
```

Create `WeatherData.java`:

```java
package com.sstpnk.wclock.weather;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WeatherData {
    public final String providerName;
    public final String cityName;
    public final long updatedAtMillis;
    public final boolean stale;
    public final double temperatureC;
    public final double feelsLikeC;
    public final int weatherCode;
    public final String descriptionRu;
    public final double precipitationMm;
    public final double windSpeed;
    public final int windDirection;
    public final List<ForecastDay> forecast;

    public WeatherData(String providerName, String cityName, long updatedAtMillis, boolean stale, double temperatureC, double feelsLikeC, int weatherCode, String descriptionRu, double precipitationMm, double windSpeed, int windDirection, List<ForecastDay> forecast) {
        this.providerName = providerName;
        this.cityName = cityName;
        this.updatedAtMillis = updatedAtMillis;
        this.stale = stale;
        this.temperatureC = temperatureC;
        this.feelsLikeC = feelsLikeC;
        this.weatherCode = weatherCode;
        this.descriptionRu = descriptionRu;
        this.precipitationMm = precipitationMm;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.forecast = Collections.unmodifiableList(new ArrayList<ForecastDay>(forecast));
    }
}
```

Create `WeatherCodeMapper.java`:

```java
package com.sstpnk.wclock.weather;

public final class WeatherCodeMapper {
    private WeatherCodeMapper() {
    }

    public static String openMeteoDescription(int code) {
        if (code == 0) return "Ясно";
        if (code == 1 || code == 2) return "Переменная облачность";
        if (code == 3) return "Пасмурно";
        if (code == 45 || code == 48) return "Туман";
        if (code >= 51 && code <= 57) return "Морось";
        if (code >= 61 && code <= 67) return "Дождь";
        if (code >= 71 && code <= 77) return "Снег";
        if (code >= 80 && code <= 82) return "Ливень";
        if (code >= 85 && code <= 86) return "Снегопад";
        if (code >= 95 && code <= 99) return "Гроза";
        return "Неизвестно";
    }
}
```

- [ ] **Step 5: Implement provider interface and Open-Meteo parser**

Create `WeatherProvider.java`:

```java
package com.sstpnk.wclock.weather;

public interface WeatherProvider {
    String name();

    String buildUrl(double latitude, double longitude);

    WeatherData parse(String cityName, String body, long updatedAtMillis) throws Exception;
}
```

Create `OpenMeteoProvider.java`:

```java
package com.sstpnk.wclock.weather;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OpenMeteoProvider implements WeatherProvider {
    @Override
    public String name() {
        return "Open-Meteo";
    }

    @Override
    public String buildUrl(double latitude, double longitude) {
        return String.format(Locale.US,
                "https://api.open-meteo.com/v1/forecast?latitude=%.5f&longitude=%.5f&current=temperature_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,wind_direction_10m&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max&timezone=auto&forecast_days=5",
                latitude, longitude);
    }

    @Override
    public WeatherData parse(String cityName, String body, long updatedAtMillis) throws Exception {
        return parseBody(cityName, body, updatedAtMillis);
    }

    public static WeatherData parseBody(String cityName, String body, long updatedAtMillis) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONObject current = root.getJSONObject("current");
        int currentCode = current.getInt("weather_code");
        List<ForecastDay> forecast = new ArrayList<ForecastDay>();
        JSONObject daily = root.getJSONObject("daily");
        JSONArray dates = daily.getJSONArray("time");
        JSONArray codes = daily.getJSONArray("weather_code");
        JSONArray maxTemps = daily.getJSONArray("temperature_2m_max");
        JSONArray minTemps = daily.getJSONArray("temperature_2m_min");
        JSONArray precipitation = daily.getJSONArray("precipitation_probability_max");
        JSONArray wind = daily.getJSONArray("wind_speed_10m_max");
        int count = Math.min(5, dates.length());
        for (int i = 0; i < count; i++) {
            int code = codes.getInt(i);
            forecast.add(new ForecastDay(
                    dates.getString(i),
                    code,
                    WeatherCodeMapper.openMeteoDescription(code),
                    minTemps.getDouble(i),
                    maxTemps.getDouble(i),
                    precipitation.optInt(i, 0),
                    wind.optDouble(i, 0.0)));
        }
        return new WeatherData(
                "Open-Meteo",
                cityName,
                updatedAtMillis,
                false,
                current.getDouble("temperature_2m"),
                current.optDouble("apparent_temperature", current.getDouble("temperature_2m")),
                currentCode,
                WeatherCodeMapper.openMeteoDescription(currentCode),
                current.optDouble("precipitation", 0.0),
                current.optDouble("wind_speed_10m", 0.0),
                current.optInt("wind_direction_10m", 0),
                forecast);
    }
}
```

The test must call:

```java
WeatherData data = OpenMeteoProvider.parseBody("Москва", json, 1000L);
```

- [ ] **Step 6: Add MET Norway provider with conservative parser**

Create `MetNorwayProvider.java`:

```java
package com.sstpnk.wclock.weather;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MetNorwayProvider implements WeatherProvider {
    @Override
    public String name() {
        return "MET Norway";
    }

    @Override
    public String buildUrl(double latitude, double longitude) {
        return String.format(Locale.US,
                "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.5f&lon=%.5f",
                latitude, longitude);
    }

    @Override
    public WeatherData parse(String cityName, String body, long updatedAtMillis) throws Exception {
        JSONObject root = new JSONObject(body);
        JSONArray timeseries = root.getJSONObject("properties").getJSONArray("timeseries");
        JSONObject first = timeseries.getJSONObject(0);
        JSONObject instant = first.getJSONObject("data").getJSONObject("instant").getJSONObject("details");
        JSONObject next = first.getJSONObject("data").optJSONObject("next_1_hours");
        String symbol = "";
        if (next != null) {
            symbol = next.getJSONObject("summary").optString("symbol_code", "");
        }
        int code = codeFromSymbol(symbol);
        List<ForecastDay> forecast = new ArrayList<ForecastDay>();
        int count = Math.min(5, timeseries.length());
        for (int i = 0; i < count; i++) {
            JSONObject point = timeseries.getJSONObject(i);
            JSONObject details = point.getJSONObject("data").getJSONObject("instant").getJSONObject("details");
            int pointCode = code;
            JSONObject pointNext = point.getJSONObject("data").optJSONObject("next_6_hours");
            if (pointNext != null) {
                pointCode = codeFromSymbol(pointNext.getJSONObject("summary").optString("symbol_code", ""));
            }
            double temp = details.getDouble("air_temperature");
            forecast.add(new ForecastDay(
                    point.getString("time").substring(0, 10),
                    pointCode,
                    WeatherCodeMapper.openMeteoDescription(pointCode),
                    temp,
                    temp,
                    0,
                    details.optDouble("wind_speed", 0.0)));
        }
        return new WeatherData(
                "MET Norway",
                cityName,
                updatedAtMillis,
                false,
                instant.getDouble("air_temperature"),
                instant.getDouble("air_temperature"),
                code,
                WeatherCodeMapper.openMeteoDescription(code),
                0.0,
                instant.optDouble("wind_speed", 0.0),
                (int) instant.optDouble("wind_from_direction", 0.0),
                forecast);
    }

    private static int codeFromSymbol(String symbol) {
        if (symbol == null) return 3;
        if (symbol.indexOf("clearsky") >= 0) return 0;
        if (symbol.indexOf("cloudy") >= 0) return 3;
        if (symbol.indexOf("rain") >= 0) return 63;
        if (symbol.indexOf("snow") >= 0) return 75;
        if (symbol.indexOf("thunder") >= 0) return 95;
        if (symbol.indexOf("fog") >= 0) return 45;
        return 2;
    }
}
```

- [ ] **Step 7: Run weather tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.sstpnk.wclock.weather.WeatherCodeMapperTest --tests com.sstpnk.wclock.weather.OpenMeteoProviderTest
```

Expected:

- `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit weather parsing**

Run:

```powershell
git add app/src/main/java/com/sstpnk/wclock/weather app/src/test/java/com/sstpnk/wclock/weather app/src/test/resources
git commit -m "feat: parse open meteo weather"
```

---

### Task 5: Network Client, Weather Repository, Cache, Fallback

**Files:**

- Create: `app/src/main/java/com/sstpnk/wclock/util/NetworkClient.java`
- Create: `app/src/main/java/com/sstpnk/wclock/util/JsonParser.java`
- Create: `app/src/main/java/com/sstpnk/wclock/weather/WeatherRepository.java`
- Modify: `app/src/main/java/com/sstpnk/wclock/weather/MetNorwayProvider.java`
- Create: `app/src/test/java/com/sstpnk/wclock/weather/WeatherRepositoryTest.java`
- Create: `app/src/test/resources/met_norway_forecast.json`

- [ ] **Step 1: Write repository fallback test**

Create `WeatherRepositoryTest.java`:

```java
package com.sstpnk.wclock.weather;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WeatherRepositoryTest {
    @Test
    public void usesFallbackWhenPrimaryFails() {
        WeatherProvider failing = new FakeProvider("primary", true, 1.0);
        WeatherProvider fallback = new FakeProvider("fallback", false, 2.0);
        WeatherRepository repository = new WeatherRepository(null, failing, fallback);

        WeatherData data = repository.refreshForTest("Москва", 55.7, 37.6, 1000L);

        assertEquals("fallback", data.providerName);
        assertEquals(2.0, data.temperatureC, 0.01);
    }

    private static final class FakeProvider implements WeatherProvider {
        private final String name;
        private final boolean fail;
        private final double temp;

        FakeProvider(String name, boolean fail, double temp) {
            this.name = name;
            this.fail = fail;
            this.temp = temp;
        }

        public String name() { return name; }
        public String buildUrl(double latitude, double longitude) { return "memory://" + name; }
        public WeatherData parse(String cityName, String body, long updatedAtMillis) throws Exception {
            if (fail) throw new RuntimeException("fail");
            return new WeatherData(name, cityName, updatedAtMillis, false, temp, temp, 0, "Ясно", 0.0, 0.0, 0, java.util.Collections.<ForecastDay>emptyList());
        }
    }
}
```

- [ ] **Step 2: Implement network client**

Create `NetworkClient.java`:

```java
package com.sstpnk.wclock.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public final class NetworkClient {
    private final String userAgent;
    private final int timeoutMillis;

    public NetworkClient(String userAgent, int timeoutMillis) {
        this.userAgent = userAgent;
        this.timeoutMillis = timeoutMillis;
    }

    public String get(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(timeoutMillis);
        connection.setReadTimeout(timeoutMillis);
        connection.setRequestProperty("User-Agent", userAgent);
        connection.setRequestProperty("Accept", "application/json");
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        String body = readAll(stream);
        connection.disconnect();
        if (code < 200 || code >= 300) {
            throw new RuntimeException("HTTP " + code + ": " + body);
        }
        return body;
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }
}
```

- [ ] **Step 3: Implement repository**

Create `WeatherRepository.java`:

```java
package com.sstpnk.wclock.weather;

import com.sstpnk.wclock.util.NetworkClient;

public final class WeatherRepository {
    private final NetworkClient networkClient;
    private final WeatherProvider primary;
    private final WeatherProvider fallback;
    private WeatherData lastSuccessful;
    private String lastError = "";

    public WeatherRepository(NetworkClient networkClient, WeatherProvider primary, WeatherProvider fallback) {
        this.networkClient = networkClient;
        this.primary = primary;
        this.fallback = fallback;
    }

    public WeatherData refresh(String cityName, double latitude, double longitude, long nowMillis) {
        return tryProviders(cityName, latitude, longitude, nowMillis, true);
    }

    public WeatherData refreshForTest(String cityName, double latitude, double longitude, long nowMillis) {
        return tryProviders(cityName, latitude, longitude, nowMillis, false);
    }

    public WeatherData lastSuccessful() {
        return lastSuccessful;
    }

    public String lastError() {
        return lastError;
    }

    private WeatherData tryProviders(String cityName, double latitude, double longitude, long nowMillis, boolean fetchNetwork) {
        try {
            WeatherData data = fetch(primary, cityName, latitude, longitude, nowMillis, fetchNetwork);
            lastSuccessful = data;
            lastError = "";
            return data;
        } catch (Exception primaryError) {
            try {
                WeatherData data = fetch(fallback, cityName, latitude, longitude, nowMillis, fetchNetwork);
                lastSuccessful = data;
                lastError = "Primary failed: " + primaryError.getMessage();
                return data;
            } catch (Exception fallbackError) {
                lastError = "Weather failed: " + primaryError.getMessage() + "; fallback: " + fallbackError.getMessage();
                return staleCopy(lastSuccessful);
            }
        }
    }

    private WeatherData fetch(WeatherProvider provider, String cityName, double latitude, double longitude, long nowMillis, boolean fetchNetwork) throws Exception {
        String body = fetchNetwork ? networkClient.get(provider.buildUrl(latitude, longitude)) : "";
        return provider.parse(cityName, body, nowMillis);
    }

    private static WeatherData staleCopy(WeatherData data) {
        if (data == null) {
            return null;
        }
        return new WeatherData(data.providerName, data.cityName, data.updatedAtMillis, true, data.temperatureC, data.feelsLikeC, data.weatherCode, data.descriptionRu, data.precipitationMm, data.windSpeed, data.windDirection, data.forecast);
    }
}
```

- [ ] **Step 4: Run repository test**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.sstpnk.wclock.weather.WeatherRepositoryTest
```

Expected:

- `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit repository**

Run:

```powershell
git add app/src/main/java/com/sstpnk/wclock/util/NetworkClient.java app/src/main/java/com/sstpnk/wclock/weather/WeatherRepository.java app/src/test/java/com/sstpnk/wclock/weather/WeatherRepositoryTest.java
git commit -m "feat: add weather fallback repository"
```

---

### Task 6: Settings UI And File Browser

**Files:**

- Modify: `app/src/main/java/com/sstpnk/wclock/settings/SettingsActivity.java`
- Create: `app/src/main/java/com/sstpnk/wclock/settings/FileBrowserActivity.java`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Replace settings placeholder with D-pad friendly form**

Implement `SettingsActivity` as a vertical `ScrollView` with platform widgets:

```java
package com.sstpnk.wclock.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class SettingsActivity extends Activity {
    private SettingsRepository repository;
    private SettingsRepository.Settings settings;
    private TextView folderValue;
    private EditText city;
    private EditText latitude;
    private EditText longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new SettingsRepository(this);
        settings = repository.load();
        setContentView(buildContent());
    }

    private View buildContent() {
        ScrollView scrollView = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        scrollView.addView(root);

        TextView title = label("Настройки WClock", 28);
        root.addView(title);

        folderValue = label("Папка: " + valueOrDash(settings.photoFolderPath), 18);
        root.addView(folderValue);
        Button folder = button("Выбрать папку");
        folder.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivityForResult(new Intent(SettingsActivity.this, FileBrowserActivity.class), 10);
            }
        });
        root.addView(folder);

        city = edit("Город", settings.cityName);
        latitude = edit("Широта", Double.toString(settings.latitude));
        longitude = edit("Долгота", Double.toString(settings.longitude));
        root.addView(city);
        root.addView(latitude);
        root.addView(longitude);

        Button save = button("Сохранить");
        save.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveAndFinish();
            }
        });
        root.addView(save);
        return scrollView;
    }

    private TextView label(String text, int sp) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setPadding(0, 12, 0, 12);
        return view;
    }

    private EditText edit(String hint, String value) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setText(value);
        edit.setSingleLine(true);
        edit.setTextSize(20);
        return edit;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(20);
        button.setGravity(Gravity.CENTER);
        button.setFocusable(true);
        return button;
    }

    private String valueOrDash(String value) {
        return value == null || value.length() == 0 ? "не выбрана" : value;
    }

    private void saveAndFinish() {
        settings.cityName = city.getText().toString();
        settings.latitude = parseDouble(latitude.getText().toString(), settings.latitude);
        settings.longitude = parseDouble(longitude.getText().toString(), settings.longitude);
        repository.save(settings);
        finish();
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10 && resultCode == RESULT_OK && data != null) {
            settings.photoFolderPath = data.getStringExtra("path");
            folderValue.setText("Папка: " + valueOrDash(settings.photoFolderPath));
        }
    }
}
```

- [ ] **Step 2: Implement simple file browser**

Create `FileBrowserActivity.java`:

```java
package com.sstpnk.wclock.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class FileBrowserActivity extends Activity {
    private File current;
    private TextView path;
    private ArrayAdapter<String> adapter;
    private final List<File> entries = new ArrayList<File>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        current = Environment.getExternalStorageDirectory();
        setContentView(buildContent());
        load(current);
    }

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        path = new TextView(this);
        path.setTextSize(18);
        root.addView(path);

        Button choose = new Button(this);
        choose.setText("Выбрать эту папку");
        choose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent data = new Intent();
                data.putExtra("path", current.getAbsolutePath());
                setResult(RESULT_OK, data);
                finish();
            }
        });
        root.addView(choose);

        ListView list = new ListView(this);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                File selected = entries.get(position);
                if ("..".equals(selected.getName())) {
                    load(current.getParentFile() == null ? current : current.getParentFile());
                } else if (selected.isDirectory()) {
                    load(selected);
                }
            }
        });
        root.addView(list, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        return root;
    }

    private void load(File dir) {
        current = dir;
        path.setText(dir.getAbsolutePath());
        entries.clear();
        List<String> names = new ArrayList<String>();
        if (dir.getParentFile() != null) {
            entries.add(new File(".."));
            names.add("..");
        }
        File[] files = dir.listFiles();
        if (files != null) {
            List<File> sorted = Arrays.asList(files);
            Collections.sort(sorted);
            for (File file : sorted) {
                if (file.isDirectory() && !file.isHidden()) {
                    entries.add(file);
                    names.add(file.getName() + "/");
                }
            }
        }
        adapter.clear();
        adapter.addAll(names);
        adapter.notifyDataSetChanged();
    }
}
```

- [ ] **Step 3: Build and manually check D-pad focus**

Run:

```powershell
.\gradlew.bat assembleDebug
```

Manual check:

- Launch settings.
- Move focus through fields with Tab or D-pad.
- Open file browser.
- Select a folder.
- Save settings.
- Reopen settings and confirm values are retained.

- [ ] **Step 4: Commit settings UI**

Run:

```powershell
git add app/src/main/java/com/sstpnk/wclock/settings app/src/main/AndroidManifest.xml
git commit -m "feat: add settings and file browser"
```

---

### Task 7: Brightness Schedule And Burn-In Controllers

**Files:**

- Create: `app/src/main/java/com/sstpnk/wclock/brightness/BrightnessSchedule.java`
- Create: `app/src/main/java/com/sstpnk/wclock/brightness/BrightnessController.java`
- Create: `app/src/main/java/com/sstpnk/wclock/render/BurnInController.java`
- Create: `app/src/main/java/com/sstpnk/wclock/render/OverlayLayoutEngine.java`
- Create: `app/src/test/java/com/sstpnk/wclock/brightness/BrightnessScheduleTest.java`
- Create: `app/src/test/java/com/sstpnk/wclock/render/BurnInControllerTest.java`

- [ ] **Step 1: Write controller tests**

Create `BrightnessScheduleTest.java`:

```java
package com.sstpnk.wclock.brightness;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BrightnessScheduleTest {
    @Test
    public void choosesNightBrightnessDuringNightHours() {
        BrightnessSchedule schedule = new BrightnessSchedule(7, 19, 23, 0.8f, 0.45f, 0.12f);
        assertEquals(0.12f, schedule.brightnessForHour(2), 0.001f);
        assertEquals(0.8f, schedule.brightnessForHour(12), 0.001f);
        assertEquals(0.45f, schedule.brightnessForHour(20), 0.001f);
    }
}
```

Create `BurnInControllerTest.java`:

```java
package com.sstpnk.wclock.render;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class BurnInControllerTest {
    @Test
    public void returnsSafeZoneWithinScreenBounds() {
        BurnInController controller = new BurnInController(6);
        BurnInController.Zone zone = controller.zoneForIndex(2, 1920, 1080, 500, 220);
        assertTrue(zone.left >= 0);
        assertTrue(zone.top >= 0);
        assertTrue(zone.left + 500 <= 1920);
        assertTrue(zone.top + 220 <= 1080);
    }
}
```

- [ ] **Step 2: Implement brightness schedule**

Create `BrightnessSchedule.java`:

```java
package com.sstpnk.wclock.brightness;

public final class BrightnessSchedule {
    private final int dayStartHour;
    private final int eveningStartHour;
    private final int nightStartHour;
    private final float dayBrightness;
    private final float eveningBrightness;
    private final float nightBrightness;

    public BrightnessSchedule(int dayStartHour, int eveningStartHour, int nightStartHour, float dayBrightness, float eveningBrightness, float nightBrightness) {
        this.dayStartHour = dayStartHour;
        this.eveningStartHour = eveningStartHour;
        this.nightStartHour = nightStartHour;
        this.dayBrightness = dayBrightness;
        this.eveningBrightness = eveningBrightness;
        this.nightBrightness = nightBrightness;
    }

    public float brightnessForHour(int hour) {
        if (hour >= nightStartHour || hour < dayStartHour) {
            return nightBrightness;
        }
        if (hour >= eveningStartHour) {
            return eveningBrightness;
        }
        return dayBrightness;
    }
}
```

Create `BrightnessController.java`:

```java
package com.sstpnk.wclock.brightness;

import android.view.Window;
import android.view.WindowManager;

public final class BrightnessController {
    public void apply(Window window, float brightness) {
        WindowManager.LayoutParams params = window.getAttributes();
        params.screenBrightness = clamp(brightness);
        window.setAttributes(params);
    }

    private float clamp(float value) {
        return Math.max(0.02f, Math.min(1.0f, value));
    }
}
```

- [ ] **Step 3: Implement burn-in safe zones**

Create `BurnInController.java`:

```java
package com.sstpnk.wclock.render;

public final class BurnInController {
    private final int zoneCount;

    public BurnInController(int zoneCount) {
        this.zoneCount = Math.max(4, zoneCount);
    }

    public Zone zoneForIndex(int index, int screenWidth, int screenHeight, int panelWidth, int panelHeight) {
        int safeWidth = Math.max(1, screenWidth - panelWidth);
        int safeHeight = Math.max(1, screenHeight - panelHeight);
        int normalized = Math.abs(index % zoneCount);
        float xRatio;
        float yRatio;
        switch (normalized % 6) {
            case 0: xRatio = 0.04f; yRatio = 0.04f; break;
            case 1: xRatio = 0.54f; yRatio = 0.06f; break;
            case 2: xRatio = 0.08f; yRatio = 0.58f; break;
            case 3: xRatio = 0.50f; yRatio = 0.56f; break;
            case 4: xRatio = 0.28f; yRatio = 0.28f; break;
            default: xRatio = 0.64f; yRatio = 0.34f; break;
        }
        return new Zone((int) (safeWidth * xRatio), (int) (safeHeight * yRatio));
    }

    public static final class Zone {
        public final int left;
        public final int top;

        public Zone(int left, int top) {
            this.left = left;
            this.top = top;
        }
    }
}
```

Create `OverlayLayoutEngine.java`:

```java
package com.sstpnk.wclock.render;

import android.graphics.RectF;

public final class OverlayLayoutEngine {
    private final BurnInController burnInController = new BurnInController(6);

    public RectF primaryPanel(int zoneIndex, int width, int height) {
        int panelWidth = Math.max(360, (int) (width * 0.42f));
        int panelHeight = Math.max(180, (int) (height * 0.22f));
        BurnInController.Zone zone = burnInController.zoneForIndex(zoneIndex, width, height, panelWidth, panelHeight);
        return new RectF(zone.left, zone.top, zone.left + panelWidth, zone.top + panelHeight);
    }
}
```

- [ ] **Step 4: Run tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.sstpnk.wclock.brightness.BrightnessScheduleTest --tests com.sstpnk.wclock.render.BurnInControllerTest
```

Expected:

- `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit controllers**

Run:

```powershell
git add app/src/main/java/com/sstpnk/wclock/brightness app/src/main/java/com/sstpnk/wclock/render/BurnInController.java app/src/main/java/com/sstpnk/wclock/render/OverlayLayoutEngine.java app/src/test/java/com/sstpnk/wclock/brightness app/src/test/java/com/sstpnk/wclock/render
git commit -m "feat: add brightness and burn-in controllers"
```

---

### Task 8: Photo Scanner And Collage Layout

**Files:**

- Create: `app/src/main/java/com/sstpnk/wclock/collage/PhotoItem.java`
- Create: `app/src/main/java/com/sstpnk/wclock/collage/PhotoScanner.java`
- Create: `app/src/main/java/com/sstpnk/wclock/collage/CollageLayout.java`
- Create: `app/src/test/java/com/sstpnk/wclock/collage/PhotoScannerTest.java`

- [ ] **Step 1: Write photo scanner test**

Create `PhotoScannerTest.java`:

```java
package com.sstpnk.wclock.collage;

import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PhotoScannerTest {
    @Test
    public void acceptsCommonImageExtensions() {
        PhotoScanner scanner = new PhotoScanner();
        assertEquals(true, scanner.isSupportedImage(new File("a.jpg")));
        assertEquals(true, scanner.isSupportedImage(new File("a.jpeg")));
        assertEquals(true, scanner.isSupportedImage(new File("a.png")));
        assertEquals(true, scanner.isSupportedImage(new File("a.webp")));
        assertEquals(false, scanner.isSupportedImage(new File("a.txt")));
    }

    @Test
    public void missingFolderReturnsEmptyList() {
        PhotoScanner scanner = new PhotoScanner();
        List<PhotoItem> items = scanner.scan(new File("Z:/path/that/does/not/exist"));
        assertEquals(0, items.size());
    }
}
```

- [ ] **Step 2: Implement scanner and models**

Create `PhotoItem.java`:

```java
package com.sstpnk.wclock.collage;

import java.io.File;

public final class PhotoItem {
    public final File file;
    public final long modifiedAt;

    public PhotoItem(File file, long modifiedAt) {
        this.file = file;
        this.modifiedAt = modifiedAt;
    }
}
```

Create `PhotoScanner.java`:

```java
package com.sstpnk.wclock.collage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PhotoScanner {
    public List<PhotoItem> scan(File folder) {
        List<PhotoItem> result = new ArrayList<PhotoItem>();
        if (folder == null || !folder.isDirectory()) {
            return result;
        }
        File[] files = folder.listFiles();
        if (files == null) {
            return result;
        }
        List<File> sorted = Arrays.asList(files);
        Collections.sort(sorted);
        for (File file : sorted) {
            if (file.isFile() && isSupportedImage(file)) {
                result.add(new PhotoItem(file, file.lastModified()));
            }
        }
        return result;
    }

    public boolean isSupportedImage(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp");
    }
}
```

Create `CollageLayout.java`:

```java
package com.sstpnk.wclock.collage;

import android.graphics.RectF;

public final class CollageLayout {
    public RectF frameForIndex(int index, int width, int height) {
        float shortSide = Math.max(160.0f, Math.min(width, height) * 0.32f);
        float longSide = shortSide * 1.35f;
        float x = ((index * 173) % Math.max(1, width));
        float y = ((index * 97) % Math.max(1, height));
        x = Math.max(-shortSide * 0.25f, Math.min(width - shortSide * 0.75f, x - shortSide * 0.5f));
        y = Math.max(-longSide * 0.25f, Math.min(height - longSide * 0.75f, y - longSide * 0.5f));
        return new RectF(x, y, x + shortSide, y + longSide);
    }

    public float rotationForIndex(int index) {
        return ((index * 13) % 17) - 8.0f;
    }
}
```

- [ ] **Step 3: Run scanner tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest --tests com.sstpnk.wclock.collage.PhotoScannerTest
```

Expected:

- `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit scanner**

Run:

```powershell
git add app/src/main/java/com/sstpnk/wclock/collage app/src/test/java/com/sstpnk/wclock/collage
git commit -m "feat: add photo scanner and layout"
```

---

### Task 9: Bitmap Loader And Collage Engine

**Files:**

- Create: `app/src/main/java/com/sstpnk/wclock/collage/BitmapLoader.java`
- Create: `app/src/main/java/com/sstpnk/wclock/collage/CollageEngine.java`
- Modify: `app/src/main/java/com/sstpnk/wclock/render/ClockWeatherCollageView.java`

- [ ] **Step 1: Implement downsampling bitmap loader**

Create `BitmapLoader.java`:

```java
package com.sstpnk.wclock.collage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;

public final class BitmapLoader {
    public Bitmap decode(File file, int maxWidth, int maxHeight) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxWidth, maxHeight);
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    int sampleSize(int sourceWidth, int sourceHeight, int maxWidth, int maxHeight) {
        int sample = 1;
        while ((sourceWidth / sample) > maxWidth * 2 || (sourceHeight / sample) > maxHeight * 2) {
            sample *= 2;
        }
        return sample;
    }
}
```

- [ ] **Step 2: Implement collage engine draw loop**

Create `CollageEngine.java`:

```java
package com.sstpnk.wclock.collage;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class CollageEngine {
    private final PhotoScanner scanner = new PhotoScanner();
    private final BitmapLoader loader = new BitmapLoader();
    private final CollageLayout layout = new CollageLayout();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final List<PhotoItem> photos = new ArrayList<PhotoItem>();
    private final List<Bitmap> bitmaps = new ArrayList<Bitmap>();
    private String loadedPath = "";

    public void setFolder(String path, int maxVisible, int targetWidth, int targetHeight) {
        if (path == null || path.equals(loadedPath)) {
            return;
        }
        recycle();
        loadedPath = path;
        photos.addAll(scanner.scan(new File(path)));
        int count = Math.min(maxVisible, photos.size());
        for (int i = 0; i < count; i++) {
            Bitmap bitmap = loader.decode(photos.get(i).file, Math.max(320, targetWidth / 2), Math.max(320, targetHeight / 2));
            if (bitmap != null) {
                bitmaps.add(bitmap);
            }
        }
    }

    public void draw(Canvas canvas, long nowMillis) {
        canvas.drawColor(Color.rgb(12, 14, 16));
        if (bitmaps.size() == 0) {
            drawPlaceholder(canvas);
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        for (int i = 0; i < bitmaps.size(); i++) {
            Bitmap bitmap = bitmaps.get(i);
            RectF frame = layout.frameForIndex(i, width, height);
            float drift = (float) Math.sin((nowMillis / 120000.0) + i) * 12.0f;
            frame.offset(drift, -drift * 0.5f);
            canvas.save();
            canvas.rotate(layout.rotationForIndex(i), frame.centerX(), frame.centerY());
            paint.setColor(0x66000000);
            canvas.drawRect(frame.left + 8, frame.top + 8, frame.right + 8, frame.bottom + 8, paint);
            canvas.drawBitmap(bitmap, null, frame, paint);
            canvas.restore();
        }
    }

    public void recycle() {
        for (Bitmap bitmap : bitmaps) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        bitmaps.clear();
        photos.clear();
    }

    private void drawPlaceholder(Canvas canvas) {
        paint.setColor(Color.rgb(48, 72, 84));
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        for (int i = 0; i < 7; i++) {
            RectF frame = layout.frameForIndex(i, width, height);
            canvas.save();
            canvas.rotate(layout.rotationForIndex(i), frame.centerX(), frame.centerY());
            canvas.drawRect(frame, paint);
            canvas.restore();
        }
    }
}
```

- [ ] **Step 3: Wire collage engine into render view**

Modify `ClockWeatherCollageView`:

```java
private final com.sstpnk.wclock.collage.CollageEngine collageEngine = new com.sstpnk.wclock.collage.CollageEngine();
private String photoFolderPath = "";

public void setPhotoFolderPath(String path) {
    this.photoFolderPath = path == null ? "" : path;
}
```

At the top of `onDraw`, replace manual placeholder background with:

```java
collageEngine.setFolder(photoFolderPath, 8, getWidth(), getHeight());
collageEngine.draw(canvas, System.currentTimeMillis());
```

Add:

```java
@Override
protected void onDetachedFromWindow() {
    collageEngine.recycle();
    super.onDetachedFromWindow();
}
```

- [ ] **Step 4: Build**

Run:

```powershell
.\gradlew.bat assembleDebug
```

Expected:

- `BUILD SUCCESSFUL`.
- App still renders when no folder is configured.

- [ ] **Step 5: Commit collage engine**

Run:

```powershell
git add app/src/main/java/com/sstpnk/wclock/collage app/src/main/java/com/sstpnk/wclock/render/ClockWeatherCollageView.java
git commit -m "feat: render photo collage"
```

---

### Task 10: Integrate Weather, Settings, Brightness, And Burn-In Into Display

**Files:**

- Modify: `app/src/main/java/com/sstpnk/wclock/render/ClockWeatherCollageView.java`
- Modify: `app/src/main/java/com/sstpnk/wclock/render/RenderController.java`
- Create: `app/src/main/java/com/sstpnk/wclock/render/WeatherIconPainter.java`
- Modify: `app/src/main/java/com/sstpnk/wclock/host/MainActivity.java`
- Modify: `app/src/main/java/com/sstpnk/wclock/host/WClockDreamService.java`

- [ ] **Step 1: Add Canvas weather icons**

Create `WeatherIconPainter.java`:

```java
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
```

- [ ] **Step 2: Extend render view with weather and moving overlays**

Add fields to `ClockWeatherCollageView`:

```java
private final OverlayLayoutEngine overlayLayoutEngine = new OverlayLayoutEngine();
private final WeatherIconPainter weatherIconPainter = new WeatherIconPainter();
private com.sstpnk.wclock.weather.WeatherData weatherData;
private int burnInZoneIndex;

public void setWeatherData(com.sstpnk.wclock.weather.WeatherData weatherData) {
    this.weatherData = weatherData;
}

public void setBurnInZoneIndex(int burnInZoneIndex) {
    this.burnInZoneIndex = burnInZoneIndex;
}
```

Use `overlayLayoutEngine.primaryPanel(burnInZoneIndex, width, height)` for the clock panel instead of fixed coordinates. Draw weather from `weatherData` when present:

```java
String weatherLine = weatherData == null
        ? "Погода загружается"
        : weatherData.cityName + "  " + Math.round(weatherData.temperatureC) + "°C";
String description = weatherData == null ? "" : weatherData.descriptionRu + (weatherData.stale ? " · устарело" : "");
```

Draw the current weather icon near the weather text:

```java
if (weatherData != null) {
    weatherIconPainter.draw(canvas, weatherData.weatherCode, width * 0.62f, 92, 72);
}
```

- [ ] **Step 3: Extend render controller for settings and weather refresh**

Modify constructor:

```java
private final com.sstpnk.wclock.settings.SettingsRepository settingsRepository;
private final com.sstpnk.wclock.weather.WeatherRepository weatherRepository;
private long lastWeatherRefresh;
private int zoneIndex;

public RenderController(ClockWeatherCollageView view,
                        com.sstpnk.wclock.settings.SettingsRepository settingsRepository,
                        com.sstpnk.wclock.weather.WeatherRepository weatherRepository) {
    this.view = view;
    this.settingsRepository = settingsRepository;
    this.weatherRepository = weatherRepository;
}
```

Inside frame runnable before `invalidate()`:

```java
com.sstpnk.wclock.settings.SettingsRepository.Settings settings = settingsRepository.load();
view.setPhotoFolderPath(settings.photoFolderPath);
long now = System.currentTimeMillis();
if (now - lastWeatherRefresh > settings.weatherRefreshMinutes * 60L * 1000L) {
    lastWeatherRefresh = now;
    com.sstpnk.wclock.weather.WeatherData data = weatherRepository.refresh(settings.cityName, settings.latitude, settings.longitude, now);
    view.setWeatherData(data);
}
zoneIndex = (int) ((now / (settings.burnInMinMinutes * 60L * 1000L)) % 6);
view.setBurnInZoneIndex(zoneIndex);
```

- [ ] **Step 4: Update hosts to pass repositories**

In both hosts create:

```java
com.sstpnk.wclock.settings.SettingsRepository settingsRepository = new com.sstpnk.wclock.settings.SettingsRepository(this);
com.sstpnk.wclock.util.NetworkClient networkClient = new com.sstpnk.wclock.util.NetworkClient("WClock/0.1 contact: github.com/sstpnk/apk-wclock", 10000);
com.sstpnk.wclock.weather.WeatherRepository weatherRepository = new com.sstpnk.wclock.weather.WeatherRepository(
        networkClient,
        new com.sstpnk.wclock.weather.OpenMeteoProvider(),
        new com.sstpnk.wclock.weather.MetNorwayProvider());
renderController = new RenderController(view, settingsRepository, weatherRepository);
```

- [ ] **Step 5: Apply brightness in fullscreen host**

In `MainActivity.onResume()`:

```java
com.sstpnk.wclock.settings.SettingsRepository.Settings settings = new com.sstpnk.wclock.settings.SettingsRepository(this).load();
java.util.Calendar calendar = java.util.Calendar.getInstance();
com.sstpnk.wclock.brightness.BrightnessSchedule schedule = new com.sstpnk.wclock.brightness.BrightnessSchedule(
        7, 19, 23, settings.dayBrightness, settings.eveningBrightness, settings.nightBrightness);
new com.sstpnk.wclock.brightness.BrightnessController().apply(getWindow(), schedule.brightnessForHour(calendar.get(java.util.Calendar.HOUR_OF_DAY)));
```

- [ ] **Step 6: Build and run tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Expected:

- `BUILD SUCCESSFUL`.
- Fullscreen display shows configured photo folder when present.
- Weather does not block clock/collage when network fails.

- [ ] **Step 7: Commit display integration**

Run:

```powershell
git add app/src/main/java/com/sstpnk/wclock
git commit -m "feat: integrate display data"
```

---

### Task 11: Weather Diagnostics And Provider Hardening

**Files:**

- Modify: `app/src/main/java/com/sstpnk/wclock/weather/MetNorwayProvider.java`
- Modify: `app/src/main/java/com/sstpnk/wclock/settings/SettingsActivity.java`
- Modify: `app/src/main/java/com/sstpnk/wclock/weather/WeatherRepository.java`
- Create: `app/src/test/resources/met_norway_forecast.json`

- [ ] **Step 1: Implement MET Norway parser enough for fallback**

Use MET Norway compact response fields:

```java
JSONObject root = new JSONObject(body);
JSONArray timeseries = root.getJSONObject("properties").getJSONArray("timeseries");
JSONObject first = timeseries.getJSONObject(0);
JSONObject instant = first.getJSONObject("data").getJSONObject("instant").getJSONObject("details");
double temp = instant.getDouble("air_temperature");
double wind = instant.optDouble("wind_speed", 0.0);
```

Map `next_1_hours.summary.symbol_code` with a small local mapper:

```java
private static int codeFromSymbol(String symbol) {
    if (symbol == null) return 3;
    if (symbol.indexOf("clearsky") >= 0) return 0;
    if (symbol.indexOf("cloudy") >= 0) return 3;
    if (symbol.indexOf("rain") >= 0) return 63;
    if (symbol.indexOf("snow") >= 0) return 75;
    if (symbol.indexOf("thunder") >= 0) return 95;
    if (symbol.indexOf("fog") >= 0) return 45;
    return 2;
}
```

- [ ] **Step 2: Show diagnostics in settings**

Add read-only diagnostics text to `SettingsActivity`:

```java
TextView diagnostics = label("Погода: Open-Meteo основной, MET Norway резервный. Последняя ошибка показывается на главном экране при сбое.", 16);
root.addView(diagnostics);
```

This is intentionally simple for beta; live last-error wiring can be added after repository persistence is introduced.

- [ ] **Step 3: Build**

Run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Expected:

- `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit diagnostics**

Run:

```powershell
git add app/src/main/java/com/sstpnk/wclock/weather app/src/main/java/com/sstpnk/wclock/settings app/src/test/resources
git commit -m "feat: harden weather fallback"
```

---

### Task 12: TV, Long-Run, And Compatibility Pass

**Files:**

- Modify: `app/src/main/java/com/sstpnk/wclock/host/MainActivity.java`
- Modify: `app/src/main/java/com/sstpnk/wclock/settings/SettingsActivity.java`
- Modify: `app/src/main/java/com/sstpnk/wclock/render/RenderController.java`
- Create: `docs/BETA_CHECKLIST.md`

- [ ] **Step 1: Add TV-friendly manifest features**

In `AndroidManifest.xml`, add:

```xml
<uses-feature android:name="android.hardware.touchscreen" android:required="false" />
```

Keep launcher category as standard `LAUNCHER`; do not add Leanback dependencies.

- [ ] **Step 2: Ensure settings can be exited with Back/Menu**

In `SettingsActivity`, keep every `Button` focusable and avoid custom touch-only controls. Confirm no control requires swipe gestures.

- [ ] **Step 3: Add lifecycle cleanup**

In `RenderController.stop()` ensure all callbacks are removed:

```java
running = false;
handler.removeCallbacksAndMessages(null);
```

In `ClockWeatherCollageView.onDetachedFromWindow()` recycle collage bitmaps as in Task 9.

- [ ] **Step 4: Create beta checklist**

Create `docs/BETA_CHECKLIST.md`:

```markdown
# WClock Beta Checklist

- [ ] `.\gradlew.bat testDebugUnitTest` passes.
- [ ] `.\gradlew.bat assembleDebug` passes.
- [ ] Dependency report contains no `play-services`.
- [ ] Dependency report contains no runtime `androidx`.
- [ ] APK installs on Android 4.4 emulator or device.
- [ ] Fullscreen mode starts and keeps screen awake.
- [ ] DreamService is visible in Android screensaver settings.
- [ ] Settings are usable by D-pad only.
- [ ] Folder browser selects a local image folder.
- [ ] Empty folder does not crash the app.
- [ ] Corrupt image file does not crash the app.
- [ ] Open-Meteo weather appears for configured coordinates.
- [ ] MET Norway fallback is tested by forcing Open-Meteo failure.
- [ ] Offline mode keeps clock and collage visible.
- [ ] Brightness follows schedule in fullscreen mode.
- [ ] Overlay position changes within 5-15 minutes.
- [ ] 8-hour fullscreen run completes without crash.
- [ ] Known limitations are documented in README.
```

- [ ] **Step 5: Run full verification**

Run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath
```

Expected:

- Tests pass.
- Debug APK builds.
- No Play Services dependency.
- No runtime AndroidX dependency.

- [ ] **Step 6: Commit hardening**

Run:

```powershell
git add app/src/main docs/BETA_CHECKLIST.md
git commit -m "chore: harden beta compatibility"
```

---

### Task 13: Public Beta Packaging Docs

**Files:**

- Create or modify: `README.md`
- Modify: `docs/BETA_CHECKLIST.md`

- [ ] **Step 1: Write README**

Create `README.md`:

```markdown
# WClock

WClock is an Android 4.4 compatible fullscreen and screensaver clock with weather and a local photo collage background.

## Requirements

- Android 4.4 or newer.
- No Google Play Services required.
- Local photo folder on internal storage, SD, or USB storage exposed by the device.
- Internet access for weather.

## Weather

- Primary provider: Open-Meteo.
- Fallback provider: MET Norway.
- Location is configured manually with city name, latitude, and longitude.
- Yandex Weather is not used.

## Modes

- Fullscreen always-on mode from the launcher icon.
- Android screensaver mode through system Daydream/screensaver settings.

## Build

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

## Beta Limitations

- Brightness automation is schedule-based.
- Ambient light sensor support is planned after beta foundation.
- Folder picker is an in-app filesystem browser because many Android 4.4 TV devices do not provide a reliable system picker.
- Weather text descriptions are mapped locally in Russian.
```

- [ ] **Step 2: Produce beta APK**

Run:

```powershell
.\gradlew.bat assembleDebug
```

Expected artifact:

```text
app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 3: Final dependency check**

Run:

```powershell
.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath
```

Expected:

- No `com.google.android.gms`.
- No `play-services`.
- No runtime `androidx`.

- [ ] **Step 4: Commit beta docs**

Run:

```powershell
git add README.md docs/BETA_CHECKLIST.md
git commit -m "docs: add beta setup guide"
```

---

## Manual Test Matrix

Run before tagging public beta:

```text
Device/API              Mode          Required result
Android 4.4 emulator    Fullscreen    Launches, renders clock/collage/weather placeholder
Android 4.4 emulator    DreamService  Appears in screensaver settings and renders shared view
Tablet landscape        Fullscreen    Text fits, collage visible, brightness applies
Tablet portrait         Fullscreen    Text fits, collage visible, no clipping
TV/D-pad                Settings      All controls reachable and usable
Offline network         Fullscreen    Clock and collage continue, weather degrades gracefully
500-photo folder        Fullscreen    Startup remains responsive
8-hour run              Fullscreen    No crash, no unbounded memory growth
```

## Self-Review Notes

Spec coverage:

- Android 4.4/API 19: Task 1.
- No Google Play Services: Task 1, Task 12, Task 13 dependency checks.
- Fullscreen and DreamService modes: Task 3.
- Simultaneous clock/date/weather/forecast/collage: Tasks 3, 4, 9, 10.
- Weather icons: Task 10.
- Open-Meteo primary and MET Norway fallback: Tasks 4, 5, 11.
- Manual city/coordinates: Tasks 2, 6.
- In-app folder browser: Task 6.
- Custom collage engine: Tasks 8, 9.
- Burn-in prevention: Tasks 7, 10.
- Schedule brightness: Tasks 7, 10.
- TV/D-pad support: Tasks 6, 12.
- Public beta packaging: Task 13.

Known implementation risks to watch during execution:

- The Open-Meteo parser step must use `parseBody` for the static parser to avoid a Java duplicate signature.
- MET Norway parsing should stay conservative and can initially expose current weather plus derived forecast rows from timeseries.
- Android 4.4 TLS failures must be treated as provider errors, not app crashes.
- Collage defaults must stay conservative until a real device memory profile is available.
