package com.zm.epad.plugins;



import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.IPackageManager;
import android.os.Environment;
import android.os.IUserManager;
import android.os.ServiceManager;
import android.util.Log;
import android.net.Uri;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class RemotePkgsManager {
    public static final String TAG="RemotePkgsManager";
	
    class PackageDeleteObserver extends IPackageDeleteObserver.Stub {
        boolean finished;
        boolean result;

        public void packageDeleted(String packageName, int returnCode) {
            synchronized (this) {
                finished = true;
                result = returnCode == PackageManager.DELETE_SUCCEEDED;
                notifyAll();
            }
        }
    }
    class PackageInstallObserver extends IPackageInstallObserver.Stub {
        boolean finished;
        int result;

        public void packageInstalled(String name, int status) {
            synchronized( this) {
                finished = true;
                result = status;
                notifyAll();
            }
        }
    }
    IPackageManager mPm;
    IUserManager mUm;
    PackageManager mPackageManager;
    Context mContext;
    public RemotePkgsManager(Context context){
        try {
            mContext = context;
            mUm = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
            mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
            mPackageManager = context.getPackageManager();
        } catch (Exception e) {
            // TODO: handle exception
        }
        
    }
    
    
    public boolean enablePkgForUser(String pkgName, int userId){
        if(pkgName == null)
            return false;
        try {
        	//simulate it as pm command
            mPm.setApplicationEnabledSetting(pkgName, 
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0,userId,
                    "shell:" + android.os.Process.myUid());
        } catch (Exception e) {
        	Log.e(TAG,"enablePkgForUser:"+e.toString());
            return false;
        }
        return true;
    }
    
    public boolean disablePkgForUser(String pkgName, int userId){
        try {
        	//simulate it as pm command
            mPm.setApplicationEnabledSetting(pkgName, 
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0,userId,
                    "shell:" + android.os.Process.myUid());
        } catch (Exception e) {
        	Log.e(TAG,"disablePkgForUser:"+e.toString());
            return false;
        }
        return true;
    }
    
    /*
     * Download an new app?
     * */
    public boolean updatePkgForUser(String pkgName, int userId){
        return false;
    }
    /*
     * Special note:
     * For uninstall and install, userId is not used, because 
     * PackageManager will use caller's userid.This means 
     * User A could not install or uninstall apk for other users 
     * except for himself
     * 
     * */
    public boolean uninstallPkgForUser(String pkgName,int userId){
        if(pkgName == null)
            return false;
        PackageDeleteObserver obs = new PackageDeleteObserver();
        try {
            mPm.deletePackageAsUser(pkgName, obs, 0,0);

            synchronized (obs) {
                while (!obs.finished) {
                    try {
                        obs.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        } catch (RemoteException e) {
        	Log.e(TAG,"uninstallPkgForUser:"+e.toString());
           return false;
        }
        return obs.result;
        
        
    }
    //zhimo://apkname-->/sdcard/xxx/apkname
    private String getRealPath(String apkLocation){
        if(apkLocation == null)
            return null;
        if(apkLocation.startsWith("http://"))
            return apkLocation;
        else if(apkLocation.startsWith("zhimo://")){
            String realApkLocation = apkLocation.substring(apkLocation.lastIndexOf("/"));
            File file = Environment.getExternalStorageDirectory();
            realApkLocation = file.getAbsolutePath() + "/" + realApkLocation;
            return realApkLocation;
        } else
            return apkLocation;
    }
    public boolean installPkgForUser(String apkLocation,int userId){
        apkLocation = getRealPath(apkLocation);
        if(apkLocation == null)
            return false;
        PackageInstallObserver obs = new PackageInstallObserver();
        try {
            Uri apkURI = Uri.parse(apkLocation);
             mPm.installPackage(apkURI, obs, 0,null);
            synchronized (obs) {
                while (!obs.finished) {
                    try {
                        obs.wait();
                    } catch (InterruptedException e) {
                    }
                }
                if (obs.result == PackageManager.INSTALL_SUCCEEDED) {
                    return true;
                } else {
                   /* System.err.println("Failure ["
                            + installFailureToString(obs.result)
                            + "]");*/
                    return false;
                }
            }
        } catch (RemoteException e) {
        	Log.e(TAG,"installPkgForUser:"+e.toString());
            return false;
        }
    }
    
    /*
     * we nerver return null;
     * */
    public List<PackageInfo> getInstallPkgs(int flags){
        try {
            return mPackageManager.getInstalledPackages(flags);
        } catch (Exception e) {
            return new ArrayList<PackageInfo>();
        }
    }
    
    
}
