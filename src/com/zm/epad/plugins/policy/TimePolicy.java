package com.zm.epad.plugins.policy;

import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;
import com.zm.epad.plugins.RemoteAlarmManager;

public abstract class TimePolicy extends Policy {

    public TimePolicy(int id) {
        super(id);
    }

    public abstract boolean shouldRunNow();

    public abstract void run();

    public abstract long getNextAlarmTime();

    public void setNextAlarm(long next) {

        LogManager.local("TimePolicy", "now:" + System.currentTimeMillis()
                + "; next:" + next);
        try {
            SubSystemFacade.getInstance().setAlarm(next, getAlarmId(),
                    new AlarmCallback(this));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    void cancel() {
        SubSystemFacade.getInstance().cancelAlarm(getAlarmId());
    }

    protected class AlarmCallback implements RemoteAlarmManager.AlarmCallback {
        protected TimePolicy mPolicy;

        public AlarmCallback(TimePolicy policy) {
            mPolicy = policy;
        }

        @Override
        public void wakeUp() {
            mPolicy.run();
            mPolicy.setNextAlarm(mPolicy.getNextAlarmTime());
        }

    }

    protected String getAlarmId() {
        return "TimePolicy_" + mId;
    }
}