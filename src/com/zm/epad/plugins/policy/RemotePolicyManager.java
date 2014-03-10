package com.zm.epad.plugins.policy;

import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class RemotePolicyManager {
    private static final String TAG = "RemotePolicyManager";

    private static final String PolicyFile = "policy.xml";
    private static final int EVT_POLICY_ALARM = 100;
    private static final String CHARSET = "utf-8";

    private Context mContext;
    //private HandlerThread mThread;
    private final AlarmManager mAlarmManager;
    private Handler mHandler;
    private List<TimePolicy> mTimePolicies = new ArrayList<TimePolicy>();
    private int mNextPolicyId = 0;

  
    public void stop() {
        LogManager.local(TAG, "stop");
        delete();
    }

    public RemotePolicyManager(Context context) {
        mContext = context;
        mAlarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
        //mThread = new HandlerThread(TAG);
        //mThread.start();
        //mHandler = new Handler(mThread.getLooper(), new policyAlarmCallback());
    }
    
    private void delete() {
        /*try {
            mThread.quit();
            mThread.join();
        } catch (Exception e) {
            LogManager.local(TAG, "destroy:" + e.toString());
        }*/
    }

    public void updatePolicy(String policyForm) throws Exception {
        if (policyForm == null) {
            return;
        }

        // if fail to parse, throw exception and do not write;
        parsePolicy(policyForm);
        writePolicy(policyForm);
        executePolicy();
    }

    public void loadPolicy() {
        if (mHandler == null) {
            mHandler = new Handler(SubSystemFacade.getInstance()
                    .getAThreadLooper());
        }
        try {
            parsePolicy(readPolicy());
        } catch (Exception e) {
            LogManager.local(TAG, "Fail to load policy");
            e.printStackTrace();
        }
        executePolicy();
    }

    public Policy addPolicy(String type, String arg1, String arg2) {
        Policy ret = null;
        try {
            if (type.equals(PolicyConstants.TYPE_SWITCH)) {
                if (arg2 == null) {
                    ret = new SwitchPolicy(getPolicyId(), arg1);
                } else {
                    ret = new TimeSlotPolicy(getPolicyId(), arg1, arg2);
                }
                addTimePolicy_I((TimePolicy) ret);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    private void addTimePolicy_I(TimePolicy policy) {
        if (policy != null) {
            mTimePolicies.add(policy);
        }
    }

    private int getPolicyId() {
        return mNextPolicyId++;
    }

    private void executePolicy() {
        for (TimePolicy p : mTimePolicies) {
            if (p.shouldRunNow()) {
                p.run();
            }
            p.setNextAlarm(p.getNextAlarmTime());
        }
    }

    private class policyAlarmCallback implements Handler.Callback {

        @Override
        public boolean handleMessage(Message msg) {
            boolean ret = false;
            switch (msg.what) {
            case EVT_POLICY_ALARM:
                ret = handlePolicyAlarm((TimePolicy) msg.obj);
                break;
            default:
                break;
            }

            return ret;
        }

        private boolean handlePolicyAlarm(TimePolicy policy) {
            policy.run();
            policy.setNextAlarm(policy.getNextAlarmTime());
            return true;
        }

    }

    private void parsePolicy(String policyForm) throws Exception {
        if (policyForm == null) {
            return;
        }

        try {
            mTimePolicies.clear();
            cancelAll();
            PolicyParser parser = new PolicyParser(mContext, policyForm);
            parser.parse();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void cancelAll() {
        mHandler.removeMessages(EVT_POLICY_ALARM);
        mNextPolicyId = 0;
    }

    private String readPolicy() {
        try {
            File file = new File(mContext.getFilesDir().getAbsolutePath(),
                    PolicyFile);
            if (!file.exists()) {
                return null;
            }
            //@todo: this code really needs improvement.
            FileInputStream in = new FileInputStream(file);
            byte[] policyForm = new byte[(int) file.length()];
            in.read(policyForm);
            in.close();
            return new String(policyForm, CHARSET);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void writePolicy(String policyForm) {
        try {
            File file = new File(mContext.getFilesDir().getAbsolutePath(),
                    PolicyFile);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(file);
            out.write(policyForm.getBytes(CHARSET));
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public abstract class Policy {
        protected int mId;
        protected String mType;

        public Policy(int id) {
            mId = id;
        }

        public int getId() {
            return mId;
        }

        public String getType() {
            return mType;
        }
    }

    public abstract class TimePolicy extends Policy {

        public TimePolicy(int id) {
            super(id);
        }

        public abstract boolean shouldRunNow();

        public abstract void run();

        public abstract long getNextAlarmTime();

        public void setNextAlarm(long next) {

            LogManager.local(TAG, "now:" + System.currentTimeMillis()
                    + "; next:" + next);
            Message msg = mHandler.obtainMessage(EVT_POLICY_ALARM, this);
            mHandler.sendMessageDelayed(msg, next - System.currentTimeMillis());
        }
    }

    public class SwitchPolicy extends TimePolicy {
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

    interface TimeSlotListener {
        void onStart(TimeSlotPolicy policy);

        void onEnd(TimeSlotPolicy policy);

        boolean runNow(TimeSlotPolicy policy);
    }

    public class TimeSlotPolicy extends SwitchPolicy {
        protected String mEndTime;
        int mSlotStart;
        int mSlotEnd;
        TimeSlotListener mCallback;

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
    }

}
