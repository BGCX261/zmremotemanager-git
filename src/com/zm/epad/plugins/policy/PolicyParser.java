package com.zm.epad.plugins.policy;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.zm.epad.core.LogManager;
import com.zm.epad.plugins.RemoteDeviceManager;
import com.zm.epad.plugins.RemotePackageManager;
import com.zm.epad.plugins.policy.RemotePolicyManager.SwitchPolicy;
import com.zm.epad.plugins.policy.RemotePolicyManager.TimeSlotPolicy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class PolicyParser {
    private static final String TAG = "PolicyParser";
    private RemotePolicyManager mManager;
    private Context mContext;
    private XmlPullParser mParser;
    private final String TAG_POLICY = "policy";
    private final String TAG_COMMAND = "command";
    @SuppressWarnings("serial")
    private final HashMap<String, Intent> activityMap = new HashMap<String, Intent>() {

        {
            put("zmdebug", new Intent().setComponent(new ComponentName(
                    "com.zm.epad", "com.zm.epad.core.RemoteManagerService")));
        }
    };

    public PolicyParser(Context context, String policyForm)
            throws XmlPullParserException {
        mContext = context;
        mParser = XmlPullParserFactory.newInstance().newPullParser();
        mManager = RemotePolicyManager.getInstance();
        StringReader in = new StringReader(policyForm);
        mParser.setInput(in);
    }

    public void parse() throws XmlPullParserException, IOException {
        boolean done = false;
        while (!done) {
            int eventType = mParser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (mParser.getName().equals(TAG_POLICY)) {
                    String type = mParser.getAttributeValue(null, PolicyConstants.ATTR_TYPE);
                    if (type.equals(PolicyConstants.TYPE_SWITCH)) {
                        parseSwitchPolicy(mParser);
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (mParser.getName().equals(TAG_COMMAND)) {
                    done = true;
                }
            } else if (eventType == XmlPullParser.END_DOCUMENT) {
                done = true;
            }
        }
    }

    private void parseSwitchPolicy(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        boolean done = false;
        String action = null;
        String start = null;
        String end = null;
        String param = null;

        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals(PolicyConstants.SWITCH_ACTION)) {
                    action = parser.nextText();
                } else if (parser.getName()
                        .equals(PolicyConstants.SWITCH_START)) {
                    start = parser.nextText();
                } else if (parser.getName().equals(PolicyConstants.SWITCH_END)) {
                    end = parser.nextText();
                } else if (parser.getName()
                        .equals(PolicyConstants.SWITCH_PARAM)) {
                    param = parser.nextText();
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(TAG_POLICY)) {
                    done = true;
                }
            } else if (eventType == XmlPullParser.END_DOCUMENT) {
                done = true;
            }
        }

        LogManager.local(TAG, "parseSwitchPolicy: act:" + action + ";param:"
                + param);

        if (action.equals(PolicyConstants.ACTION_DISABLE_USER)) {
            TimeSlotPolicy policy = (TimeSlotPolicy) mManager.addPolicy(
                    PolicyConstants.TYPE_SWITCH, start, end);
            policy.setCallback(new RemotePolicyManager.TimeSlotListener() {

                @Override
                public void onStart(TimeSlotPolicy policy) {
                    LogManager.local(TAG,
                            "disable user start:" + policy.getId());
                    RemotePackageManager.getInstance().setGuestEnabled(false);
                    // when start, mandatory lock screen.
                    RemoteDeviceManager.getInstance().lockScreen();
                }

                @Override
                public void onEnd(TimeSlotPolicy policy) {
                    LogManager.local(TAG, "disable user end:" + policy.getId());
                    RemotePackageManager.getInstance().setGuestEnabled(true);
                }

                @Override
                public boolean runNow(TimeSlotPolicy policy) {
                    boolean guest = RemotePackageManager.getInstance()
                            .isGusetEnabled();
                    if (guest && policy.isInSlot()) {
                        return true;
                    } else if (!guest && !policy.isInSlot()) {
                        return true;
                    }
                    return false;
                }

            });
        } else if (action.equals(PolicyConstants.ACTION_ENABLE_USER)) {
            TimeSlotPolicy policy = (TimeSlotPolicy) mManager.addPolicy(
                    PolicyConstants.TYPE_SWITCH, start, end);
            policy.setCallback(new RemotePolicyManager.TimeSlotListener() {

                @Override
                public void onStart(TimeSlotPolicy policy) {
                    LogManager.local(TAG, "enable user start:" + policy.getId());
                    RemotePackageManager.getInstance().setGuestEnabled(true);
                    // when start, mandatory lock screen.
                    RemoteDeviceManager.getInstance().lockScreen();
                }

                @Override
                public void onEnd(TimeSlotPolicy policy) {
                    LogManager.local(TAG, "enable user end:" + policy.getId());
                    RemotePackageManager.getInstance().setGuestEnabled(false);
                }

                @Override
                public boolean runNow(TimeSlotPolicy policy) {
                    boolean guest = RemotePackageManager.getInstance()
                            .isGusetEnabled();
                    if (!guest && policy.isInSlot()) {
                        return true;
                    } else if (guest && !policy.isInSlot()) {
                        return true;
                    }
                    return false;
                }

            });
        } else if (action.equals(PolicyConstants.ACTION_ENABLE_USER)) {
            SwitchPolicy policy = (SwitchPolicy) mManager.addPolicy(
                    PolicyConstants.TYPE_SWITCH, start, null);
            final Intent intent = activityMap.get(param);
            policy.setCallback(new Runnable() {
                @Override
                public void run() {
                    LogManager.local(TAG, "start activity:"
                            + intent.getComponent().toString());
                    mContext.startActivity(intent);
                }
            });
        }
    }
}
