package com.zm.epad.core;

import android.app.Application;

import java.lang.Thread.UncaughtExceptionHandler;

public class RemoteManagerApplication extends Application {
    private static String TAG = "RemoteManagerApplication";

    private class RemoteManagerUncaughtExceptionHandler implements
            UncaughtExceptionHandler {
        private boolean mCrashing = false;

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            try {
                e.printStackTrace();
                if (mCrashing)
                    return;
                mCrashing = true;

                LogManager.local(TAG, "FATAL EXCEPTION: " + t.getName() + " "
                        + e);
            } catch (Throwable t2) {
                try {
                    LogManager.local(TAG, "Error reporting crash" + t2);
                    t2.printStackTrace();
                } catch (Throwable t3) {
                    // Even Slog.e() fails! Oh well.
                }
            } finally {
                LogManager.local(TAG, "before we quit, kill all resources");

            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler(new RemoteManagerUncaughtExceptionHandler());
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // do something to clean up
    }
}
