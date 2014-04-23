package com.zm.epad.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class Config {
    public static final String SERVER_ADDRESS = "server_address";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String RESOURCE = "resource";

    private final String CONFIG = "config.xml";
    private final String CHARSET = "utf-8";
    private final String CONFIG_PATH;
    private static Config sInstance = null;
    private Context mContext = null;
    private String mDeviceId = null;

    private HashMap<String, String> ConfigMap = new HashMap<String, String>();
    private String[] mUnchangableConfigs = { SERVER_ADDRESS, USERNAME };

    public static Config getConfig() {
        return sInstance;
    }

    public static String getDeviceId() {
        if (sInstance != null) {
            return sInstance.mDeviceId;
        } else {
            return CoreConstants.CONSTANT_BUILDID;
        }
    }

    public static Config loadConfig(Context context) {
        if (sInstance == null) {
            try {
                sInstance = new Config(context);
            } catch (Exception e) {
                e.printStackTrace();
                sInstance = null;
            }
        }
        return sInstance;
    }

    public static void closeAndSaveConfig() {
        if (sInstance != null) {
            sInstance.writeConfigInfo();
        }
        sInstance = null;
    }

    public String getConfig(String key) {
        return ConfigMap.get(key);
    }

    public String setConfig(String key, String value) throws Exception {
        if (!isChangeable(key)) {
            throw new Exception("config(" + key + ") can't be changed");
        }
        return ConfigMap.put(key, value);
    }

    public void saveConfig() {
        writeConfigInfo();
    }

    public boolean isAccountInitiated() {
        return getConfig(Config.PASSWORD) == null ? false : true;
    }

    private boolean isChangeable(String key) {
        for (String s : mUnchangableConfigs) {
            if (key.equals(s)) {
                return false;
            }
        }

        return true;
    }

    private Config(Context context) throws Exception {
        mContext = context;
        CONFIG_PATH = context.getFilesDir().getAbsolutePath();
        readConfigInfo();
        mDeviceId = getConfig(USERNAME);
    }

    private void readConfigInfo() {

        // At first, set default config
        setDefaultConfig();

        File file = new File(CONFIG_PATH, CONFIG);
        if (!file.exists()) {
            // no config file, only use default config
            return;
        }

        // If there is config in config.xml, the config will
        // overwrite the default one
        try {
            FileInputStream in = new FileInputStream(file);
            byte[] config = new byte[(int) file.length()];
            in.read(config);
            in.close();
            parseConfig(new String(config, CHARSET));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void writeConfigInfo() {
        try {
            File file = new File(CONFIG_PATH, CONFIG);
            if (!file.exists()) {
                file.createNewFile();
            }

            Set<Entry<String, String>> config = ConfigMap.entrySet();
            StringBuffer sb = new StringBuffer();
            for (Entry<String, String> entry : config) {
                sb.append("<" + entry.getKey() + ">");
                sb.append(entry.getValue());
                sb.append("</" + entry.getKey() + ">");
                sb.append("\r\n");
            }
            FileOutputStream out = new FileOutputStream(file);
            out.write(sb.toString().getBytes(CHARSET));
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseConfig(String configString) throws Exception {

        XmlPullParser parser = XmlPullParserFactory.newInstance()
                .newPullParser();
        StringReader in = new StringReader(configString);
        parser.setInput(in);

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals(SERVER_ADDRESS)) {
                    ConfigMap.put(SERVER_ADDRESS, parser.nextText());
                } else if (parser.getName().equals(USERNAME)) {
                    ConfigMap.put(USERNAME, parser.nextText());
                } else if (parser.getName().equals(PASSWORD)) {
                    ConfigMap.put(PASSWORD, parser.nextText());
                } else if (parser.getName().equals(RESOURCE)) {
                    ConfigMap.put(RESOURCE, parser.nextText());
                }
            } else if (eventType == XmlPullParser.END_DOCUMENT) {
                done = true;
            }
        }
    }

    private void setDefaultConfig() {
        ConfigMap.put(SERVER_ADDRESS, CoreConstants.CONSTANT_SERVER_ADDRESS);

        // This function may be called before sub-system start
        // So use WifiManager directly instead of RemoteDeviceManager
        WifiManager wifi = (WifiManager) mContext
                .getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        String mac = info.getMacAddress();
        if (mac != null) {
            String userName = mac.replace(":", "-");
            ConfigMap.put(USERNAME, userName);
        }

        ConfigMap.put(RESOURCE, CoreConstants.CONSTANT_DEFALT_RESOURCE);
    }
}
