package com.zm.epad.plugins.backup;

import android.app.backup.IBackupManager;
import android.app.backup.IFullBackupRestoreObserver;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class Adapter {
    private static Method sFullBackupSilently;
    private static Method sFullRestoreSilently;

    static {
        try {
            sFullBackupSilently = IBackupManager.class.getMethod("fullBackupSilently",
                    ParcelFileDescriptor.class, boolean.class, boolean.class,
                    boolean.class, boolean.class, boolean.class,
                    String[].class, String.class, String.class,
                    IFullBackupRestoreObserver.class);
            sFullRestoreSilently = IBackupManager.class.getMethod("fullRestoreSilently",
                    ParcelFileDescriptor.class, String.class, String.class,
                    IFullBackupRestoreObserver.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    static void fullBackupSilently(IBackupManager backupManager,
            ParcelFileDescriptor fd, boolean includeApks, boolean includeObbs,
            boolean includeShared, boolean allApps, boolean allIncludesSystem,
            String[] packageNames, String curPassword, String encPassword,
            IFullBackupRestoreObserver observer) throws RemoteException {
        try {
            sFullBackupSilently.invoke(backupManager,
                    fd, includeApks, includeObbs,
                    includeShared, allApps, allIncludesSystem,
                    packageNames, curPassword, encPassword,
                    observer);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    static void fullRestoreSilently(IBackupManager backupManager,
            ParcelFileDescriptor fd,
            String curPassword, String encPassword,
            IFullBackupRestoreObserver observer)
                    throws RemoteException {
        try {
            sFullRestoreSilently.invoke(backupManager, fd, curPassword,
			encPassword, observer);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    static boolean supportBackupOrRestore() {
        return !(sFullBackupSilently == null || sFullRestoreSilently == null);
    }

    static void deleteFiles(File file) {
	if (file.exists()) {
		if (file.isDirectory()) {
			for (String f : file.list()) {
				File sub = new File(file.getPath() + File.separator + f);
				deleteFiles(sub);
			}
		}
		file.delete();
	}
    }
}
