package com.zm.epad.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.zm.epad.core.LogManager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class RemoteAlarmManager {

    private static final String TAG = "RemoteAlarmManager";
    private static final String AlarmAction = "com.zm.epad.ALARM";

    private final int ALARM_NUM_MAX = 200;
    private Context mContext;
    private AlarmManager mAlarmManger;
    private BroadcastReceiver mReceiver;
    @SuppressWarnings("serial")
    private List<ZMAlarm> mAlarmList = new ArrayList<ZMAlarm>(ALARM_NUM_MAX) {
    };

    public static interface AlarmCallback {
        public void wakeUp();
    }

    private class ZMAlarm {
        public ZMAlarm(int alarmId) {
            id = alarmId;
            action = AlarmAction + id;
        }

        public final int id;
        public final String action;
        private PendingIntent mPendingIntent = null;
        private AlarmCallback mCallback = null;

        public boolean isActive() {
            return mPendingIntent == null ? false : true;
        }

        public void activate(PendingIntent pi, AlarmCallback cb) {
            mPendingIntent = pi;
            mCallback = cb;
        }

        public void inactivate() {
            mPendingIntent = null;
            mCallback = null;
        }

        public PendingIntent getPendingIntent() {
            return mPendingIntent;
        }

        public void goOff() {
            if (mCallback != null) {
                mCallback.wakeUp();
            }
        }
    }

    public RemoteAlarmManager(Context context) {
        mContext = context;
        mAlarmManger = (AlarmManager) mContext
                .getSystemService(Context.ALARM_SERVICE);
        mReceiver = new AlarmReceiver();
        IntentFilter filter = new IntentFilter();
        for (int i = 0; i < ALARM_NUM_MAX; i++) {
            ZMAlarm alarm = new ZMAlarm(i);
            mAlarmList.add(alarm);
            filter.addAction(alarm.action);
        }
        mContext.registerReceiver(mReceiver, filter);
    }

    public int setAlarm(long triggerAtMillis, AlarmCallback callback)
            throws Exception {
        return setAlarm(AlarmManager.RTC_WAKEUP, triggerAtMillis, callback);
    }

    public int setAlarm(int type, long triggerAtMillis, AlarmCallback callback)
            throws Exception {
        ZMAlarm alarm = null;
        PendingIntent pi = null;
        synchronized (mAlarmList) {
            for (ZMAlarm a : mAlarmList) {
                if (!a.isActive()) {
                    alarm = a;
                    break;
                }
            }
            if (alarm == null) {
                throw new Exception("Not enough alarm");
            }
            Intent intent = new Intent();
            intent.setAction(alarm.action);

            pi = PendingIntent.getBroadcast(mContext, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT);

            alarm.activate(pi, callback);
        }
        LogManager.local(TAG, "set :" + type + " :" + triggerAtMillis + " :"
                + alarm.id);
        mAlarmManger.set(type, triggerAtMillis, pi);
        return alarm.id;
    }

    public void cancelAlarm(int alarmId) {
        synchronized (mAlarmList) {
            ZMAlarm alarm = mAlarmList.get(alarmId);
            if (alarm != null && alarm.isActive()) {
                mAlarmManger.cancel(alarm.getPendingIntent());
                alarm.inactivate();
            }
        }
    }

    public void stop() {
        LogManager.local(TAG, "stop");
        mContext.unregisterReceiver(mReceiver);
        synchronized (mAlarmList) {
            for (ZMAlarm a : mAlarmList) {
                if (a.isActive()) {
                    mAlarmManger.cancel(a.getPendingIntent());
                }
            }
            mAlarmList.clear();
        }
    }

    private int getAlarmId(String action) {
        return Integer.valueOf(action.substring(AlarmAction.length()));
    }

    private class AlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            LogManager.local(TAG, "onReceive :" + intent.getAction());

            int alarmId = getAlarmId(intent.getAction());
            ZMAlarm alarm = mAlarmList.get(alarmId);
            if (alarm.isActive()) {
                LogManager.local(TAG, "wake up on alarm:" + alarmId);
                alarm.goOff();
                alarm.inactivate();
            }
        }
    }
}
