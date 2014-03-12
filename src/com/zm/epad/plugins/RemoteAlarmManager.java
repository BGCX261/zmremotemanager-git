package com.zm.epad.plugins;

import java.util.HashMap;

import com.zm.epad.core.LogManager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class RemoteAlarmManager {

    private static final String TAG = "AlarmReceiver";
    private static final String AlarmAction = "com.zm.epad.ALARM";

    private final String ALARM_ID = "alarmId";
    private Context mContext;
    private AlarmManager mAlarmManger;
    private BroadcastReceiver mReceiver;
    @SuppressWarnings("serial")
    private HashMap<String, ZMAlarm> mWaitingAlarm = new HashMap<String, ZMAlarm>() {
    };

    public static interface AlarmCallback {
        public void wakeUp();
    }

    private class ZMAlarm {
        public ZMAlarm(PendingIntent pi, AlarmCallback cb) {
            pendingIntent = pi;
            callback = cb;
        }

        public PendingIntent pendingIntent;
        public AlarmCallback callback;
    }

    public RemoteAlarmManager(Context context) {
        mContext = context;
        mAlarmManger = (AlarmManager) mContext
                .getSystemService(Context.ALARM_SERVICE);
        mReceiver = new AlarmReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(AlarmAction);
        mContext.registerReceiver(mReceiver, filter);
    }

    public void setAlarm(long triggerAtMillis, String alarmId,
            AlarmCallback callback) throws Exception {
        setAlarm(AlarmManager.RTC_WAKEUP, triggerAtMillis, alarmId, callback);
    }

    public void setAlarm(int type, long triggerAtMillis, String alarmId,
            AlarmCallback callback) throws Exception {
        Intent intent = new Intent();
        intent.setAction(AlarmAction);
        intent.putExtra(ALARM_ID, alarmId);
        PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);
        if (mWaitingAlarm.containsKey(alarmId)) {
            throw new Exception("alarm id is duplicated");
        }

        mWaitingAlarm.put(alarmId, new ZMAlarm(pi, callback));
        LogManager.local(TAG, "set :" + type + " :" + triggerAtMillis);
        mAlarmManger.set(type, triggerAtMillis, pi);
    }

    public void cancelAlarm(String alarmId) {
        ZMAlarm alarm = mWaitingAlarm.get(alarmId);
        if (alarm != null) {
            mAlarmManger.cancel(alarm.pendingIntent);
            mWaitingAlarm.remove(alarmId);
        }
    }

    private class AlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            LogManager.local(TAG, "onReceive :" + intent.getAction());

            if (intent.getAction().equals(AlarmAction)) {
                String alarmId = intent.getStringExtra(ALARM_ID);
                ZMAlarm alarm = mWaitingAlarm.get(alarmId);
                if (alarm != null) {
                    LogManager.local(TAG, "wake up on alarm:" + alarmId);
                    mWaitingAlarm.remove(alarmId);
                    alarm.callback.wakeUp();
                }
            }
        }
    }
}
