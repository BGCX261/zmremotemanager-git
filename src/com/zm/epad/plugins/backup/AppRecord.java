package com.zm.epad.plugins.backup;

import android.app.backup.IBackupManager;
import android.app.backup.IFullBackupRestoreObserver;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageStats;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

class AppRecord extends Record {
    public final static String TAG = BackupManager.TAG;

    PackageInfo mPackageInfo;
    ApplicationInfo mAppInfo;
    String mPackageName;
    String[] mAllPackages;
    final boolean mInstalled;
    private final static IBackupManager mBackupManager =
            IBackupManager.Stub.asInterface(ServiceManager.getService("backup"));

    AppRecord(RecordSet recordSet, String[] packages) {
        this(recordSet, packages, true, false);
    }

    AppRecord(RecordSet recordSet, String[] packages, boolean installed) {
        this(recordSet, packages, installed, false);
    }

    AppRecord(RecordSet recordSet, String[] packages, boolean installed, boolean excludeFirst) {
        super(recordSet);
        if (excludeFirst) {
            mAllPackages = new String[packages.length - 1];
            for (int i = 1; i < packages.length; i++) {
                mAllPackages[i - 1] = packages[i];
            }
        } else {
            mAllPackages = packages;
        }
        mPackageName = packages[0];
        mInstalled = installed;
    }

    public AppRecord(RecordSet recordSet, boolean installed) {
        super(recordSet);
        mInstalled = installed;
    }

    @Override
    public String toString() {
        return "AppContent[" + mPackageName + " has " + mAllPackages.length + " packages]";
    }

    @Override
    void backup() {
        ParcelFileDescriptor fd = null;
        try {
            fd = ParcelFileDescriptor.open(new File(path()),
                    ParcelFileDescriptor.MODE_READ_WRITE |
                    ParcelFileDescriptor.MODE_CREATE |
                    ParcelFileDescriptor.MODE_TRUNCATE);
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
            return;
        }
        try {
            Adapter.fullBackupSilently(mBackupManager, fd, false, false,
                    false, false, true, mAllPackages,
                    "aaaa", "aaaa", mFullObserver);
        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        try {
            fd.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    void restore() {
        ParcelFileDescriptor fd = null;
        try {
            fd = ParcelFileDescriptor.open(new File(path()),
            ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
            return;
        }
        try {
            Adapter.fullRestoreSilently(mBackupManager, fd,
                    "aaaa", "aaaa", mFullObserver);
        } catch (RemoteException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        try {
            fd.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    String path() {
        return mRecordSet.getBasePath() + File.separatorChar + mPackageName;
    }

    @Override
    int type() {
        return mInstalled ? TYPE_INSTALLED : TYPE_SYSTEM;
    }

    private final IFullBackupRestoreObserver.Stub mFullObserver =
            new IFullBackupRestoreObserver.Stub() {

    String prompt() {
        return mDisplayName + " " + (mSize / 1024) + "Kb";
    }

        @Override
        public void onStartBackup() throws RemoteException {
            mRecordSet.mNotifier.notifyRecordStart(prompt());
        }

        @Override
        public void onBackupPackage(String name) throws RemoteException {
            Log.i(TAG, " ---- backup: " + name);
            mRecordSet.mNotifier.notifyRecordProgress(prompt());
        }

        @Override
        public void onEndBackup() throws RemoteException {
            mRecordSet.mNotifier.notifyRecordEnd(prompt());
        }

        @Override
        public void onStartRestore() throws RemoteException {
            mRecordSet.mNotifier.notifyRecordStart(prompt());
        }

        @Override
        public void onRestorePackage(String name) throws RemoteException {
            Log.i(TAG, " ---- restore: " + name);
            mRecordSet.mNotifier.notifyRecordProgress(prompt());
        }

        @Override
        public void onEndRestore() throws RemoteException {
            mRecordSet.mNotifier.notifyRecordEnd(prompt());
        }

        @Override
        public void onTimeout() throws RemoteException {
            Log.i(TAG, " ---- backup/restore timeout.");
            mRecordSet.mNotifier.notifyRecordTimeout(prompt());
        }
    };

    private class PackageSizeFetcher extends IPackageStatsObserver.Stub {
        AtomicInteger mCount = new AtomicInteger();
        int mSize;

        public synchronized void fetch() {
            for (String pkg : mAllPackages) {
                mRecordSet.mPackageManager.getPackageSizeInfo(pkg, this);
            }
            try {
                while (mCount.get() < mAllPackages.length) {
                    wait();
                }
            } catch (Exception e) {
            }
        }

        @Override
        public synchronized void onGetStatsCompleted(PackageStats stats, boolean succeeded) {
            if (!succeeded) {
                Log.w(TAG, "Failed to get package stats: " + stats.packageName);
            }

            mSize += stats.cacheSize;
            mSize += stats.dataSize;

            mCount.incrementAndGet();
            if (mCount.get() >= mAllPackages.length) {
                notify();
            }
        }
    };

    int getRoughSize() {
        PackageSizeFetcher fetcher = new PackageSizeFetcher();
        fetcher.fetch();
        return fetcher.mSize;
    }
}
