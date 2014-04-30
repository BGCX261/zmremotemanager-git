package com.zm.epad.plugins;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.zm.epad.plugins.backup.IZmBackupManager;
import com.zm.epad.plugins.backup.IZmObserver;
import com.zm.epad.plugins.backup.ZMBackupManagerService;

import java.util.concurrent.ExecutorService;

/**
 * BackupManager API for zm main process.
 * It connects to ZMBackupManagerService which running in process "com.zm.pad".
 */
public class BackupManager {

    private IZmBackupManager mBackupManager;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBackupManager = IZmBackupManager.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBackupManager = null;
        }
    };

    public BackupManager(Context context) {
        Intent intent = new Intent(context, ZMBackupManagerService.class);
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        context.startService(intent);
    }

    public boolean supportBackupOrRestore() {
        try {
            return mBackupManager.supportBackupOrRestore();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean running() {
        try {
            return mBackupManager.running();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean backingUp() {
        try {
            return mBackupManager.backingUp();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean restoring() {
        try {
            return mBackupManager.restoring();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void backup(IZmObserver observer) {
        try {
            mBackupManager.backup(observer);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void cancelBackup() {
        try {
            mBackupManager.cancelBackup();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void restore(IZmObserver observer) {
        try {
            mBackupManager.restore(observer);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void cancelRestore() {
        try {
            mBackupManager.cancelRestore();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void backupSpecial(IZmObserver observer) {
        try {
            mBackupManager.backupSpecial(observer);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void restoreSpecial(IZmObserver observer) {
        try {
            mBackupManager.restoreSpecial(observer);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
