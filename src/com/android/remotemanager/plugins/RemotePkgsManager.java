package com.android.remotemanager.plugins;



import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.IPackageManager;
import android.os.IUserManager;
import android.os.ServiceManager;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;


public class RemotePkgsManager {
    
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
    public RemotePkgsManager(Context context){
        try {
            mUm = IUserManager.Stub.asInterface(ServiceManager.getService("user"));
            mPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
            mPackageManager = context.getPackageManager();
        } catch (Exception e) {
            // TODO: handle exception
        }
        
    }
    
    
    
    public boolean enablePkg(String pkgName){
        if(pkgName == null)
            return false;
        try {
            mPm.setApplicationEnabledSetting(pkgName, 
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, 0,0);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    
    public boolean disablePkg(String pkgName){
        try {
            mPm.setApplicationEnabledSetting(pkgName, 
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, 0,0);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
    
    /*
     * Download an new app?
     * */
    public boolean updatePkg(String pkgName){
        return false;
    }
    public boolean uninstallPkg(String pkgName){
        if(pkgName == null)
            return false;
        PackageDeleteObserver obs = new PackageDeleteObserver();
        try {
            mPm.deletePackage(pkgName, obs, 0);

            synchronized (obs) {
                while (!obs.finished) {
                    try {
                        obs.wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
        } catch (RemoteException e) {
           return false;
        }
        return obs.result;
        
        
    }
    public boolean installPkg(String apkLocation){
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
