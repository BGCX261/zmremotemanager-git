package com.zm.epad.core;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
	    String action = intent.getAction();
	    if(action.equals(Intent.ACTION_BOOT_COMPLETED)){
	        Intent serviceIntent = new Intent();
            serviceIntent.setComponent(new ComponentName("com.zm.epad",
                    "com.zm.epad.core.RemoteManagerService"));
	        context.startService(serviceIntent);
	    }

	}
}