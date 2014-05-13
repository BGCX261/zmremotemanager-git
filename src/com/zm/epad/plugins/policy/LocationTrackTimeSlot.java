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

    private static final long POSITION_DEFAULT_INTERVAL = 5 * 60 * 1000; // milliseconds
    private static final int POSITION_DEFAULT_DISTANCE = 50; // meters
    private long mInterval;
    private int mDistance;

    public LocationTrackTimeSlot() {
        this(POSITION_DEFAULT_INTERVAL, POSITION_DEFAULT_DISTANCE);
    }

    public LocationTrackTimeSlot(long interval, int distance) {
        mInterval = interval;
        mDistance = distance;
    }

    @Override
    public void onStart(TimeSlotPolicy policy) {
        // TODO Auto-generated method stub
        LogManager.local(TAG, "location track start:" + policy.getId());
        SubSystemFacade.getInstance().startTrackLocation(
                Settings.Secure.LOCATION_MODE_HIGH_ACCURACY, mInterval,
                mDistance, new LocationCallback());
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
