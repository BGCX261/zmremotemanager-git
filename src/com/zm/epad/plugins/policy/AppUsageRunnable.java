package com.zm.epad.plugins.policy;

import com.android.internal.os.PkgUsageStats;
import com.zm.epad.core.SubSystemFacade;

public class AppUsageRunnable implements Runnable {

    @Override
    public void run() {
        SubSystemFacade system = SubSystemFacade.getInstance();
        PkgUsageStats[] usage = system.getAllPkgUsageStats();
        system.sendNotify(SubSystemFacade.NOTIFY_APP_USAGE, (Object)usage);
    }

}
