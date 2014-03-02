package com.zm.epad.plugins;

import com.zm.epad.core.LogManager;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

    public ArrayList<AppUsageStatistic> getAppUsageInfo() {
            try {
                mLock.lock();
                Collection<AppUsageStatistic> values = mAppUsage.values();
                ArrayList<AppUsageStatistic> ret = new ArrayList<AppUsageStatistic>();
                for(AppUsageStatistic one:values){
                    ret.add(one);
                }
                return ret;
            } finally {
                mLock.unlock();
            }
    }
    public UsageStatisticsManager(Context context) {
        mContext = context;
        mAm = ActivityManagerNative.getDefault();
        mAppUsage = new HashMap<String, AppUsageStatistic>();
        
    }
    public class AppUsageStatistic {
        public String mstrAppName; // Normally, it is package name
        public long mRunningTime; // app runing time. Unit is minute
        // public long mIdleTime; //no need to know this. app idle time. It
        // means
        private long mSampleBeginTime;
        private long mSampleEndTime;

        public AppUsageStatistic() {
            mSampleBeginTime = mSampleEndTime = SystemClock.elapsedRealtime();
            mRunningTime = 0;
        }

        /*
         * public void calculateIdleTime(){ long now =
         * SystemClock.elapsedRealtime(); mIdleTime += now - mSampleEndTime;
         * mSampleEndTime = now; }
         */
        public void syncTimeClock(long timeclock) {
            mSampleBeginTime = timeclock;
        }

        public void calculateRunningTime(long now) {
            mRunningTime += now - mSampleBeginTime;
            mSampleBeginTime = now;
        }

        public void reset() {
            mSampleBeginTime = mSampleEndTime = SystemClock.elapsedRealtime();
            mRunningTime = 0;
        }
    }

    private HashMap<String, AppUsageStatistic> mAppUsage;
    private AppUsageStatistic mRunningApp;

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
            mRunningApp = null;
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
                LogManager.local(TAG, "** Pkg resuming: " + pkg);
            }
            return true;
        }

        @Override
        public boolean activityStarting(Intent intent, String pkg) {
            LogManager.local(TAG, "** Pkg starting: " + pkg);
            try {
                mLock.lock();
                AppUsageStatistic sample = mAppUsage.get(pkg);
                if (sample == null) {
                    sample = new AppUsageStatistic();
                    sample.mstrAppName = pkg;
                    mAppUsage.put(pkg, sample);
                }
                long now = SystemClock.elapsedRealtime();
                Collection<AppUsageStatistic> values = mAppUsage.values();
                for (AppUsageStatistic one : values) {
                    if (one == mRunningApp) {
                        one.calculateRunningTime(now);
                    } else {

                        one.syncTimeClock(now);
                    }
                }

                mRunningApp = sample;
            } finally {
                mLock.unlock();
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
