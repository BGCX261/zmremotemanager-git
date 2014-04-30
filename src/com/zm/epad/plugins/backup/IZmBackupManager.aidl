package com.zm.epad.plugins.backup;

import android.app.backup.IFullBackupRestoreObserver;
import android.app.backup.IRestoreSession;
import android.os.ParcelFileDescriptor;
import android.content.Intent;
import com.zm.epad.plugins.backup.IZmObserver;

interface IZmBackupManager {
    boolean supportBackupOrRestore();
    boolean running();
    boolean backingUp();
    boolean restoring();
    void backup(IZmObserver observer);
    void restore(IZmObserver observer);
    void backupSpecial(IZmObserver observer);
    void restoreSpecial(IZmObserver observer);
    void cancelBackup();
    void cancelRestore();
    String getBackupPath();
}
