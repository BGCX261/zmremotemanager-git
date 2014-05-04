package com.zm.epad.plugins.policy;

import android.provider.Settings;

import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;
import com.zm.epad.plugins.RemoteDeviceManager;
import com.zm.epad.plugins.RemoteDeviceManager.RemoteLocation;
import com.zm.epad.plugins.policy.TimeSlotPolicy.TimeSlotListener;
import com.zm.xmpp.communication.result.IResult;

public class LocationTrackTimeSlot implements TimeSlotListener {

    private static final String TAG = "LocationTrackTimeSlot";

    private long POSITION_DEFAULT_INTERVAL = 60 * 1000; // milliseconds
    private int POSITION_DEFAULT_DISTANCE = 50; // meters

    @Override
    public void onStart(TimeSlotPolicy policy) {
        // TODO Auto-generated method stub
        LogManager.local(TAG, "location track start:" + policy.getId());
        SubSystemFacade.getInstance().startTrackLocation(
                Settings.Secure.LOCATION_MODE_HIGH_ACCURACY,
                POSITION_DEFAULT_INTERVAL, POSITION_DEFAULT_DISTANCE,
                new LocationCallback());
    }

    @Override
    public void onEnd(TimeSlotPolicy policy) {
        // TODO Auto-generated method stub
        LogManager.local(TAG, "location track end:" + policy.getId());
        SubSystemFacade.getInstance().stopTrackLocation();
    }

    @Override
    public boolean runNow(TimeSlotPolicy policy) {
        if (policy.isInSlot()) {
            return true;
        }
        return false;
    }

    private class LocationCallback implements
            RemoteDeviceManager.LocationReportCallback {

        @Override
        public void reportLocation(RemoteLocation loc) {
            SubSystemFacade.getInstance().sendNotify(
                    SubSystemFacade.NOTIFY_POSITION, loc);
        }

        @Override
        public void reportLocationTrackStatus(boolean bRunning) {
            // do nothing

        }
    }
}
