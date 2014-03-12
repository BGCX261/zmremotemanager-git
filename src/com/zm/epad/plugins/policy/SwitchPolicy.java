package com.zm.epad.plugins.policy;

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import com.zm.epad.core.LogManager;

public class SwitchPolicy extends TimePolicy {
    private static final String TAG = "SwitchPolicy";
    protected String mStartTime;
    protected CronExpression mCron;
    protected Runnable mCallback;
    protected TimeZone mTimeZone;

    SwitchPolicy(int id, String start) throws ParseException {
        super(id);
        mType = PolicyConstants.TYPE_SWITCH;
        mStartTime = start;
        mTimeZone = TimeZone.getDefault();
        mCron = new CronExpression(mStartTime);
        mCron.setTimeZone(mTimeZone);
    }

    public void setCallback(Runnable callback) {
        mCallback = callback;
    }

    @Override
    public boolean shouldRunNow() {
        return false;
    }

    @Override
    public void run() {
        if (mCallback != null) {
            mCallback.run();
        }
    }

    @Override
    public long getNextAlarmTime() {
        Date now = new Date();
        Date next = mCron.getNextValidTimeAfter(now);
        LogManager.local(TAG, "SwitchPolicy next:" + next.getTime());
        return next.getTime();
    }
}
