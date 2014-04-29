package com.zm.epad.plugins.backup;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

class RecordSet {
    public final static String TAG = BackupManager.TAG;

    final PackageManager mPackageManager;
    final Notifier mNotifier;
    private final Manifest mManifest;
    private final String mBasePath;
    private final ArrayList<Record> mRecords = new ArrayList<Record>();
    private ArrayList<Record> mTempRecords;
    private int mCurOrder;

    RecordSet(String path, PackageManager packageManager, Notifier notifier) {
        mBasePath = path;
        mPackageManager = packageManager;
        mNotifier = notifier;
        mManifest = new Manifest(mBasePath);
    }

    String getBasePath() {
        return mBasePath;
    }

    String getManifestPath() {
        return mManifest.path();
    }

    /**
     * Gathering all backup records.
     */
    void gathering() {
        mCurOrder = 0;
        mRecords.clear();
        gatheringSystemRecords();
        gatheringInstalledRecords();
    }

    /**
     * write all backup records to manifest.
     */
    void writeManifest() {
        mManifest.prepareBackup();
        for (Record r : mRecords) {
            mManifest.addElementRecord(r);
        }
        mManifest.writeToFile();
        mManifest.close();
    }

    void readManifest() {
        mManifest.readFromFile();
        mManifest.readRoot();
        mRecords.clear();
        mManifest.readRecords(this);
        mManifest.close();
    }

    /**
     * prepare.
     */
    @SuppressWarnings("unchecked")
    void prepare() {
        mTempRecords = (ArrayList<Record>) mRecords.clone();
    }

    /**
     * returning true means still has next one to backup.
     */
    boolean backupOne() {
        if (mTempRecords.size() <= 0) {
            return false;
        }
        Record item = mTempRecords.remove(0);
        item.backup();
        return true;
    }

    boolean restoreOne() {
        if (mTempRecords.size() <= 0) {
            return false;
        }
        Record item = mTempRecords.remove(0);
        item.restore();
        return true;
    }

    private void gatheringSystemRecords() {
        AppRecord contacts = new AppRecord(this,
                new String[] {"com.android.contacts",
                          "com.android.contacts.common"}, false);
        addSystemPackageInfo(contacts);

        AppRecord conversation = new AppRecord(this,
                new String[] {"com.android.phone",
                          "com.android.providers.telephony"}, false);
        addSystemPackageInfo(conversation);

        AppRecord chrome = new AppRecord(this,
                new String[] {"com.android.chrome"}, false);
        addSystemPackageInfo(chrome);

        AppRecord browser = new AppRecord(this,
                new String[] {"com.android.browser"}, false);
        addSystemPackageInfo(browser);

        AppRecord alarm = new AppRecord(this,
                new String[] {"com.android.deskclock"}, false);
        addSystemPackageInfo(alarm);

        AppRecord calendar = new AppRecord(this,
                new String[] {"com.android.calendar",
                              "com.android.providers.calendar"}, false);
        addSystemPackageInfo(calendar);

        AppRecord settings = new AppRecord(this,
                new String[] {"com.android.settings",
                              "com.android.providers.settings",
                              "android"}, false, true);
        addSystemPackageInfo(settings);

        AppRecord email = new AppRecord(this,
                new String[] {"com.android.email"}, false);
        addSystemPackageInfo(email);

        AppRecord zm = new AppRecord(this,
                new String[] {"com.zm.epad"}, false);
        addSystemPackageInfo(zm);
    }

    private void addSystemPackageInfo(AppRecord pi) {
        try {
            pi.mAppInfo = mPackageManager.getApplicationInfo(pi.mPackageName, 0);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Package " + pi.mPackageName + " not found in system.");
            return;
        }
        if ((pi.mAppInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
            return;
        }
        pi.mDisplayName = (String) mPackageManager.getApplicationLabel(pi.mAppInfo);
        pi.mSize = pi.getRoughSize();
        pi.mOrder = mCurOrder++;
        mRecords.add(pi);
    }

    private void gatheringInstalledRecords() {
        List<ApplicationInfo> list3rd = mPackageManager.getInstalledApplications(0);
        for (ApplicationInfo appInfo : list3rd) {
            if ((appInfo.flags & (ApplicationInfo.FLAG_ALLOW_BACKUP |
                    ApplicationInfo.FLAG_SYSTEM)) == ApplicationInfo.FLAG_ALLOW_BACKUP) {
                AppRecord appItem = new AppRecord(this,
                        new String[] { appInfo.packageName });
                appItem.mAppInfo = appInfo;
                appItem.mDisplayName = (String) mPackageManager.getApplicationLabel(appInfo);
                appItem.mSize = appItem.getRoughSize();
                appItem.mOrder = mCurOrder++;
                mRecords.add(appItem);
            }
        }
    }

    int getSystemAppCount() {
        int count = 0;
        for (Record r : mRecords) {
            if (r instanceof AppRecord && !((AppRecord)r).mInstalled) {
                count++;
            }
        }
        return count;
    }

    int getInstalledAppCount() {
        int count = 0;
        for (Record r : mRecords) {
            if (r instanceof AppRecord && ((AppRecord)r).mInstalled) {
                count++;
            }
        }
        return count;
    }

    int getFilesCount() {
        int count = 0;
        for (Record r : mRecords) {
            if (r instanceof FileRecord) {
                count += ((FileRecord)r).count();
            }
        }
        return count;
    }

    void addRecord(Record r) {
        mRecords.add(r);
    }
}
