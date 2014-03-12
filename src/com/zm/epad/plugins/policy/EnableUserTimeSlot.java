package com.zm.epad.plugins.policy;

import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;
import com.zm.epad.plugins.policy.TimeSlotPolicy.TimeSlotListener;

public class EnableUserTimeSlot implements TimeSlotListener {

    private static final String TAG = "EnableUserTimeSlot";

    @Override
    public void onStart(TimeSlotPolicy policy) {
        LogManager.local(TAG, "enable user start:" + policy.getId());
        SubSystemFacade.getInstance().setGuestEnabled(true);
        // when start, mandatory lock screen.
        SubSystemFacade.getInstance().lockScreen();
    }

    @Override
    public void onEnd(TimeSlotPolicy policy) {
        LogManager.local(TAG, "enable user end:" + policy.getId());
        SubSystemFacade.getInstance().setGuestEnabled(false);
    }

    @Override
    public boolean runNow(TimeSlotPolicy policy) {
        boolean guest = SubSystemFacade.getInstance().isGuestEnabled();
        if (!guest && policy.isInSlot()) {
            return true;
        } else if (guest && !policy.isInSlot()) {
            return true;
        }
        return false;
    }

}
