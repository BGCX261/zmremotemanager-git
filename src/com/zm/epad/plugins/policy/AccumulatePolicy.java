package com.zm.epad.plugins.policy;

import android.os.Handler;
import android.os.Message;

import com.zm.epad.core.SubSystemFacade;
import com.zm.epad.plugins.RemoteAlarmManager;

public class AccumulatePolicy extends Policy {

    public static final int EVT_START = 1000;
    public static final int EVT_INTERRUPT = 1001;

    protected long mAccumulation;
    protected int mAlarmId = -1;
    protected boolean mAccumulating;
    protected Handler mHandler;
    protected Callback mCallback;

    public interface Callback {
        void setHandler(Handler handler);

        void fire();

        void stop();
    }

    public AccumulatePolicy(int id, long seconds) {
        super(id);
        // the input is seconds, turn it to milliseconds
        mAccumulation = seconds * 1000;
        mAccumulating = false;
        mHandler = new PolicyHandler();
    }

    public void setCallbck(Callback cb) {
        mCallback = cb;
        mCallback.setHandler(mHandler);
    }

    @Override
    void cancel() {
        interrupt();
        if (mCallback != null) {
            mCallback.setHandler(null);
            mCallback = null;
        }
    }

    protected void start() {
        if (!mAccumulating) {
            try {
                mAlarmId = SubSystemFacade.getInstance().setAlarm(
                        System.currentTimeMillis() + mAccumulation,
                        new AlarmCallback());
                mAccumulating = true;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    protected void interrupt() {
        if (mAccumulating) {
            SubSystemFacade.getInstance().cancelAlarm(mAlarmId);
            mAccumulating = false;
        }
    }

    protected class AlarmCallback implements RemoteAlarmManager.AlarmCallback {

        @Override
        public void wakeUp() {
            if (mCallback != null) {
                mAccumulating = false;
                mCallback.fire();
            }
        }

    }

    protected class PolicyHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case EVT_START:
                start();
                break;
            case EVT_INTERRUPT:
                interrupt();
                break;
            default:
                break;
            }
        }
    }
}
