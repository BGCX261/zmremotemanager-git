package com.zm.epad.plugins.policy;

import com.zm.epad.core.LogManager;
import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class RemotePolicyManager {
    private static final String TAG = "RemotePolicyManager";

    private static final String PolicyFile = "policy.xml";
    private static final String CHARSET = "utf-8";

    private Context mContext;
    private List<TimePolicy> mTimePolicies = new ArrayList<TimePolicy>();
    private int mNextPolicyId = 0;

    public void stop() {
        LogManager.local(TAG, "stop");
        delete();
    }

    public RemotePolicyManager(Context context) {
        mContext = context;
    }

    private void delete() {

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
                addTimePolicyInner((TimePolicy) ret);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    private void addTimePolicyInner(TimePolicy policy) {
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

    private void parsePolicy(String policyForm) throws Exception {
        if (policyForm == null) {
            return;
        }

        try {
            cancelAll();
            PolicyParser parser = new PolicyParser(mContext, policyForm);
            parser.parse();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void cancelAll() {
        for (TimePolicy p : mTimePolicies) {
            p.cancel();
        }
        mTimePolicies.clear();
        mNextPolicyId = 0;
    }

    private String readPolicy() {
        try {
            File file = new File(mContext.getFilesDir().getAbsolutePath(),
                    PolicyFile);
            if (!file.exists()) {
                return null;
            }
            // @todo: this code really needs improvement.
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

}
