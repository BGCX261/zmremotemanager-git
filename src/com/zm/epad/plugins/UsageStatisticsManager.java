package com.zm.epad.plugins;

import com.zm.epad.core.LogManager;

import android.content.Context;
import android.content.Intent;

import java.util.concurrent.locks.ReentrantLock;
import android.app.IActivityController;
import android.app.IActivityManager;
import android.app.ActivityManagerNative;
public class UsageStatisticsManager {
    private static String TAG = "UsageStatisticsManager";

    private static UsageStatisticsManager sInstance = null;
    Context mContext;
    ActivityStatisticsCollector mActivityStatisticsCollector = null;

    private IActivityManager mAm;
    private ReentrantLock mLock = new ReentrantLock();

    public static UsageStatisticsManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new UsageStatisticsManager(context);
        }

        return sInstance;
    }

    public static UsageStatisticsManager getInstance() {
        LogManager.local(TAG, "getInstance:" + sInstance == null ? "null"
                : "OK");
        return sInstance;
    }

    public static void release() {
        LogManager.local(TAG, "release");
        sInstance = null;
    }

    private UsageStatisticsManager(Context context) {
        mContext = context;
        mAm = ActivityManagerNative.getDefault();
    }

    public boolean start() {
        try {
            mLock.lock();
            if (mActivityStatisticsCollector != null)
                return true;
            mActivityStatisticsCollector = new ActivityStatisticsCollector();
            mAm.setActivityController(mActivityStatisticsCollector);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            mLock.unlock();
        }

    }

    public void stop() {
        try {
            mLock.lock();
            mAm.setActivityController(null);
            mActivityStatisticsCollector = null;
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            mLock.unlock();
        }
    }

    private class ActivityStatisticsCollector extends IActivityController.Stub {
        public ActivityStatisticsCollector() {
        }

        @Override
        public boolean activityResuming(String pkg) {
            synchronized (this) {
                LogManager.local(TAG, "** Activity resuming: " + pkg);
            }
            return true;
        }

        @Override
        public boolean activityStarting(Intent intent, String pkg) {
            synchronized (this) {
                LogManager.local(TAG, "** Activity resuming: " + pkg);
            }
            return true;
        }

        @Override
        public boolean appCrashed(String processName, int pid, String shortMsg,
                String longMsg, long timeMillis, String stackTrace) {
            synchronized (this) {
                StringBuilder builder = new StringBuilder();
                builder.append("** ERROR: PROCESS CRASHED");
                builder.append("processName: " + processName);
                builder.append("processPid: " + pid);
                builder.append("shortMsg: " + shortMsg);
                builder.append("longMsg: " + longMsg);
                builder.append("timeMillis: " + timeMillis);
                builder.append("stack:");
                builder.append("#");
                LogManager.local(TAG, "** app crashed: " + builder.toString());
                return true;
            }
        }

        @Override
        public int appEarlyNotResponding(String processName, int pid,
                String annotation) {
            return 0;
        }

        @Override
        public int appNotResponding(String processName, int pid,
                String processStats) {
            return 0;
        }

        @Override
        public int systemNotResponding(String message) {
            synchronized (this) {
                StringBuilder builder = new StringBuilder();
                builder.append("** ERROR: PROCESS NOT RESPONDING");
                builder.append("message: " + message);
                builder.append("#");
                builder.append("Allowing system to die.");
                return -1;
            }
        }
    }
}
