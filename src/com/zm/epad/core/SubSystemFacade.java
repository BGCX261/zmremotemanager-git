package com.zm.epad.core;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.UserManager;

import com.android.internal.os.PkgUsageStats;
import com.zm.epad.plugins.BackupManager;
import com.zm.epad.plugins.RemoteAlarmManager;
import com.zm.epad.plugins.RemoteAlarmManager.AlarmCallback;
import com.zm.epad.plugins.RemoteDesktopManager;
import com.zm.epad.plugins.RemoteDeviceManager;
import com.zm.epad.plugins.RemoteDeviceManager.LocationReportCallback;
import com.zm.epad.plugins.RemoteDeviceManager.RemoteLocation;
import com.zm.epad.plugins.RemoteFileManager;
import com.zm.epad.plugins.RemoteFileManager.FileTransferCallback;
import com.zm.epad.plugins.RemotePackageManager;
import com.zm.epad.plugins.RemotePackageManager.installCallback;
import com.zm.epad.plugins.RemoteStatsManager;
import com.zm.epad.plugins.RemoteWebManager;
import com.zm.epad.plugins.SmartShareManager;
import com.zm.epad.plugins.backup.IZmObserver;
import com.zm.epad.plugins.policy.RemotePolicyManager;
import com.zm.epad.structure.Application;
import com.zm.epad.structure.Configuration;
import com.zm.epad.structure.Device;
import com.zm.xmpp.communication.result.ResultRunningApp;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SubSystemFacade {

    private static final String TAG = "SubSystemFacade";

    private RemotePackageManager mPackageManager;
    private RemoteDeviceManager mDeviceManager;
    private RemoteFileManager mFileManager;
    private RemotePolicyManager mPolicyManager;
    private RemoteStatsManager mStatsManager;
    private RemoteAlarmManager mAlarmManager;
    private SmartShareManager mSmartShare;
    private RemoteWebManager mWebManager;
    private BackupManager mBackupManager;

    private Context mContext;
    private static SubSystemFacade gSubSystemFacade = null;
    @SuppressWarnings("serial")
    private List<NotifyListener> mListeners = new ArrayList<NotifyListener>() {
    };

    public static final int NOTIFY_APP_USAGE = 1;
    public static final int NOTIFY_POSITION = 2;

    // TODO: need to figure out whether NotifyListener is suitable.
    // Because it will broadcast all notification to all listeners.
    // Some listener should not receive all message at all.
    // And, it cannot carry more than 2 data directly.
    // I try to use Message/Looper/Handler for instead.
    public static final int NOTIFY_DESKTOP_SHARE = 3;

    public SubSystemFacade(Context context) {
        mContext = context;
    }

    public static SubSystemFacade getInstance() {
        return gSubSystemFacade;
    }

    public interface NotifyListener {
        void notify(int type, Object obj);
    }

    public void setListener(NotifyListener listener) {
        mListeners.add(listener);
    }

    public void cancelListener(NotifyListener listener) {
        mListeners.remove(listener);
    }

    public void sendNotify(int type, Object obj) {
        for (NotifyListener nl : mListeners) {
            nl.notify(type, obj);
        }
    }

    /**
     * Only used for normal Runnable threads, which not use Looper.prepare()
     * Because if a Looper applied to a thread, the thread can not re-used.
     */
    private ExecutorService mCachedThreadPool;

    /**
     * Used for message threads, which use Loop.prepare().
     */
    private ExecutorService mNonCachedThreadPool;

    public void start(Bundle loginBundle) {
        if (gSubSystemFacade == null)
            gSubSystemFacade = this;

        mCachedThreadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 180L,
                TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

        mNonCachedThreadPool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 0L,
                TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());

        startFirstPriority(loginBundle);

        mPackageManager = new RemotePackageManager(mContext);

        mDeviceManager = new RemoteDeviceManager(mContext);

        mSmartShare = new SmartShareManager(mContext);
        mSmartShare.setThreadPool(mNonCachedThreadPool);

        mPolicyManager = new RemotePolicyManager(mContext);
        mPolicyManager.loadPolicy();

        mStatsManager = new RemoteStatsManager(mContext);
        mStatsManager.start();

        mWebManager = new RemoteWebManager(mContext);

        mBackupManager = new BackupManager(mContext);
    }

    public RemotePackageManager getRemotePackageManager() {
        return mPackageManager;
    }

    public RemoteDeviceManager getRemoteDeviceManager() {
        return mDeviceManager;
    }

    public RemoteFileManager getRemoteFileManager() {
        return mFileManager;
    }

    public RemotePolicyManager getRemotePolicyManager() {
        return mPolicyManager;
    }

    public RemoteStatsManager getRemoteStatsManager() {
        return mStatsManager;
    }

    public boolean addTaskToSubSystemThreadPool(Runnable task) {
        if (mCachedThreadPool == null)
            return false;
        mCachedThreadPool.execute(task);
        return true;
    }

    public void stop() {
        mListeners.clear();

        mStatsManager.stop();
        mStatsManager = null;

        mPackageManager.stop();
        mPackageManager = null;

        mDeviceManager.stop();
        mDeviceManager = null;

        mPolicyManager.stop();
        mPolicyManager = null;

        stopFirstPriority();

        shutdownAndAwaitTermination();

        mCachedThreadPool = null;
        mNonCachedThreadPool = null;

        gSubSystemFacade = null;
    }

    private void startFirstPriority(Bundle loginBundle) {
        mAlarmManager = new RemoteAlarmManager(mContext);

        mFileManager = new RemoteFileManager(mContext);
        mFileManager.setXmppLoginResource(loginBundle);
        mFileManager.setThreadPool(mCachedThreadPool);
    }

    private void stopFirstPriority() {
        mFileManager.stop();
        mFileManager = null;

        mAlarmManager.stop();
        mAlarmManager = null;
    }

    void shutdownAndAwaitTermination() {
        mCachedThreadPool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!mCachedThreadPool.awaitTermination(60, TimeUnit.SECONDS))
                mCachedThreadPool.shutdownNow(); // Cancel currently executing tasks
            // Wait a while for tasks to respond to being cancelled
            if (!mCachedThreadPool.awaitTermination(60, TimeUnit.SECONDS))
                LogManager.local(TAG, "Pool did not terminate");

        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            mCachedThreadPool.shutdownNow();
            // Preserve interrupt status
            // Thread.currentThread().interrupt();
        }
        mNonCachedThreadPool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!mNonCachedThreadPool.awaitTermination(60, TimeUnit.SECONDS))
                mNonCachedThreadPool.shutdownNow(); // Cancel currently executing tasks
            // Wait a while for tasks to respond to being cancelled
            if (!mNonCachedThreadPool.awaitTermination(60, TimeUnit.SECONDS))
                LogManager.local(TAG, "Pool did not terminate");

        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            mNonCachedThreadPool.shutdownNow();
            // Preserve interrupt status
            // Thread.currentThread().interrupt();
        }
    }

    /*
     * 
     * Wrapper around RemotePackageManager
     */
    public boolean enablePkgForUser(String pkgName, int userId) {
        return mPackageManager.enablePkgForUser(pkgName, userId);
    }

    public boolean disablePkgForUser(String pkgName, int userId) {
        return mPackageManager.disablePkgForUser(pkgName, userId);
    }

    public boolean isNewPackage(String packageName, String version) {
        return mPackageManager.isNewPackage(packageName, version);
    }

    public int installPkgForUser(String url, int userId, installCallback cb) {
        return mPackageManager.installPkgForUser(url, userId, true, cb);
    }

    public int installPkgForUser(String url, int userId, installCallback cb,
            boolean update) {
        return mPackageManager.installPkgForUser(url, userId, update, cb);
    }

    public boolean InstallExsitedPackage(String packageName, int userId) {
        return mPackageManager.InstallExsitedPackage(packageName, userId);
    }

    public boolean uninstallPkgForUser(String name, int userId) {
        return mPackageManager.uninstallPkgForUser(name, userId);
    }

    public void setGuestEnabled(boolean enable) {
        mPackageManager.setGuestEnabled(enable);
    }

    public Configuration getZMUserConfigInfo(int uid) {
        Bundle userRestrictionInfo = mPackageManager.getUserRestrictions(uid);

        if (userRestrictionInfo == null) {
            return null;
        }

        Configuration cfg = new Configuration();
        cfg.setNoModifyAccount(String.valueOf(userRestrictionInfo
                .getBoolean(UserManager.DISALLOW_MODIFY_ACCOUNTS)));
        cfg.setNoConfigWifi(String.valueOf(userRestrictionInfo
                .getBoolean(UserManager.DISALLOW_CONFIG_WIFI)));
        cfg.setNoInstallApps(String.valueOf(userRestrictionInfo
                .getBoolean(UserManager.DISALLOW_INSTALL_APPS)));
        cfg.setNoInstallApps(String.valueOf(userRestrictionInfo
                .getBoolean(UserManager.DISALLOW_UNINSTALL_APPS)));
        cfg.setNoShareLocation(String.valueOf(userRestrictionInfo
                .getBoolean(UserManager.DISALLOW_SHARE_LOCATION)));
        cfg.setNoInstallUnknownSources(String.valueOf(userRestrictionInfo
                .getBoolean(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)));
        cfg.setNoConfigBluetooth(String.valueOf(userRestrictionInfo
                .getBoolean(UserManager.DISALLOW_CONFIG_BLUETOOTH)));
        cfg.setNoUsbFileTranster(String.valueOf(userRestrictionInfo
                .getBoolean(UserManager.DISALLOW_USB_FILE_TRANSFER)));
        cfg.setNoConfigCredentials(String.valueOf(userRestrictionInfo
                .getBoolean(UserManager.DISALLOW_CONFIG_CREDENTIALS)));
        cfg.setNoRemoveUser(String.valueOf(userRestrictionInfo
                .getBoolean(UserManager.DISALLOW_REMOVE_USER)));

        return cfg;
    }

    public List<UserInfo> getAllUsers() {
        return mPackageManager.getAllUsers();
    }

    public List<PackageInfo> getInstalledPackages(int flags, int userid) {
        return mPackageManager.getInstalledPackages(0, userid);
    }

    public PackageInfo getPackageInfo(String pkgName, int flags, int userId) {
        return mPackageManager.getPackageInfo(pkgName, flags, userId);
    }

    public Application getZMApplicationInfo(PackageInfo pi) {
        String name = mPackageManager.getApplicationName(pi);
        String pkgname = pi.packageName;
        String enabled = String.valueOf(pi.applicationInfo.enabled);
        String flag = String.valueOf(pi.applicationInfo.flags);
        String version = pi.versionName;

        Application zmAppInfo = new Application();
        zmAppInfo.setName(name);
        zmAppInfo.setAppName(pkgname);
        zmAppInfo.setEnabled(enabled);
        zmAppInfo.setFlag(flag);
        zmAppInfo.setVersion(version);

        return zmAppInfo;
    }

    public List<RunningAppProcessInfo> getRunningAppProcesses() {
        return mPackageManager.getRunningAppProcesses();
    }

    static public final int INDEX_NAME = 0;
    static public final int INDEX_PKGNAME = 1;
    static public final int INDEX_DISPLAY = 2;
    static public final int INDEX_IMPORTANCE = 3;
    static public final int INDEX_VERSION = 4;
    static public final int INDEX_MAX = INDEX_VERSION + 1;

    public void getRunningAppProcessInfo(String[] outArray,
            RunningAppProcessInfo pi) {
        outArray[INDEX_NAME] = pi.processName;
        outArray[INDEX_IMPORTANCE] = getAppProcessImportance(pi.importance);

        if (pi.importanceReasonComponent != null) {
            outArray[INDEX_PKGNAME] = pi.importanceReasonComponent
                    .getPackageName();
        } else {
            // if no importance reason, show the first loaded pkg
            outArray[INDEX_PKGNAME] = pi.pkgList[0];
        }
        int userId = mPackageManager.getCurrentUserId();

        outArray[INDEX_DISPLAY] = mPackageManager.getApplicationName(
                outArray[INDEX_PKGNAME], 0, userId);
        outArray[INDEX_VERSION] = mPackageManager.getApplicationVersion(
                outArray[INDEX_PKGNAME], 0, userId);

        return;

    }

    private String getAppProcessImportance(int importance) {
        String ret = ResultRunningApp.PROCESS_UNKNOWN;

        switch (importance) {
        case RunningAppProcessInfo.IMPORTANCE_FOREGROUND:
            ret = ResultRunningApp.PROCESS_FOREGROUND;
            break;
        case RunningAppProcessInfo.IMPORTANCE_VISIBLE:
            ret = ResultRunningApp.PROCESS_VISIBLE;
            break;
        case RunningAppProcessInfo.IMPORTANCE_SERVICE:
            ret = ResultRunningApp.PROCESS_SERVICE;
            break;
        case RunningAppProcessInfo.IMPORTANCE_BACKGROUND:
            ret = ResultRunningApp.PROCESS_BACKGROUND;
            break;
        case RunningAppProcessInfo.IMPORTANCE_EMPTY:
            ret = ResultRunningApp.PROCESS_EMPTY;
            break;
        default:
            break;
        }

        return ret;
    }

    public int getCurrentUserId() {
        return mPackageManager.getCurrentUserId();
    }

    public boolean isGuestEnabled() {
        return mPackageManager.isGuestEnabled();
    }

    public void startMonitorRunningAppInfo(long interval,
            RemotePackageManager.ReportRunningAppInfo callback) {
        mPackageManager.startMonitorRunningApp(interval, callback);
    }

    public void stopMonitorRunningAppInfo() {
        mPackageManager.stopMonitorRunningApp();
    }

    public List<ComponentName> getPackageComponent(String action, String pkgName) {
        return mPackageManager.getPackageComponent(action, pkgName);
    }

    private class ThreadRunnable implements Runnable {
        private Looper retLooper = null;

        public void run() {
            Looper.prepare();
            retLooper = Looper.myLooper();
            synchronized (this) {
                notifyAll();
            }
            Looper.loop();
        }

        public synchronized Looper getLooper() {
            if (retLooper == null) {
                try {
                    wait();
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
            return retLooper;
        }
    }

    public Looper getAThreadLooper() {
        ThreadRunnable looperHelper = new ThreadRunnable();
        mNonCachedThreadPool.execute(looperHelper);
        return looperHelper.getLooper();
    }

    /**
     * Wrapper around RemoteDeviceManager
     */
    public Device getZMDeviceInfo() {
        Device device = new Device();

        device.setWifi(mDeviceManager.getWifiName());
        device.setBt(mDeviceManager.getBlueToothStatus());
        device.setNfc(mDeviceManager.getNfcStatus());
        device.setIp(mDeviceManager.getIpAddress());
        device.setGps(mDeviceManager.getGpsStatus());
        device.setAmode(mDeviceManager.getAirplaneMode());
        device.setMnet(mDeviceManager.getMobileNetwork());

        LogManager.local(TAG, device.toString());
        return device;
    }

    public boolean changeWallpaper(String wallpaperFile) {
        return mDeviceManager.changeWallpaper(wallpaperFile);
    }

    public void lockScreen() {
        mDeviceManager.lockScreen();
    }

    public boolean startTrackLocation(int mode, long minTime, int minDistance,
            LocationReportCallback callback) {
        return mDeviceManager.startTrackLocation(mode, minTime, minDistance,
                callback);
    }

    public void stopTrackLocation() {
        mDeviceManager.stopTrackLocation();
    }

    public RemoteLocation[] getHistoryLocations() {
        return mDeviceManager.getHistoryLocations();
    }

    /*
     * Wrapper around RemoteAlarmManager
     */
    public boolean isScreenOn() {
        return mDeviceManager.isScreenOn();
    }

    /**
     * Wrapper around RemoteDeviceManager Used to make screen off and block any
     * key event
     * 
     * @param disable
     *            : if true, disable screen; if false, enable screen.
     */
    public void disableScreen(boolean disable) {
        mDeviceManager.toggleScreen(!disable);
    }

    /*
     * Wrapper around RemoteFileManager
     */
    public void downloadFile(String url, FileTransferCallback callback) {
        mFileManager.addFileDownloadTask(url, callback);
    }
    public void uploadFile(String url,String filePath,String fileName,Bundle info,
            FileTransferCallback callback){
        mFileManager.addFileUploadTask(url, filePath, fileName, info, callback);
    }
    public void zipAndUploadFile(String url,String srcPath,String zipPath,Bundle info,
            FileTransferCallback callback){
        mFileManager.zipAndUploadFile(url, srcPath, zipPath, info, callback);
    }
    public void uploadScreenshot(String url, Bundle info,
            FileTransferCallback callback) {
        mFileManager.addScreenshotTask(url, info, callback);
    }

    /*
     * Wrapper around RemotePolicyManager
     */
    public void updatePolicy(String policy) throws Exception {
        mPolicyManager.updatePolicy(policy);
    }

    /*
     * Wrapper around RemoteStatsManager
     */
    public PkgUsageStats[] getAllPkgUsageStats() {
        return mStatsManager.getAllPkgUsageStats();
    }

    /*
     * Wrapper around RemoteAlarmManager
     */
    public int setAlarm(long triggerAtMillis, AlarmCallback callback)
            throws Exception {
        return mAlarmManager.setAlarm(triggerAtMillis, callback);
    }

    /*
     * Wrapper around RemoteAlarmManager
     */
    public void cancelAlarm(int alarmId) {
        mAlarmManager.cancelAlarm(alarmId);
    }

    public boolean supportDesktopShare() {
        return RemoteDesktopManager.support();
    }

    /**
     * Following msg set to arg1 of desktop's notify.
     */
    public final static int MSG_DESKTOP_SERVER_CREATED_OK = 1; // return url
                                                              // in the object
                                                              // param of notify
    public final static int MSG_DESKTOP_RUNNING_OK = 2;
    public final static int MSG_DESKTOP_STOPPED = 3;
    public final static int MSG_DESKTOP_NOT_SUPPORT = 4;
    public final static int MSG_DESKTOP_IN_USE = 5;
    public final static int MSG_DESKTOP_NO_NETWORK = 6;
    public final static int MSG_DESKTOP_SERVER_CREATE_FAILED = 7;
    public final static int MSG_DESKTOP_DISPLAY_CREATE_FAILED = 8;
    /**
     * Start Remote Desktop
     * @param notify what=NOTIFY_DESKTOP_SHARE, I don't know if what is needed.
     */
    public void startDesktopShare(Message notify) {
        mSmartShare.startDesktopShare(notify);
    }

    public void stopDesktopShare() {
        mSmartShare.stopDesktopShare();
    }

    public TimeZone getDefaultTimeZone() {
        return TimeZone.getTimeZone(CoreConstants.CONSTANT_TIMEZOME_DEFAULT);
    }

    public void acquireWakeLock(String tag) {
        // Allow screen to go off as default
        mDeviceManager.acquireWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
    }

    public void releaseWakeLock(String tag) {
        mDeviceManager.releaseWakeLock(tag);
    }

    public List<RemoteWebManager.WebVisitInfo> getBrowserHistory() {
        return mWebManager.getBrowerHistory();
    }

    public boolean supportBackupOrRestore() {
        return mBackupManager.supportBackupOrRestore();
    }

    public boolean backingUp() {
        return mBackupManager.backingUp();
    }

    public void backup(IZmObserver observer) {
        mBackupManager.backup(observer);
    }

    public void backupSpecial(IZmObserver observer) {
        mBackupManager.backupSpecial(observer);
    }

    public boolean restoring() {
        return mBackupManager.restoring();
    }

    public void restore(IZmObserver observer) {
        mBackupManager.restore(observer);
    }

    public void restoreSpecial(IZmObserver observer) {
        mBackupManager.restoreSpecial(observer);
    }

    public boolean running() {
        return mBackupManager.running();
    }
}
