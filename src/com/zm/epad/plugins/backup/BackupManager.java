package com.zm.epad.plugins.backup;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import java.io.File;

class BackupManager extends IZmBackupManager.Stub {
    public final static String TAG = "ZMBackup";

    private final static String BACKUP_BASE_DIR = ".zmbackup";

    private final Context mContext;
    private final PackageManager mPackageManager;

    private final Handler mMainHandler;

    private final HandlerThread mHandlerThread;
    private final Handler mBackupOrRestoreHandler;
    private final String mBackupPath;

    private Notifier mBackupNotifier;
    private Notifier mRestoreNotifier;

    // Status
    private final int STATUS_NONE = 0;

    private final int STATUS_BACKUP_START = 1;
    private final int STATUS_BACKUP_GATHERING = 2;
    private final int STATUS_BACKUP_MANIFEST = 3;
    private final int STATUS_BACKUP_PREPARE = 4;
    private final int STATUS_BACKUP_ONE_PROGRESS = 5;
    private final int STATUS_BACKUP_END = 6;

    private final int STATUS_RESTORE_START = 7;
    private final int STATUS_RESTORE_MANIFEST = 8;
    private final int STATUS_RESTORE_PREPARE = 9;
    private final int STATUS_RESTORE_ONE_PROGRESS = 10;
    private final int STATUS_RESTORE_END = 11;
    private int mStatus;

    private RecordSet mRecordSet;

    public BackupManager(Context context) {
        mContext = context;
        mPackageManager = mContext.getPackageManager();
        mMainHandler = new Handler(context.getMainLooper());
        // backupOrRestore thread and handler
        mHandlerThread = new HandlerThread("backupOrRestore");
        mHandlerThread.start();
        mBackupOrRestoreHandler = new Handler(mHandlerThread.getLooper());
        // base path
        mBackupPath = Environment.getDownloadCacheDirectory().getPath()
            + File.separator + BACKUP_BASE_DIR;
    }

    @Override
    public boolean supportBackupOrRestore() {
        return Adapter.supportBackupOrRestore();
    }

    @Override
    public boolean backingUp() {
        return mStatus >= STATUS_BACKUP_START && mStatus <= STATUS_BACKUP_END;
    }

    @Override
    public boolean restoring() {
        return mStatus >= STATUS_RESTORE_START && mStatus <= STATUS_RESTORE_END;
    }

    @Override
    public boolean running() {
	return mStatus != STATUS_NONE;
    }

    @Override
    public void backup(IZmObserver observer) {
        mBackupNotifier = new Notifier(mMainHandler, observer);
        doBackup(STATUS_BACKUP_START);
    }

    @Override
    public void restore(IZmObserver observer) {
        mRestoreNotifier = new Notifier(mMainHandler, observer);
        doRestore(STATUS_RESTORE_START);
    }

    @Override
    public void cancelBackup() {

    }

    @Override
    public void cancelRestore() {

    }

    private void doBackup(int nextStatus) {
        mStatus = nextStatus;
        mBackupOrRestoreHandler.post(mBackupRunnable);
    }

    private void doRestore(int nextStatus) {
        mStatus = nextStatus;
        mBackupOrRestoreHandler.post(mRestoreRunnable);
    }

    private final Runnable mBackupRunnable = new Runnable() {
        @Override
        public void run() {
            switch (mStatus) {
                case STATUS_BACKUP_START:
			mBackupNotifier.notifyStart(mBackupPath);
			File file = new File(mBackupPath);
			Adapter.deleteFiles(file);
                    mRecordSet = new RecordSet(mBackupPath, mPackageManager,
                            mBackupNotifier);
                    doBackup(STATUS_BACKUP_GATHERING);
                    break;
                case STATUS_BACKUP_GATHERING:
                    mRecordSet.gathering();
                    doBackup(STATUS_BACKUP_MANIFEST);
                    break;
                case STATUS_BACKUP_MANIFEST:
                    mRecordSet.writeManifest();
                    doBackup(STATUS_BACKUP_PREPARE);
                    break;
                case STATUS_BACKUP_PREPARE:
                    mRecordSet.prepare();
                    doBackup(STATUS_BACKUP_ONE_PROGRESS);
                    break;
                case STATUS_BACKUP_ONE_PROGRESS:
                    doBackup(mRecordSet.backupOne() ?
                            STATUS_BACKUP_ONE_PROGRESS : STATUS_BACKUP_END);
                    break;
                case STATUS_BACKUP_END:
                    mStatus = STATUS_NONE;
                    mBackupNotifier.notifyEnd(mBackupPath,
				mRecordSet.getSystemAppCount(),
				mRecordSet.getInstalledAppCount(),
				mRecordSet.getFilesCount());
                    mBackupNotifier = null;
                    break;
                default:
			mStatus = STATUS_NONE;
			break;
            }
        }
    };

    private final Runnable mRestoreRunnable = new Runnable() {
        @Override
        public void run() {
            switch (mStatus) {
                case STATUS_RESTORE_START:
			mBackupNotifier.notifyStart(mBackupPath);
                    mRecordSet = new RecordSet(mBackupPath, mPackageManager,
                            mRestoreNotifier);
                    doRestore(STATUS_RESTORE_MANIFEST);
                    break;
                case STATUS_RESTORE_MANIFEST:
                    mRecordSet.readManifest();
                    doRestore(STATUS_RESTORE_PREPARE);
                    break;
                case STATUS_RESTORE_PREPARE:
                    mRecordSet.prepare();
                    doRestore(STATUS_RESTORE_ONE_PROGRESS);
                    break;
                case STATUS_RESTORE_ONE_PROGRESS:
			doRestore(mRecordSet.restoreOne() ?
				STATUS_RESTORE_ONE_PROGRESS : STATUS_RESTORE_END);
                    break;
                case STATUS_RESTORE_END:
                    mStatus = STATUS_NONE;
                    mBackupNotifier.notifyEnd(mBackupPath,
				mRecordSet.getSystemAppCount(),
				mRecordSet.getInstalledAppCount(),
				mRecordSet.getFilesCount());
                    mBackupNotifier = null;
                    break;
                default:
			mStatus = STATUS_NONE;
			break;
            }
        }
    };
}
