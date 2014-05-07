package com.zm.epad.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

public class WebServiceClient {
    private static final String TAG = "WebServiceClient";

    public static final int ERR_NO = 0;
    public static final int ERR_UNKNOWN = 1;
    public static final int ERR_NETUNREACH = 2;
    public static final int ERR_LOGIN_CHECK = 3;
    public static final int ERR_ALREADY_DONE = 4;

    private final String YES = "Y";
    private final String CHARSET = "UTF-8";
    private final String PARM_LOGIN_USERNAME = "username";
    private final String PARM_LOGIN_PASSWORD = "pwd";
    private final String PARM_LOGIN_DEVICEID = "deviceid";
    private final String PARM_ASYNC_DEVICEID = "deviceid";
    private final String PARM_ASYNC_PASSWORD = "password";
    private final String PARM_ASYNC_SEQUENCE = "sequence";
    private final String PARM_ASYNC_DONE = "OK";
    private final String PARM_ASYNC_ERROR = "ERROR";

    private Context mContext;
    private HandlerThread mThread;
    private Handler mHandler;

    public interface Result<T> {
        void receiveResult(T result, int errorCode);
    }

    public WebServiceClient(Context context) {
        mContext = context;
    }

    public void start() {
        LogManager.local(TAG, "start");
        mThread = new HandlerThread(TAG);
        mThread.start();
        mHandler = new Handler(mThread.getLooper());
    }

    public void stop() {
        try {
            mThread.quit();
            mThread.join();
            mThread = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getAsyncCommands(final Result<String> result) {
        LogManager.local(TAG, "getAsyncCommands");
        Runnable run = new Runnable() {
            Result<String> mResult = result;

            @Override
            public void run() {
                try {
                    RestClient client = new RestClient();
                    HashMap<String, String> map = new HashMap<String, String>();
                    Config config = Config.getInstance();
                    map.put(PARM_ASYNC_DEVICEID, Config.getDeviceId());
                    map.put(PARM_ASYNC_PASSWORD,
                            config.getConfig(Config.PASSWORD));
                    map.put(PARM_ASYNC_SEQUENCE,
                            config.getConfig(Config.ASYNC_SEQUENCE));
                    URL url = new URL(config.getConfig(Config.ASYNC_SERVER));

                    HttpURLConnection urlconn = (HttpURLConnection) url
                            .openConnection();
                    String commands = client.post(urlconn, map);
                    String sequence = urlconn
                            .getHeaderField(PARM_ASYNC_SEQUENCE);
                    LogManager.local(TAG, "async:" + commands);
                    LogManager.local(TAG, "sequence:" + sequence);
                    if (commands.equals(PARM_ASYNC_DONE)) {
                        config.setConfig(Config.ASYNC_SEQUENCE, "0");
                        config.saveConfig();
                        mResult.receiveResult(null, ERR_ALREADY_DONE);
                    } else if (commands.startsWith(PARM_ASYNC_ERROR)) {
                        mResult.receiveResult(null, ERR_UNKNOWN);
                    } else {
                        config.setConfig(Config.ASYNC_SEQUENCE, sequence);
                        config.saveConfig();
                        mResult.receiveResult(commands, ERR_NO);
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    mResult.receiveResult(null, ERR_UNKNOWN);
                }
            }
        };

        mHandler.post(run);
    }

    public void checkIfUserNameAvailable(final String username,
            final Result<Boolean> result) {
        Runnable run = new Runnable() {
            String mUserName = username;
            Result<Boolean> mResult = result;

            @Override
            public void run() {
                boolean available = false;
                int errorCode = ERR_LOGIN_CHECK;
                try {
                    available = _checkIfUserNameAvailable(mUserName);
                    errorCode = ERR_NO;
                } catch (Exception e) {
                    if (e instanceof ConnectException) {
                        errorCode = ERR_NETUNREACH;
                    }
                }
                if (mResult != null) {
                    mResult.receiveResult(available, errorCode);
                }

            }
        };

        mHandler.post(run);
        return;
    }

    public void login(final String username, final String password,
            final Result<String> result) {
        Runnable run = new Runnable() {
            String mUserName = username;
            String mPassword = password;
            Result<String> mResult = result;

            @Override
            public void run() {
                String password = null;
                int errorCode = ERR_NO;
                try {
                    boolean available = _checkIfUserNameAvailable(username);
                    if (!available) {
                        errorCode = ERR_LOGIN_CHECK;
                        throw new Exception("invalid username");
                    }
                    password = _login(mUserName, mPassword);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (e instanceof ConnectException) {
                        errorCode = ERR_NETUNREACH;
                    } else {
                        errorCode = ERR_UNKNOWN;
                    }
                } finally {
                    if (mResult != null) {
                        mResult.receiveResult(password, errorCode);
                    }
                }
            }
        };

        mHandler.post(run);
        return;
    }

    private boolean _checkIfUserNameAvailable(String username) throws Exception {

        RestClient rest = new RestClient();
        String url = Config.getInstance().getConfig(Config.REST_CHECKEXIST)
                + "?username=" + username;
        String ret = rest.get(url);
        return ret.equals(YES);
    }

    private String _login(String username, String password) throws Exception {

        RestClient rest = new RestClient();
        Map<String, String> map = new HashMap<String, String>();
        map.put(PARM_LOGIN_USERNAME, username);
        map.put(PARM_LOGIN_PASSWORD, password);
        map.put(PARM_LOGIN_DEVICEID, Config.getDeviceId());
        return rest.post(Config.getInstance().getConfig(Config.REST_SIGNON),
                map);

    }

    private class RestClient {
        public String get(String url) throws Exception {

            HttpClient httpclient = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            LogManager.local(TAG, "Rest get:" + result);
            return result;

        }

        public String post(String strUrl, Map<String, String> form)
                throws Exception {

            URL url = new URL(strUrl);

            HttpURLConnection urlconn = (HttpURLConnection) url
                    .openConnection();

            return post(urlconn, form);
        }

        public String post(HttpURLConnection urlconn, Map<String, String> form)
                throws Exception {

            urlconn.setRequestMethod("POST");
            urlconn.setDoOutput(true);
            urlconn.setDoInput(true);
            urlconn.setUseCaches(false);
            urlconn.setAllowUserInteraction(false);
            urlconn.setRequestProperty("connection", "Keep-Alive");
            urlconn.setRequestProperty("Charset", "UTF-8");

            String content = transfermap(form);
            OutputStream outs = urlconn.getOutputStream();
            outs.write(content.toString().getBytes(CHARSET));
            outs.close();

            if (urlconn.getResponseCode() != 200) {
                throw new IOException(urlconn.getResponseMessage());
            }

            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(urlconn.getInputStream()));
            StringBuilder stringBuilder = new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            bufferedReader.close();
            urlconn.disconnect();
            return stringBuilder.toString();
        }

        private String transfermap(Map map) {
            StringBuilder sb = new StringBuilder();
            Iterator it = map.keySet().iterator();
            int i = 0;
            while (it.hasNext()) {
                String key = (String) it.next();
                String value = (String) map.get(key);
                if (i > 0) {
                    sb.append("&");
                }
                sb.append(key);
                sb.append("=");
                sb.append(value);
                i++;
            }
            return sb.toString();
        }
    }
}
