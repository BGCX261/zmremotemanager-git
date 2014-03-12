package com.zm.epad.plugins;

import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;
import com.zm.epad.core.XmppClient;
import com.zm.epad.plugins.RemoteFileManager.*;
import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.client.ZMIQResult;
import com.zm.xmpp.communication.handler.ResultFactory;
import com.zm.xmpp.communication.result.IResult;

import org.jivesoftware.smack.packet.Packet;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.IPackageManager;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.IUserManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;

import android.os.ServiceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RemotePackageManager {
    public static final String TAG = "RemotePkgsManager";

    private IPackageManager mPm;
    private IUserManager mUm;
    private PackageManager mPackageManager;
    private Context mContext;
    private static final ArrayList<PackageVerificationItem> mWhitelist = new ArrayList<PackageVerificationItem>();
    private static final ArrayList<PackageVerificationItem> mBlacklist = new ArrayList<PackageVerificationItem>();
    // lock used to protect white and black list. if debug, can re-name it.
    private static Object mLock = new Object();
    private RunningAppMonitorHandler mRunningAppMonitorHandler;
 
    public void stop() {
        LogManager.local(TAG, "stop");
    }


    // TODO: I cannot define the item info exactly right now.
    private static class PackageVerificationItem {
        String packageName;

        PackageVerificationItem(String name) {
            packageName = name;
        }
    }

    class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        boolean finished;
        boolean result;
        public String pkgName = "";

        public void packageDeleted(String packageName, int returnCode) {
            synchronized (this) {
                finished = true;
                result = returnCode == PackageManager.DELETE_SUCCEEDED;
                pkgName = packageName;
                notifyAll();
            }
        }
    }

    class PackageInstallObserver extends IPackageInstallObserver.Stub {
        boolean finished;
        int result;
        public String pkgName = "";

        public void packageInstalled(String name, int status) {
            synchronized (this) {
                finished = true;
                result = status;
                pkgName = name;
                notifyAll();
            }
        }
    }

    // PackageManagerService will broadcast in InstallParams.handleStartCopy()
    public static class PackageVerificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_PACKAGE_NEEDS_VERIFICATION)) {
                final int verificationId = intent.getIntExtra(
                        PackageManager.EXTRA_VERIFICATION_ID, 0);
                final String packageName = intent
                        .getStringExtra(PackageManager.EXTRA_VERIFICATION_PACKAGE_NAME);
                if (checkInWhitelist(packageName)) {
                    verifyPendingInstall(verificationId,
                            PackageManager.VERIFICATION_ALLOW);
                } else if (checkInBlacklist(packageName)) {
                    verifyPendingInstall(verificationId,
                            PackageManager.VERIFICATION_REJECT);
                } else {
                    verifyPendingInstall(verificationId,
                            PackageManager.VERIFICATION_ALLOW);
                }
            }
        }

        private boolean checkInWhitelist(String packageName) {
            synchronized (mLock) {
                for (PackageVerificationItem item : mWhitelist) {
                    if (item.packageName == packageName) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean checkInBlacklist(String packageName) {
            synchronized (mLock) {
                for (PackageVerificationItem item : mBlacklist) {
                    if (item.packageName == packageName) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void verifyPendingInstall(int verificationId,
                int verificationCode) {
            IPackageManager pm = IPackageManager.Stub
                    .asInterface(ServiceManager.getService("package"));
            try {
                pm.verifyPendingInstall(verificationId, verificationCode);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public RemotePackageManager(Context context) {
        try {
            mContext = context;
            mUm = IUserManager.Stub.asInterface(ServiceManager
                    .getService("user"));
            mPm = IPackageManager.Stub.asInterface(ServiceManager
                    .getService("package"));
            mPackageManager = context.getPackageManager();
        } catch (Exception e) {
        }

    }

    public boolean enablePkgForUser(String pkgName, int userId) {
        LogManager.local(TAG, "enablePkgForUser: " + userId);
        if (pkgName == null)
            return false;
        try {
            // simulate it as pm command
            mPm.setApplicationEnabledSetting(pkgName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0, userId,
                    "shell:" + android.os.Process.myUid());
        } catch (Exception e) {
            LogManager.local(TAG, "enablePkgForUser:" + e.toString());
            return false;
        }
        return true;
    }

    public boolean disablePkgForUser(String pkgName, int userId) {
        LogManager.local(TAG, "disablePkgForUser: " + userId);
        try {
            // simulate it as pm command
            mPm.setApplicationEnabledSetting(pkgName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0, userId,
                    "shell:" + android.os.Process.myUid());
        } catch (Exception e) {
            LogManager.local(TAG, "disablePkgForUser:" + e.toString());
            return false;
        }
        return true;
    }

    // recieve the white or black list from server.
    public void updateWhiteOrBlacklist() {
        synchronized (mLock) {
            mWhitelist.add(new PackageVerificationItem(
                    "com.example.android.apis"));
        }
    }

    /*
     * Download an new app?
     */
    public boolean updatePkgForUser(String pkgName, int userId) {
        return false;
    }

    public boolean unistallPkgForAll(String pkgName) {
        boolean ret = false;

        if (pkgName == null)
            return false;

        try {
            PackageDeleteObserver obs = new PackageDeleteObserver();

            // delete as owner(0)
            mPm.deletePackageAsUser(pkgName, obs, 0,
                    PackageManager.DELETE_ALL_USERS);

            synchronized (obs) {
                while (!obs.finished) {
                    try {
                        obs.wait();
                    } catch (InterruptedException e) {
                    }
                }

                ret = obs.result;
            }
        } catch (Exception e) {
            LogManager.local(TAG, "uninstallPkgForUser:" + e.toString());
            return false;
        }

        return ret;
    }

    public boolean uninstallPkgForUser(String pkgName, int userId) {
        boolean ret = false;

        LogManager.local(TAG, "uninstallPkgForUser: " + userId);
        if (pkgName == null)
            return false;

        try {
            if (UserHandle.myUserId() == userId) {
                // deletePackageAsUser can't be called from other user in system
                // process
                PackageDeleteObserver obs = new PackageDeleteObserver();

                mPm.deletePackageAsUser(pkgName, obs, userId, 0);

                synchronized (obs) {
                    while (!obs.finished) {
                        try {
                            obs.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
                ret = obs.result;
            } else {
                mPm.setApplicationBlockedSettingAsUser(pkgName, true, userId);
                ret = true;
            }
        } catch (Exception e) {
            LogManager.local(TAG, "uninstallPkgForUser:" + e.toString());
        }

        if (isBlockedOrUnstalledByAll(pkgName)) {
            ret = unistallPkgForAll(pkgName);
        }

        return ret;
    }

    public interface installCallback {
        void callback(boolean result);
    }

    private boolean needDownload(String where) {
        if (where.startsWith("http://") || where.startsWith("https://")) {
            return true;
        }
        return false;
    }

    public int installPkgForUser(String apkLocation, final int userId,
            final installCallback cb) {
        LogManager.local(TAG, "installPkgForUser: " + userId);
        if (needDownload(apkLocation)) {
            SubSystemFacade.getInstance().downloadFile(apkLocation,
                              new FileTransferCallback() {
                                int mUserId = userId;
                                installCallback mCallback = cb;

                                @Override
                                public void onDone(FileTransferTask task) {
                                    // TODO Auto-generated method stub
                                    File result = (File) task.getResult();
                                    boolean ret = installPkgForUser(
                                            result.getAbsolutePath(), mUserId);
                                    mCallback.callback(ret);
                                    result.delete();
                                }

                                @Override
                                public void onCancel(FileTransferTask task) {
                                    // TODO Auto-generated method stub
                                    mCallback.callback(false);
                                }

                            });
            return -1;
        } else {
            return installPkgForUser(apkLocation, userId) ? 0 : 1;
        }
    }

    // zhimo://apkname-->/sdcard/xxx/apkname
    private String getRealPath(String apkLocation) {
        if (apkLocation == null)
            return null;
        if (apkLocation.startsWith("http://"))
            return apkLocation;
        else if (apkLocation.startsWith("zhimo://")) {
            String realApkLocation = apkLocation.substring(apkLocation
                    .lastIndexOf("/"));
            File file = Environment.getExternalStorageDirectory();
            realApkLocation = file.getAbsolutePath() + "/" + realApkLocation;
            return realApkLocation;
        } else
            return apkLocation;
    }

    public boolean installPkgForUser(String apkLocation, int userId) {
        LogManager.local(TAG, "installPkgForUser: " + userId);
        apkLocation = getRealPath(apkLocation);
        if (apkLocation == null)
            return false;

        PackageInstallObserver obs = new PackageInstallObserver();
        try {
            Uri apkURI = Uri.parse(apkLocation);
            mPm.installPackage(apkURI, obs, PackageManager.INSTALL_ALL_USERS,
                    null);
            synchronized (obs) {
                while (!obs.finished) {
                    try {
                        obs.wait();
                    } catch (InterruptedException e) {
                    }
                }
                if (obs.result == PackageManager.INSTALL_SUCCEEDED) {
                    List<UserInfo> userList = getAllUsers();
                    for (UserInfo u : userList) {
                        // block all users except requested user
                        if (u.id != userId) {
                            mPm.setApplicationBlockedSettingAsUser(obs.pkgName,
                                    true, u.id);
                        }
                    }
                    return true;
                } else if (obs.result == PackageManager.INSTALL_FAILED_ALREADY_EXISTS) {
                    // if already installed
                    return InstallExsitedPackage(obs.pkgName, userId);
                } else {
                    /*
                     * System.err.println("Failure [" +
                     * installFailureToString(obs.result) + "]");
                     */
                    return false;
                }
            }
        } catch (RemoteException e) {
            LogManager.local(TAG, "installPkgForUser:" + e.toString());
            return false;
        }
    }

    /*
     * we nerver return null;
     */
    public List<PackageInfo> getInstalledPackages(int flags) {
        try {
            return mPackageManager.getInstalledPackages(flags);
        } catch (Exception e) {
            return new ArrayList<PackageInfo>();
        }
    }

    public List<PackageInfo> getInstalledPackages(int flags, int userId) {
        try {
            return mPackageManager.getInstalledPackages(flags, userId);
        } catch (Exception e) {
            return new ArrayList<PackageInfo>();
        }
    }

    public String getApplicationName(PackageInfo pi) {
        return pi.applicationInfo.loadLabel(mPackageManager).toString();
    }

    public String getApplicationName(String pkgName, int flags, int userId) {
        try {
            PackageInfo pi = mPm.getPackageInfo(pkgName, flags, userId);
            return getApplicationName(pi);

        } catch (Exception e) {
            return null;
        }
    }

    public String getApplicationVersion(String pkgName, int flags, int userId) {
        try {
            PackageInfo pi = mPm.getPackageInfo(pkgName, flags, userId);
            return pi.versionName;
        } catch (Exception e) {
            return null;
        }
    }

    public List<RunningAppProcessInfo> getRunningAppProcesses() {
        ActivityManager am = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);

        return am.getRunningAppProcesses();
    }

    public int getCurrentUserId() {
        return ActivityManager.getCurrentUser();
    }

    public List<UserInfo> getAllUsers() {
        try {
            return mUm.getUsers(true);
        } catch (Exception e) {
            return new ArrayList<UserInfo>();
        }

    }

    public Bundle getUserRestrictions(int userId) {
        Bundle userRestrictionInfo = null;

        try {
            userRestrictionInfo = mUm.getUserRestrictions(userId);
        } catch (Exception e) {
            return null;
        }

        return userRestrictionInfo;
    }

    private boolean InstallExsitedPackage(String packageName, int userId) {

        boolean ret = false;
        try {
            ApplicationInfo info = mPm.getApplicationInfo(packageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES, userId);
            LogManager.local(TAG, "installExisting: " + packageName);
            if (info == null
                    || (info.flags & ApplicationInfo.FLAG_INSTALLED) == 0) {
                if (mPm.installExistingPackageAsUser(packageName, userId) == PackageManager.INSTALL_SUCCEEDED) {
                    ret = true;
                }
            } else if ((info.flags & ApplicationInfo.FLAG_BLOCKED) != 0) {
                LogManager.local(TAG, "unblock: " + packageName);
                mPm.setApplicationBlockedSettingAsUser(packageName, false,
                        userId);
                ret = true;
            }
        } catch (Exception e) {
            LogManager.local(TAG, e.toString());
        }

        LogManager.local(TAG, "InstallExsitedPackage: " + ret);
        return ret;
    }

    private boolean isBlockedOrUnstalledByAll(String packageName) {

        boolean ret = true;
        try {
            List<UserInfo> userList = getAllUsers();
            for (UserInfo u : userList) {
                ApplicationInfo info = mPm.getApplicationInfo(packageName,
                        PackageManager.GET_UNINSTALLED_PACKAGES, u.id);
                if (info != null
                        && ((info.flags & ApplicationInfo.FLAG_INSTALLED) != 0)
                        && ((info.flags & ApplicationInfo.FLAG_BLOCKED) == 0)) {
                    // find one not blocked
                    ret = false;
                    break;
                }
            }
        } catch (Exception e) {
            LogManager.local(TAG, e.toString());
            ret = false;
        }

        LogManager.local(TAG, "isBlockedOrUnstalledByAll: " + ret);
        return ret;
    }

    public void setGuestEnabled(boolean enable) {
        try {
            mUm.setGuestEnabled(enable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isGuestEnabled() {
        boolean ret = true;
        try {
            ret = mUm.isGuestEnabled();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }
    public interface ReportRunningAppInfo{
        void reportRunningAppProcessInfos(List<RunningAppProcessInfo> infos);
    }
    public void startMonitorRunningApp(long interval,ReportRunningAppInfo callback){
       if(mRunningAppMonitorHandler == null){
           Looper aTheadLooper = SubSystemFacade.getInstance().getAThreadLooper();
           mRunningAppMonitorHandler = new RunningAppMonitorHandler(aTheadLooper);
       }
       mRunningAppMonitorHandler.start(interval, callback);
    }
    public void stopMonitorRunningApp(){
        if(mRunningAppMonitorHandler == null){
            return;
        }
        mRunningAppMonitorHandler.stop();
        mRunningAppMonitorHandler = null;
    }
    private class RunningAppMonitorHandler extends Handler{
        public static final String DEFAULT_SERVER = Constants.XMPP_NAMESPACE_CENTER;
        public static final int EVT_SCHEDULE_START = 100;
        public static final int EVT_SCHEDULE = 101;
        public static final int EVT_SCHEDULE_STOP = 102;
        
        protected boolean mRunning;
 
   
        protected boolean mImmediateResult = true;
        protected long mInterval = 60 * 60 * 1000; /* default is 1 hour */

        
        protected ReportRunningAppInfo mCallbackAppInfo;
        public RunningAppMonitorHandler(Looper looper){
            super(looper);
        }
        public void stop() {
            removeMessages(EVT_SCHEDULE_START);
            removeMessages(EVT_SCHEDULE);
            Message msg = obtainMessage(EVT_SCHEDULE_STOP);
            sendMessage(msg);
            mRunning = false;
        }

       

        public void setInterval(long interval) {
            mInterval = interval;
        }

        public boolean isRunning() {
            return mRunning;
        }

        public void sendFirstResultImmediately(boolean immediate) {
            mImmediateResult = immediate;
        }

        private void scheduleMessage() {
            Message schedule = obtainMessage(EVT_SCHEDULE);
            sendMessageDelayed(schedule, mInterval);
        }
        public boolean start(long interval, ReportRunningAppInfo callback) {
            boolean bRet = false;
            LogManager.local(TAG, "start interval:" + mInterval + " running:"
                    + mRunning);
            if (mRunning == false) {
                mInterval = interval;
                mCallbackAppInfo = callback;

                Message msg = obtainMessage(EVT_SCHEDULE_START);
                sendMessage(msg);

                bRet = true;
            }
            return bRet;
        }

        public boolean restart() {
            if (mCallbackAppInfo == null) {
                return false;
            }
            return start(mInterval, mCallbackAppInfo);
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case EVT_SCHEDULE_START:
                mRunning = true;
                if (mImmediateResult) {
                    List<RunningAppProcessInfo> runningList = getRunningAppProcesses();
                    if(mCallbackAppInfo != null)
                        mCallbackAppInfo.reportRunningAppProcessInfos(runningList);
                }
                scheduleMessage();
                break;
            case EVT_SCHEDULE:
                if (mRunning == true) {
                    List<RunningAppProcessInfo> runningList = getRunningAppProcesses();
                    if(mCallbackAppInfo != null)
                        mCallbackAppInfo.reportRunningAppProcessInfos(runningList);
                    scheduleMessage();
                }
                break;
            case EVT_SCHEDULE_STOP:
                getLooper().quit();
                break;
            default:
                break;
            }
        }
        
    }

    public List<ComponentName> getPackageComponent(String action, String pkgName) {
        try {
            Intent resolveIntent = new Intent(action, null);
            resolveIntent.setPackage(pkgName);
            List<ResolveInfo> apps = mPackageManager.queryIntentActivities(
                    resolveIntent, 0);
            List<ComponentName> cnList = new ArrayList<ComponentName>();
            for (ResolveInfo ri : apps) {
                String className = ri.activityInfo.name;
                cnList.add(new ComponentName(pkgName, className));
            }
            return cnList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
