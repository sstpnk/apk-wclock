package com.sstpnk.wclock.util;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class CrashReporter {
    public static final String FILE_NAME = "wclock-crash.log";
    private static boolean installed;

    private CrashReporter() {
    }

    public static synchronized void install(Context context) {
        if (installed || context == null) {
            return;
        }
        installed = true;
        final Context appContext = context.getApplicationContext();
        final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                write(appContext, thread, throwable);
                if (previous != null) {
                    previous.uncaughtException(thread, throwable);
                }
            }
        });
    }

    static void write(Context context, Thread thread, Throwable throwable) {
        PrintWriter writer = null;
        try {
            File file = latestCrashLog(context);
            writer = new PrintWriter(new FileWriter(file, false));
            writer.println("WClock crash");
            writer.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(new Date()));
            writer.println("Thread: " + (thread == null ? "unknown" : thread.getName()));
            if (throwable != null) {
                throwable.printStackTrace(writer);
            }
        } catch (Exception ignored) {
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    public static File latestCrashLog(Context context) {
        File external = context.getExternalFilesDir(null);
        if (external != null) {
            return new File(external, FILE_NAME);
        }
        return new File(context.getFilesDir(), FILE_NAME);
    }
}
