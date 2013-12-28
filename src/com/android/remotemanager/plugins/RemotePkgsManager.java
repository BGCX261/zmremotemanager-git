package com.android.remotemanager.plugins;

import android.content.Context;
import android.content.pm.PackageManager;


public class RemotePkgsManager {

    PackageManager mPkgManager = null;
    public RemotePkgsManager(Context context){
        mPkgManager = context.getPackageManager();
    }
    
    
    
    public boolean enablePkg(String pkgName){
        
        return false;
    }
    
    public boolean disablePkg(String pkgName){
        return false;
    }
    
    public boolean updatePkg(String pkgName){
        
    }
    public boolean uninstallPkg(String pkgName){
        
    }
    public boolean installPkg(String pkgName){
        
    }
    
}
