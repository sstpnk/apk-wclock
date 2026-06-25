package com.sstpnk.wclock.util;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class CrashReporterTest {
    @Test
    public void writeCreatesReadableCrashLogPath() {
        Context context = ApplicationProvider.getApplicationContext();

        CrashReporter.write(context, Thread.currentThread(), new RuntimeException("boom"));

        File log = CrashReporter.latestCrashLog(context);
        assertTrue(log.getAbsolutePath(), log.exists());
        assertTrue(log.getAbsolutePath().endsWith(CrashReporter.FILE_NAME));
    }
}
