package com.zm.epad.plugins;

import android.content.Context;

import com.zm.epad.core.LogManager;
import com.android.internal.app.IUsageStats;
import com.android.internal.os.PkgUsageStats;
import android.os.ServiceManager;

public class RemoteStatsManager {
    private static String TAG = "RemoteStatsManager";

    
    Context mContext;

    private IUsageStats mUsageStatsService;
   
    public RemoteStatsManager(Context context) {
        mContext = context;
    }
  
   

    public boolean start() {
        if(mUsageStatsService != null)
            return true;
        try {
            mUsageStatsService = IUsageStats.Stub.asInterface(ServiceManager
                    .getService("usagestats"));
            return true;
        } catch (Exception e) {
            LogManager.local(TAG, e.getMessage());
            return false;
        }
       

    }

    public void stop() {
        mUsageStatsService = null;
    }

    public PkgUsageStats[] getAllPkgUsageStats(){
        if(mUsageStatsService == null)
            return null;
        try {
            return mUsageStatsService.getAllPkgUsageStats();
        } catch (Exception e) {
            return null;
        }
    }
}
