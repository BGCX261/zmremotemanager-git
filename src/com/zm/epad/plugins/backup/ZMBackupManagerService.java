package com.zm.epad.plugins.backup;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * running in annother process, which named "com.zm.backup"
 * @author tkboy
 */
public class ZMBackupManagerService extends Service {
    private BackupManager mBackupManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mBackupManager = new BackupManager(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBackupManager;
    }
}
