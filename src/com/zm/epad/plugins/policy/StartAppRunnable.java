package com.zm.epad.plugins.policy;

import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;

public class StartAppRunnable implements Runnable {

    private static final String TAG = "StartAppRunnable";
    private String mPkgName;
    private Context mContext;

    public StartAppRunnable(Context context, String pkgNmae) {
        mContext = context;
        mPkgName = pkgNmae;
    }

    @Override
    public void run() {
        LogManager.local(TAG, "start activity:" + mPkgName);
        List<ComponentName> cnList = SubSystemFacade.getInstance()
                .getPackageComponent(Intent.ACTION_MAIN, mPkgName);
        if (cnList.size() > 0) {
            Intent intent = new Intent();
            intent.setComponent(cnList.get(0));
            mContext.startActivity(intent);
        }
    }

}
