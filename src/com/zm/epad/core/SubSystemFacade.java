package com.zm.epad.core;

import com.zm.epad.plugins.RemoteDeviceManager;
import com.zm.epad.plugins.RemoteFileManager;
import com.zm.epad.plugins.RemoteFileManager.FileTransferCallback;
import com.zm.epad.plugins.RemotePackageManager;
import com.zm.epad.plugins.policy.RemotePolicyManager;
import com.zm.epad.structure.Application;
import com.zm.epad.structure.Configuration;
import com.zm.epad.structure.Device;
import com.zm.xmpp.communication.result.ResultRunningApp;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.Looper;
import android.os.UserManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
public class SubSystemFacade {

    private static final String TAG = "SubSystemFacade";

    private RemotePackageManager mPackageManager;
    private RemoteDeviceManager mDeviceManager;
    private RemoteFileManager mFileManager;
    private RemotePolicyManager mPolicyManager;

    private Context mContext;
    private static SubSystemFacade gSubSystemFacade = null;

    public SubSystemFacade(Context context) {
        mContext = context;
    }

    public static SubSystemFacade getInstance() {
        return gSubSystemFacade;
    }

    private ExecutorService mThreadPool;

    public void start(Bundle loginBundle) {
        if (gSubSystemFacade == null)
            gSubSystemFacade = this;

        mThreadPool = Executors.newCachedThreadPool();

        mPackageManager = new RemotePackageManager(mContext);

        mDeviceManager = new RemoteDeviceManager(mContext);

        mFileManager = new RemoteFileManager(mContext);
        mFileManager.setXmppLoginResource(loginBundle);
        mFileManager.setThreadPool(mThreadPool);

        mPolicyManager = new RemotePolicyManager(mContext);
        mPolicyManager.loadPolicy();
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

    public boolean addTaskToSubSystemThreadPool(Runnable task) {
        if (mThreadPool == null)
            return false;
        mThreadPool.execute(task);
        return true;
    }

    public void stop() {
        mPackageManager.stop();
        mPackageManager = null;

        mDeviceManager.stop();
        mDeviceManager = null;

        mFileManager.stop();
        mFileManager = null;

        mPolicyManager.stop();
        mPolicyManager = null;

        shutdownAndAwaitTermination();

        mThreadPool = null;

        gSubSystemFacade = null;
    }

    void shutdownAndAwaitTermination() {
        mThreadPool.shutdown(); // Disable new tasks from being submitted
        try {
          // Wait a while for existing tasks to terminate
          if (!mThreadPool.awaitTermination(60, TimeUnit.SECONDS))
              mThreadPool.shutdownNow(); // Cancel currently executing tasks
            // Wait a while for tasks to respond to being cancelled
            if (!mThreadPool.awaitTermination(60, TimeUnit.SECONDS))
                LogManager.local(TAG,"Pool did not terminate");
          
        } catch (InterruptedException ie) {
          // (Re-)Cancel if current thread also interrupted
            mThreadPool.shutdownNow();
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

    public boolean installPkgForUser(String url, int userId) {
        return mPackageManager.installPkgForUser(url, userId);
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
    public int getCurrentUserId(){
        return mPackageManager.getCurrentUserId();
    }
    
    public boolean isGuestEnabled(){
        return mPackageManager.isGuestEnabled();
    }
    public void startMonitorRunningAppInfo(long interval, RemotePackageManager.ReportRunningAppInfo callback){
        mPackageManager.startMonitorRunningApp(interval,callback);
    }
    public void stopMonitorRunningAppInfo(){
        mPackageManager.stopMonitorRunningApp();
    }
    private class ThreadRunnable implements Runnable{
        private Looper retLooper = null;
        public void run() {
            Looper.prepare();
            retLooper = Looper.myLooper();
            synchronized (this) {
                notifyAll();
            }
            retLooper.loop();
        }
        
        public synchronized Looper  getLooper(){
            if(retLooper == null){
                try {
                    wait();
                } catch (Exception e) {
                    // TODO: handle exception
                }
               
            }
            return retLooper;
        }
    }
    public Looper getAThreadLooper(){
        ThreadRunnable looperHelper = new ThreadRunnable();
        mThreadPool.execute(looperHelper);
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
    
    public boolean changeWallpaper(String wallpaperFile){
        return mDeviceManager.changeWallpaper(wallpaperFile);
    }
    public void lockScreen(){
        mDeviceManager.lockScreen();
    }
    /*
     * Wrapper around RemoteFileManager
     */
    public void downloadFile(String url, FileTransferCallback callback) {
        mFileManager.addFileDownloadTask(url, callback);
    }

    public void uploadScreenshot(String url, Bundle info,
            FileTransferCallback callback) {
        mFileManager.addScreenshotTask(url, info, callback);
    }

    /*
     * Wrapper around RemotePolicyManager
     */
    public void updatePolicy(String policy) {
        mPolicyManager.updatePolicy(policy);
    }
}
