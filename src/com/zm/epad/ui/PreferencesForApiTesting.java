package com.zm.epad.ui;

import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.preference.TwoStatePreference;
import android.util.Log;

import com.zm.epad.R;
import com.zm.epad.core.SubSystemFacade;

import org.apache.http.conn.util.InetAddressUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class PreferencesForApiTesting extends PreferenceActivity {
    public static final String TAG = "DebugApiTesting";

    private Handler mHandler;

    private static final String KEY_USB = "switch_usb_preference";
    private static final String KEY_DESKTOP = "switch_desktop_preference";
    private static final String KEY_BLANK_TIMEOUT = "screen_timeout_list_preference";
    private static final String KEY_BLANK_START = "blank_screen_checkbox_preference";

    // members to test remote desktop
    private SwitchPreference mDesktopPref;
    private Handler mDesktopHandler;
    private Message mDesktopNotify;
    private String mDesktopUrl;

    // members to test blank screen
    private CheckBoxPreference mBlankSceenPref;
    private static final int TIMEOUT_TO_START_BLANK = 2;
    private int mTimeout = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        setPreferenceScreen(createPreferenceHierarchy());
    }

    private SubSystemFacade getInstance() {
        if (SubSystemFacade.getInstance() == null) {
            new SubSystemFacade(this).start(null);
        }
        return SubSystemFacade.getInstance();
    }

    private PreferenceScreen createPreferenceHierarchy() {
        // Root
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

        // Remote Desktop preferences
        PreferenceCategory desktopPrefCat = new PreferenceCategory(this);
        desktopPrefCat.setTitle(R.string.pfat_cat_remote_desktop);
        root.addPreference(desktopPrefCat);

        // USB preference
        SwitchPreference usbPref = new SwitchPreference(this);
        usbPref.setKey(KEY_USB);
        usbPref.setTitle(R.string.pfat_using_usb_interface);
        usbPref.setSummary(R.string.pfat_using_usb_interface_sum);
        usbPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
        desktopPrefCat.addPreference(usbPref);

        // Remote Desktop Opening preference
        mDesktopPref = new SwitchPreference(this);
        mDesktopPref.setKey(KEY_DESKTOP);
        mDesktopPref.setTitle(R.string.pfat_desktop_opening);
        mDesktopPref.setSummary(R.string.pfat_desktop_opening_sum);
        mDesktopPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
        desktopPrefCat.addPreference(mDesktopPref);

        // Screen Toggling preferences
        PreferenceCategory screenBlankPrefCat = new PreferenceCategory(this);
        screenBlankPrefCat.setTitle(R.string.pfat_cat_toggle_screen);
        root.addPreference(screenBlankPrefCat);

        // Screen Toggling timeout preference
        ListPreference timeoutPref = new ListPreference(this);
        timeoutPref.setEntries(R.array.entries_screen_disable_timeout_preference);
        timeoutPref.setEntryValues(R.array.entryvalues_screen_disable_timeout_preference);
        timeoutPref.setDialogTitle(R.string.pfat_toggle_screen_timeout_dialog);
        timeoutPref.setKey(KEY_BLANK_TIMEOUT);
        timeoutPref.setTitle(R.string.pfat_toggle_screen_timeout);
        timeoutPref.setDefaultValue(timeoutPref.getEntryValues()[0]);
        timeoutPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
        screenBlankPrefCat.addPreference(timeoutPref);

        // start screen toggling preference
        mBlankSceenPref = new CheckBoxPreference(this);
        mBlankSceenPref.setKey(KEY_BLANK_START);
        mBlankSceenPref.setTitle(R.string.pfat_blank_screen);
        mBlankSceenPref.setSummary(R.string.pfat_blank_screen_sum);
        mBlankSceenPref.setOnPreferenceChangeListener(mOnPreferenceChangeListener);
        screenBlankPrefCat.addPreference(mBlankSceenPref);

        if (usbPref.isChecked()) {
            setUsbTethering(usbPref, usbPref.isChecked());
        } else {
            checkNetwork(null, false);
        }
        String value = timeoutPref.getValue();
        for (int i = 0; i < timeoutPref.getEntryValues().length; i++) {
            if (value.equals(timeoutPref.getEntryValues()[i])) {
                timeoutPref.setSummary(timeoutPref.getEntries()[i]);
            }
        }
        mTimeout = Integer.parseInt((String)timeoutPref.getValue());
        mBlankSceenPref.setChecked(false);

        return root;
    }

    private Preference.OnPreferenceChangeListener mOnPreferenceChangeListener =
            new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            if ((preference instanceof SwitchPreference)
                    || (preference instanceof CheckBoxPreference)) {
                boolean enable = (Boolean) value;
                TwoStatePreference tsp = (TwoStatePreference) preference;
                if (enable == tsp.isChecked()) return true;
                if (KEY_USB.equals(tsp.getKey())) {
                    setUsbTethering(tsp, enable);
                } else if (KEY_DESKTOP.equals(tsp.getKey())) {
                    if (enable) {
                        testStartRemoteDesktop();
                    } else {
                        testStopRemoteDesktop();
                    }
                } else if (KEY_BLANK_START.equals(preference.getKey())) {
                    testDisableScreen(enable);
                }
                return true;
            } else if (preference instanceof ListPreference) {
                ListPreference pref = (ListPreference) preference;
                if (KEY_BLANK_TIMEOUT.equals(preference.getKey())) {
                    mTimeout = Integer.parseInt((String)value);
                    for (int i = 0; i < pref.getEntryValues().length; i++) {
                        if (value.equals(pref.getEntryValues()[i])) {
                            pref.setSummary(pref.getEntries()[i]);
                        }
                    }
                }
                return true;
            } else {
                Log.e(TAG, "No such preference: " + preference);
            }
            return false;
        }
    };

    private void setUsbTethering(final Preference preference, boolean enable) {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            cm.setUsbTethering(enable);
        }
        preference.setSummary(R.string.pfat_using_usb_interface_sum);
        if (!enable) {
            return;
        }
        mHandler.postDelayed(new Runnable() {
            @Override public void run() {
                checkNetwork(preference, true);
            }
        }, 500);
    }

    private void checkNetwork(final Preference preference, boolean usb) {
        try {
            Enumeration<NetworkInterface> en =
                    NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface nif = en.nextElement();
                if (!nif.supportsMulticast() && (!usb ||
                        nif.getDisplayName().contains("usb"))) continue;
                Enumeration<InetAddress> inet = nif.getInetAddresses();
                while (inet.hasMoreElements()) {
                    InetAddress ip = inet.nextElement();
                    if (!ip.isLoopbackAddress() && InetAddressUtils
                            .isIPv4Address(ip.getHostAddress())) {
                        mDesktopPref.setSummary(ip.getHostAddress());
                        if (usb) {
                            preference.setSummary(ip.getHostAddress() + " "
                                + nif.getName() + " " + nif.getIndex() + " "
                                    + nif.getDisplayName());
                        }
                        return;
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "Get host IP address failed");
            e.printStackTrace();
        }
    }

    Runnable mTestBlankScreen = new Runnable() {
        @Override public void run() {
            mBlankSceenPref.setSummary("Screen should be blanked right now.");
            try {
                getInstance().disableScreen(true);
            } catch (Exception e) {
                Log.e(TAG, "failed to run disableScreen(true)", e);
                mBlankSceenPref.setSummary("failed to run disableScreen(true), see logcat.");
                mBlankSceenPref.setChecked(false);
                return;
            }
            mHandler.postDelayed(new Runnable() {
                @Override public void run() {
                    mBlankSceenPref.setSummary(R.string.pfat_blank_screen_sum);
                    mBlankSceenPref.setChecked(false);
                    try {
                        getInstance().disableScreen(false);
                    } catch (Exception e) {
                        Log.e(TAG, "failed to run disableScreen(false)", e);
                        mBlankSceenPref.setSummary("failed to run" +
                                " disableScreen(false), see logcat.");
                    }
                }
            }, mTimeout * 1000);
        }
    };

    private void testDisableScreen(boolean start) {
        if (start) {
            mBlankSceenPref.setSummary("starting in " + TIMEOUT_TO_START_BLANK + "s");
            mHandler.postDelayed(mTestBlankScreen, TIMEOUT_TO_START_BLANK * 1000);
        } else {
            mHandler.removeCallbacks(mTestBlankScreen);
        }
    }

    private void testStartRemoteDesktop() {
        testStopRemoteDesktop();
        mDesktopHandler = new Handler() {
            @Override public void handleMessage(Message msg) {
                switch (msg.arg1) {
                case SubSystemFacade.MSG_DESKTOP_SERVER_CREATED_OK:
                    mDesktopUrl = (String) msg.obj;
                    mDesktopPref.setSummary("Server Listening at: " + mDesktopUrl);
                    Log.e(TAG, "Server Listening at: " + mDesktopUrl);
                    break;
                case SubSystemFacade.MSG_DESKTOP_RUNNING_OK:
                    mDesktopPref.setSummary("Server " + mDesktopUrl + " running.");
                    Log.e(TAG, "Server " + mDesktopUrl + " running.");
                    break;
                case SubSystemFacade.MSG_DESKTOP_STOPPED:
                    mDesktopPref.setSummary("Server " + mDesktopUrl + " stopped.");
                    Log.e(TAG, "Server " + mDesktopUrl + " stopped.");
                    mDesktopUrl = null;
                    mDesktopPref.setChecked(false);
                    break;
                case SubSystemFacade.MSG_DESKTOP_NOT_SUPPORT:
                    mDesktopPref.setSummary("This Android version not support this function.");
                    Log.e(TAG, "This Android version not support this function.");
                    mDesktopPref.setChecked(false);
                    break;
                case SubSystemFacade.MSG_DESKTOP_IN_USE:
                    mDesktopPref.setSummary("Remote Desktop in used.");
                    Log.e(TAG, "Remote Desktop in used.");
                    break;
                case SubSystemFacade.MSG_DESKTOP_NO_NETWORK:
                    mDesktopPref.setSummary("No network currently.");
                    Log.e(TAG, "No network currently.");
                    mDesktopPref.setChecked(false);
                    break;
                case SubSystemFacade.MSG_DESKTOP_SERVER_CREATE_FAILED:
                    mDesktopPref.setSummary("Server starting faild in listening.");
                    Log.e(TAG, "Server starting faild in listening.");
                    mDesktopPref.setChecked(false);
                    break;
                case SubSystemFacade.MSG_DESKTOP_DISPLAY_CREATE_FAILED:
                    mDesktopPref.setSummary("Server starting faild in display creating.");
                    Log.e(TAG, "Server starting faild in display creating.");
                    mDesktopPref.setChecked(false);
                    break;
                }
            }
        };
        mDesktopNotify = mDesktopHandler.obtainMessage(
                SubSystemFacade.NOTIFY_DESKTOP_SHARE);
        getInstance().startDesktopShare(mDesktopNotify);
    }

    private void testStopRemoteDesktop() {
        getInstance().stopDesktopShare();
        mDesktopNotify = null;
        mDesktopHandler = null;
    }
}
