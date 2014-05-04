package com.zm.epad.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

import com.zm.epad.R;
import com.zm.epad.core.CoreConstants;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterActivity extends Activity {

    private static final String TAG = "RegisterActivity";

    private final String YES = "Y";
    private final String CHARSET = "UTF-8";
    private final String PARM_LOGIN_USERNAME = "username";
    private final String PARM_LOGIN_PASSWORD = "pwd";

    private Button mBtnRegister;
    private Button mBtnBack;
    private EditText mUsername;
    private EditText mPassword;
    private EditText mConfirm;
    private TextView mLoginS;
    private TextView mLoginF;
    private String mResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        mUsername = (EditText) findViewById(R.id.register_inputusername);
        mPassword = (EditText) findViewById(R.id.register_inputpassword);
        mConfirm = (EditText) findViewById(R.id.register_inputconfirm);

        mLoginS = (TextView) findViewById(R.id.regiser_loginsuccess);
        mLoginF = (TextView) findViewById(R.id.regiser_loginfail);

        mBtnRegister = (Button) findViewById(R.id.register_register);
        mBtnRegister.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                register();
            }

        });

        mBtnBack = (Button) findViewById(R.id.register_back);
        mBtnBack.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                finish();
            }

        });
    }

    private void register() {
        String error = null;

        try {
            if (!checkIfNetworkAvailable()) {
                error = getString(R.string.login_logginfn);
                throw new Exception("network closed");
            }
            if (checkIfUsernameExist()) {
                error = getString(R.string.login_registern);
                throw new Exception("username already exist");
            }
            if (!checkIfPasswordConsistent()) {
                error = getString(R.string.login_registerp);
                throw new Exception("different password");
            }
            registerUser();
        } catch (Exception e) {
            Log.d(TAG, "register fail:" + e.getMessage());
            showRegisterFail();
            int duration = Toast.LENGTH_LONG;
            Toast t = Toast.makeText(this, error, duration);
            t.setMargin(0, 0.4f);
            t.show();
        }
    }

    private boolean checkIfNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info == null ? false : info.isConnected();
    }

    private boolean checkIfUsernameExist() {
        mResult = null;
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    String username = mUsername.getText().toString();
                    String url = CoreConstants.CONSTANT_REST_CHECKEXIST
                            + "?username=" + username;
                    RestClient rest = new RestClient();
                    mResult = rest.get(url);
                } catch (Exception e) {
                    mResult = null;
                }

            }

        });
        try {
            t.start();
            t.join();
            if (mResult != null) {
                return mResult.equals(YES);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean checkIfPasswordConsistent() {
        return mPassword.getText().toString()
                .equals(mConfirm.getText().toString());
    }

    private void registerUser() {
        mResult = null;
        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    RestClient rest = new RestClient();
                    Map<String, String> map = new HashMap<String, String>();
                    String username = mUsername.getText().toString();
                    String password = mPassword.getText().toString();
                    map.put(PARM_LOGIN_USERNAME, username);
                    map.put(PARM_LOGIN_PASSWORD, password);
                    mResult = rest.post(CoreConstants.CONSTANT_REST_REGISTER,
                            map);
                    Log.d(TAG, "register result:" + mResult);
                } catch (Exception e) {
                    Log.d(TAG, "registerUser exception:" + e.getMessage());
                    mResult = null;
                }

            }

        });

        try {
            t.start();
            t.join();
            if (mResult != null && mResult.equals(YES)) {
                showRegisterSuccess();
            } else {
                showRegisterFail();
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void showRegisterSuccess() {
        mLoginF.setVisibility(View.INVISIBLE);
        mLoginS.setVisibility(View.VISIBLE);
    }

    private void showRegisterFail() {
        mLoginS.setVisibility(View.INVISIBLE);
        mLoginF.setVisibility(View.VISIBLE);
    }

    private class RestClient {

        private final String PARM_LOGIN_USERNAME = "username";
        private final String PARM_LOGIN_PASSWORD = "pwd";
        private final String PARM_LOGIN_DEVICEID = "deviceid";

        public String get(String url) throws Exception {

            HttpClient httpclient = new DefaultHttpClient();
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity);
            Log.d(TAG, "Rest get:" + result);
            return result;

        }

        public String post(String strUrl, Map<String, String> form)
                throws Exception {

            URL url = new URL(strUrl);

            HttpURLConnection urlconn = (HttpURLConnection) url
                    .openConnection();

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
