package com.zm.epad.plugins.policy;

import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;
import com.zm.epad.plugins.RemoteAlarmManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;

public class Accumulate4Eye implements AccumulatePolicy.Callback {

    private static final String TAG = "Accumulate4Eye";

    private Context mContext;
    private long mDuration;
    private Handler mHandler;
    private BroadcastReceiver mReceiver;
    private int mAlarmId;
    private boolean mDisabled;

    public Accumulate4Eye(Context context, long seconds) {
        // the input is seconds, turn it to milliseconds
        mContext = context;
        mDuration = seconds * 1000;
        mDisabled = false;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        mReceiver = new ScreenReceiver();
        mContext.registerReceiver(mReceiver, filter);
    }

    @Override
    public void setHandler(Handler handler) {
        mHandler = handler;
        if (mHandler != null && SubSystemFacade.getInstance().isScreenOn()) {
            Message msg = mHandler.obtainMessage(AccumulatePolicy.EVT_START);
            mHandler.sendMessage(msg);
        }
    }

    @Override
    public void fire() {

        try {
            SubSystemFacade.getInstance().disableScreen(true);
            mAlarmId = SubSystemFacade.getInstance().setAlarm(
                    System.currentTimeMillis() + mDuration,
                    new RemoteAlarmManager.AlarmCallback() {

                        @Override
                        public void wakeUp() {
                            LogManager.local(TAG, "enable Screen:");
                            SubSystemFacade.getInstance().disableScreen(false);
                            mDisabled = false;
                            Message msg = mHandler
                                    .obtainMessage(AccumulatePolicy.EVT_START);
                            mHandler.sendMessage(msg);
                        }
                    });
            mDisabled = true;
            LogManager.local(TAG, "disable Screen");
        } catch (Exception e) {
            SubSystemFacade.getInstance().disableScreen(false);
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        if (mDisabled) {
            SubSystemFacade.getInstance().disableScreen(false);
            SubSystemFacade.getInstance().cancelAlarm(mAlarmId);
            mDisabled = false;
        }
        mContext.unregisterReceiver(mReceiver);
    }

    private class ScreenReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {

            String action = arg1.getAction();
            LogManager.local(TAG, "onReceive:" + action);
            if (mHandler == null) {
                return;
            }

            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                Message msg = mHandler
                        .obtainMessage(AccumulatePolicy.EVT_START);
                mHandler.sendMessage(msg);
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                Message msg = mHandler
                        .obtainMessage(AccumulatePolicy.EVT_INTERRUPT);
                mHandler.sendMessage(msg);
            }
        }
    }
}
