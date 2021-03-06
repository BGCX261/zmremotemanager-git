package com.zm.epad.plugins.policy;

import com.zm.epad.core.Config;
import com.zm.epad.core.CoreConstants;
import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;

import java.io.IOException;
import java.io.StringReader;

public class PolicyParser {
    private static final String TAG = "PolicyParser";
    private RemotePolicyManager mManager;
    private Context mContext;
    private XmlPullParser mParser;
    private final String TAG_POLICY = "policy";
    private final String TAG_COMMAND = "command";

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
            Policy policy = null;
            if (eventType == XmlPullParser.START_TAG) {
                if (mParser.getName().equals(TAG_POLICY)) {
                    String type = mParser.getAttributeValue(null,
                            PolicyConstants.ATTR_TYPE);
                    String publisher = mParser.getAttributeValue(null,
                            PolicyConstants.ATTR_PUBLISHER);
                    String userId = mParser.getAttributeValue(null,
                            PolicyConstants.ATTR_PUBLISHER);
                    if (type.equals(PolicyConstants.TYPE_SWITCH)) {
                        policy = parseSwitchPolicy(mParser);
                    } else if (type.equals(PolicyConstants.TYPE_ACCUMULATE)) {
                        policy = parseAccumulatePolicy(mParser);
                    } else if (type.equals(PolicyConstants.TYPE_SET)) {
                        parseSetPolicy(mParser);
                        policy = null;
                    }
                    if (policy != null) {
                        policy.setPublisher(publisher);
                        try {
                            policy.setUserId(Integer.valueOf(userId));
                        } catch (Exception e) {
                            LogManager.local(TAG, e.toString() + "/setUser:"
                                    + userId);
                            policy.setUserId(-1);
                        }
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

    private Policy parseSwitchPolicy(XmlPullParser parser)
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
        return handleSwitchPolicy(action, param, start, end);
    }

    private Policy handleSwitchPolicy(String action, String param, String start,
            String end) {
        LogManager.local(TAG, "parseSwitchPolicy: act:" + action + ";param:"
                + param);
        Policy ret = null;
        if (action.equals(PolicyConstants.ACTION_DISABLE_USER)) {
            TimeSlotPolicy policy = (TimeSlotPolicy) mManager.addPolicy(
                    PolicyConstants.TYPE_SWITCH, start, end);
            policy.setCallback(new DisableUserTimeSlot());
            ret = policy;
        } else if (action.equals(PolicyConstants.ACTION_ENABLE_USER)) {
            TimeSlotPolicy policy = (TimeSlotPolicy) mManager.addPolicy(
                    PolicyConstants.TYPE_SWITCH, start, end);
            policy.setCallback(new EnableUserTimeSlot());
            ret = policy;
        } else if (action.equals(PolicyConstants.ACTION_START_APP)) {
            try {
                JSONObject json = new JSONObject(param);
                String name = json.getString(PolicyConstants.PARAM_NAME);
                SwitchPolicy policy = (SwitchPolicy) mManager.addPolicy(
                        PolicyConstants.TYPE_SWITCH, start, null);
                policy.setCallback(new StartAppRunnable(mContext, name));
                ret = policy;
            } catch (JSONException e) {
                LogManager.local(TAG, "wrong param for startapp policy:"
                        + param);
                e.printStackTrace();
            }
        } else if (action.equals(PolicyConstants.ACTION_APP_USAGE)) {
            SwitchPolicy policy = (SwitchPolicy) mManager.addPolicy(
                    PolicyConstants.TYPE_SWITCH, start, null);
            policy.setCallback(new AppUsageRunnable());
            ret = policy;
        } else if (action.equals(PolicyConstants.ACTION_POSITION)) {
            try {
                if (param != null) {
                    JSONObject json = new JSONObject(param);
                    String time = json.getString(PolicyConstants.PARAM_TIME);
                    String distance = json
                            .getString(PolicyConstants.PARAM_DISTANCE);
                    TimeSlotPolicy policy = (TimeSlotPolicy) mManager
                            .addPolicy(PolicyConstants.TYPE_SWITCH, start, end);
                    policy.setCallback(new LocationTrackTimeSlot(Long
                            .valueOf(time), Integer.valueOf(distance)));
                    ret = policy;
                } else {
                    TimeSlotPolicy policy = (TimeSlotPolicy) mManager
                            .addPolicy(PolicyConstants.TYPE_SWITCH, start, end);
                    policy.setCallback(new LocationTrackTimeSlot());
                    ret = policy;
                }
            } catch (JSONException e) {
                LogManager.local(TAG, "wrong param for position policy:"
                        + param);
                e.printStackTrace();
            }
        } else if (action.equals(PolicyConstants.ACTION_LOG_UPLOAD)) {
            try {
                JSONObject json = new JSONObject(param);
                String url = json.getString(PolicyConstants.PARAM_URL);
                SwitchPolicy policy = (SwitchPolicy) mManager.addPolicy(
                        PolicyConstants.TYPE_SWITCH, start, null);
                policy.setCallback(new LogUploadRunnable(url,
                        CoreConstants.CONSTANT_INT_LOGTYPE_COMMON));
                ret = policy;
            } catch (JSONException e) {
                LogManager.local(TAG, "wrong param for log policy:" + param);
                e.printStackTrace();
            }
        } else if (action.equals(PolicyConstants.ACTION_LOG_DEBUG_UPLOAD)) {
            try {
                JSONObject json = new JSONObject(param);
                String url = json.getString(PolicyConstants.PARAM_URL);
                SwitchPolicy policy = (SwitchPolicy) mManager.addPolicy(
                        PolicyConstants.TYPE_SWITCH, start, null);
                policy.setCallback(new LogUploadRunnable(url,
                        CoreConstants.CONSTANT_INT_LOGTYPE_RUNTIME));
                ret = policy;
            } catch (JSONException e) {
                LogManager.local(TAG, "wrong param for log policy:" + param);
                e.printStackTrace();
            }
        }
        return ret;
    }

    private Policy parseAccumulatePolicy(XmlPullParser parser)
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
        return handleAccumulatePolicy(action, param);
    }

    private Policy handleAccumulatePolicy(String action, String param) {
        LogManager.local(TAG, "handleAccumulatePolicy: act:" + action
                + ";param:" + param);
        Policy ret = null;
        if (action.equals(PolicyConstants.ACTION_EYE)) {
            try {
                JSONObject json = new JSONObject(param);
                String interval = json
                        .getString(PolicyConstants.PARAM_INTERVAL);
                String duration = json
                        .getString(PolicyConstants.PARAM_INTERVAL);
                AccumulatePolicy policy = (AccumulatePolicy) mManager
                        .addPolicy(PolicyConstants.TYPE_ACCUMULATE, interval,
                                null);
                policy.setCallbck(new Accumulate4Eye(mContext, Long
                        .valueOf(duration)));
                ret = policy;
            } catch (JSONException e) {
                LogManager.local(TAG, "wrong param for eye policy" + param);
                e.printStackTrace();
            }
        }
        return ret;
    }

    private void parseSetPolicy(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        boolean done = false;
        String param = null;

        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals(PolicyConstants.SET_PARAM)) {
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
        handleSetPolicy(param);
    }

    private void handleSetPolicy(String param) {
        JSONObject json;
        try {
            json = new JSONObject(param);
        } catch (JSONException e1) {
            e1.printStackTrace();
            LogManager.local(TAG, "handleSetPolicy failed");
            return;
        }

        JSONArray names = json.names();
        Config config = Config.getInstance();
        for (int i = 0; i < names.length(); i++) {
            try {
                String name = names.getString(i);
                config.setConfig(name, json.getString(name));
            } catch (Exception e) {
                LogManager.local(TAG, "set error:" + e.toString());
            }
        }
        config.saveConfig();
    }
}
