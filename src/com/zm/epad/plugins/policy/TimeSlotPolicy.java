package com.zm.epad.plugins.policy;

import java.text.ParseException;
import java.util.Calendar;

import com.zm.epad.core.LogManager;

public class TimeSlotPolicy extends SwitchPolicy {
    private static final String TAG = "TimeSlotPolicy";
    protected String mEndTime;
    int mSlotStart;
    int mSlotEnd;
    TimeSlotListener mCallback;

    interface TimeSlotListener {
        void onStart(TimeSlotPolicy policy);

        void onEnd(TimeSlotPolicy policy);

        boolean runNow(TimeSlotPolicy policy);
    }

    TimeSlotPolicy(int id, String start, String end) throws ParseException {
        super(id, start);
        mEndTime = end;
        mSlotStart = convert2IntTime(mStartTime);
        mSlotEnd = convert2IntTime(mEndTime);
        if (mSlotStart > mSlotEnd) {
            throw new ParseException("SlotStart > SlotEnd", 0);
        }
        LogManager.local(TAG, "start:" + mSlotStart + ";end:" + mSlotEnd
                + ";in slot:" + isInSlot());
    }

    public void setCallback(TimeSlotListener callback) {
        mCallback = callback;
    }

    @Override
    public boolean shouldRunNow() {
        if (mCallback != null) {
            return mCallback.runNow(this);
        }
        return true;
    }

    @Override
    public void run() {
        if (mCallback != null) {
            if (isInSlot()) {
                mCallback.onStart(this);
            } else {
                mCallback.onEnd(this);
            }
        }
    }

    @Override
    public long getNextAlarmTime() {
        long next = 0;
        if (isInSlot()) {
            // set end time
            Calendar endTime = Calendar.getInstance(mTimeZone);
            endTime.set(Calendar.HOUR_OF_DAY, mSlotEnd / 3600);
            endTime.set(Calendar.MINUTE, (mSlotEnd % 3600) / 60);
            endTime.set(Calendar.SECOND, mSlotEnd % 60);
            next = endTime.getTimeInMillis();
        } else {
            // set next start time
            next = super.getNextAlarmTime();
        }
        LogManager.local(TAG, "TimeSlotPolicy next:" + next);
        return next;
    }

    public boolean isInSlot() {
        boolean ret = false;

        Calendar currentTime = Calendar.getInstance(mTimeZone);
        int now = currentTime.get(Calendar.HOUR_OF_DAY) * 3600
                + currentTime.get(Calendar.MINUTE) * 60
                + currentTime.get(Calendar.MINUTE);
        if (mSlotStart < now && now < mSlotEnd) {
            ret = true;
        }

        return ret;
    }

    protected int convert2IntTime(String time) throws ParseException {
        int start = 0;
        int index = 0;
        String[] smh = new String[3];

        for (int i = 0; i < 3; i++) {
            index = time.indexOf(" ", start);
            if (index < 0) {
                throw new ParseException(time, index);
            }
            smh[i] = time.substring(start, index);
            start = index + 1;
        }
        LogManager.local(TAG, "parseTime:  " + smh[2] + ":" + smh[1] + ":"
                + smh[0]);
        return Integer.valueOf(smh[2]) * 3600 + Integer.valueOf(smh[1])
                * 60 + Integer.valueOf(smh[0]);
    }

    @Override
    void cancel() {
        super.cancel();
        if (mCallback != null) {
            mCallback.onEnd(this);
        }
    }
}