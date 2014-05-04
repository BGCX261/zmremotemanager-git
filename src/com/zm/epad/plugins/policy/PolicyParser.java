package com.zm.epad.plugins.policy;

import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

public class PolicyParser {
    private static final String TAG = "PolicyParser";
    private RemotePolicyManager mManager;
    private Context mContext;
    private XmlPullParser mParser;
    private final String TAG_POLICY = "policy";
    private final String TAG_COMMAND = "command";
    private final String TAG_COMMA = ",";

    public PolicyParser(Context context, String policyForm)
            throws XmlPullParserException {
        mContext = context;
        mParser = XmlPullParserFactory.newInstance().newPullParser();
        mManager = SubSystemFacade.getInstance().getRemotePolicyManager();
        StringReader in = new StringReader(policyForm);
        mParser.setInput(in);
    }

    public void parse() throws XmlPullParserException, IOException {
        boolean done = false;
        while (!done) {
            int eventType = mParser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (mParser.getName().equals(TAG_POLICY)) {
                    String type = mParser.getAttributeValue(null,
                            PolicyConstants.ATTR_TYPE);
                    if (type.equals(PolicyConstants.TYPE_SWITCH)) {
                        parseSwitchPolicy(mParser);
                    } else if (type.equals(PolicyConstants.TYPE_ACCUMULATE)) {
                        parseAccumulatePolicy(mParser);
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
        handleSwitchPolicy(action, param, start, end);
    }

    private void handleSwitchPolicy(String action, String param, String start,
            String end) {
        LogManager.local(TAG, "parseSwitchPolicy: act:" + action + ";param:"
                + param);

        if (action.equals(PolicyConstants.ACTION_DISABLE_USER)) {
            TimeSlotPolicy policy = (TimeSlotPolicy) mManager.addPolicy(
                    PolicyConstants.TYPE_SWITCH, start, end);
            policy.setCallback(new DisableUserTimeSlot());
        } else if (action.equals(PolicyConstants.ACTION_ENABLE_USER)) {
            TimeSlotPolicy policy = (TimeSlotPolicy) mManager.addPolicy(
                    PolicyConstants.TYPE_SWITCH, start, end);
            policy.setCallback(new EnableUserTimeSlot());
        } else if (action.equals(PolicyConstants.ACTION_START_APP)) {
            SwitchPolicy policy = (SwitchPolicy) mManager.addPolicy(
                    PolicyConstants.TYPE_SWITCH, start, null);
            policy.setCallback(new StartAppRunnable(mContext, param));
        } else if (action.equals(PolicyConstants.ACTION_APP_USAGE)) {
            SwitchPolicy policy = (SwitchPolicy) mManager.addPolicy(
                    PolicyConstants.TYPE_SWITCH, start, null);
            policy.setCallback(new AppUsageRunnable());
        } else if (action.equals(PolicyConstants.ACTION_POSITION)) {
            TimeSlotPolicy policy = (TimeSlotPolicy) mManager.addPolicy(
                    PolicyConstants.TYPE_SWITCH, start, end);
            policy.setCallback(new LocationTrackTimeSlot());
        }
    }

    private void parseAccumulatePolicy(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        boolean done = false;
        String action = null;
        String param = null;

        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals(PolicyConstants.ACCUMULATE_ACTION)) {
                    action = parser.nextText();
                } else if (parser.getName().equals(
                        PolicyConstants.ACCUMULATE_PARAM)) {
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
        handleAccumulatePolicy(action, param);
    }

    private void handleAccumulatePolicy(String action, String param) {
        LogManager.local(TAG, "handleAccumulatePolicy: act:" + action
                + ";param:" + param);
        if (action.equals(PolicyConstants.ACTION_EYE)) {
            int devide = param.indexOf(TAG_COMMA);
            String accumulate = param.substring(0, devide);
            String closeTime = param.substring(devide + 1);
            AccumulatePolicy policy = (AccumulatePolicy) mManager.addPolicy(
                    PolicyConstants.TYPE_ACCUMULATE, accumulate, null);
            policy.setCallbck(new Accumulate4Eye(mContext, Long
                    .valueOf(closeTime)));
        }
    }

}
