package com.zm.epad.core;

import com.zm.epad.plugins.RemoteDeviceManager;
import com.zm.epad.plugins.RemoteFileManager;
import com.zm.epad.plugins.RemotePackageManager;
import com.zm.epad.plugins.policy.RemotePolicyManager;

import android.content.Context;
import android.os.Bundle;

public class SubSystemFacade {
    
    private RemotePackageManager mPackageManager;
    private RemoteDeviceManager mDeviceManager;
    private RemoteFileManager mFileManager;
    private RemotePolicyManager mPolicyManager;
    
    private Context mContext;
    
    public SubSystemFacade(Context context){
        mContext = context;
    }
    public void start(Bundle loginBundle){
        mPackageManager = RemotePackageManager.getInstance(mContext);
        mDeviceManager = RemoteDeviceManager.getInstance(mContext);
        mFileManager = RemoteFileManager.getInstance(mContext);
        mFileManager.setXmppLoginResource(loginBundle);
        mPolicyManager = RemotePolicyManager.getInstance(mContext);
        mPolicyManager.loadPolicy();
    }
    
    public RemotePackageManager getRemotePackageManager(){
        return mPackageManager;
    }
    public RemoteDeviceManager getRemoteDeviceManager(){
        return mDeviceManager;
    }
    public RemoteFileManager getRemoteFileManager(){
        return mFileManager;
    }
    public RemotePolicyManager getRemotePolicyManager(){
        return mPolicyManager;
    }
    public void stop(){
        RemotePackageManager.release();
        RemoteDeviceManager.release();
        RemoteFileManager.release();
        RemotePolicyManager.release();
    }
}
