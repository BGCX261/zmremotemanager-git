package com.zm.epad.ui;

import com.zm.epad.R;
import com.zm.epad.RemoteManager;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends Activity {

    private static final String TAG = "LoginActivity";

    private final String REGISTER_PAGE = "http://114.215.110.230:8080/zmepadconsole/spring/login"; // temp
    private final int LOGIN_REQUEST_CODE = 1;

    private Button mBtnRegister;
    private Button mBtnLogin;
    private Button mBtnDone;
    private EditText mUsername;
    private EditText mPassword;
    private TextView mLogging;
    private ProgressBar mLoggingBar;
    private TextView mLoginS;
    private TextView mLoginF;

    private RemoteManager mRm;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        switch (requestCode) {
        case LOGIN_REQUEST_CODE:
            handleLoginResult(resultCode);
            break;
        default:
            break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        mRm = new RemoteManager();

        mUsername = (EditText) findViewById(R.id.login_inputusername);
        mPassword = (EditText) findViewById(R.id.login_inputpassword);
        mBtnRegister = (Button) findViewById(R.id.login_register);
        mBtnRegister.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri url = Uri.parse(REGISTER_PAGE);
                intent.setData(url);
                startActivity(intent);
            }

        });

        mBtnLogin = (Button) findViewById(R.id.login_login);
        mBtnLogin.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent data = new Intent();
                PendingIntent pi = createPendingResult(LOGIN_REQUEST_CODE,
                        data, 0);
                String username = mUsername.getText().toString();
                String password = mPassword.getText().toString();
                boolean ret = mRm.login(username, password, pi);
                if (ret) {
                    showLogging();
                } else {
                    Log.w(TAG, "login false");
                }
            }

        });

        mBtnDone = (Button) findViewById(R.id.login_done);
        mBtnDone.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                finish();
            }

        });

        mLogging = (TextView) findViewById(R.id.login_logining);
        mLoggingBar = (ProgressBar) findViewById(R.id.login_loggingbar);
        mLoginS = (TextView) findViewById(R.id.login_loginsuccess);
        mLoginF = (TextView) findViewById(R.id.login_loginfail);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if (mRm.isLogined()) {
            showAlreadyLogin();
        }
    }

    private void showLogging() {
        mLoginS.setVisibility(View.INVISIBLE);
        mLoginF.setVisibility(View.INVISIBLE);
        mLogging.setVisibility(View.VISIBLE);
        mLoggingBar.setVisibility(View.VISIBLE);
    }

    private void showLogSuccess() {
        mLoginF.setVisibility(View.INVISIBLE);
        mLogging.setVisibility(View.INVISIBLE);
        mLoggingBar.setVisibility(View.INVISIBLE);
        mLoginS.setVisibility(View.VISIBLE);
    }

    private void showLogFail() {
        mLogging.setVisibility(View.INVISIBLE);
        mLoggingBar.setVisibility(View.INVISIBLE);
        mLoginS.setVisibility(View.INVISIBLE);
        mLoginF.setVisibility(View.VISIBLE);
    }

    private void showAlreadyLogin() {
        TextView notice = (TextView) findViewById(R.id.login_notice);
        notice.setVisibility(View.INVISIBLE);
        mUsername.setVisibility(View.INVISIBLE);
        mPassword.setVisibility(View.INVISIBLE);
        mLogging.setVisibility(View.INVISIBLE);
        mLoggingBar.setVisibility(View.INVISIBLE);
        mLoginF.setVisibility(View.INVISIBLE);
        mBtnRegister.setVisibility(View.INVISIBLE);
        mBtnLogin.setVisibility(View.INVISIBLE);

        TextView already = (TextView) findViewById(R.id.login_alreadylogin);
        already.setVisibility(View.VISIBLE);
        mLoginS.setVisibility(View.VISIBLE);
        mBtnDone.setVisibility(View.VISIBLE);
    }

    private void handleLoginResult(int resultCode) {
        int error = 0;
        switch (resultCode) {
        case RemoteManager.RESULT_OK:
            showAlreadyLogin();
            break;
        case RemoteManager.RESULT_NETWORK_ERROR:
            error = R.string.login_logginfn;
            showLogFail();
            break;
        case RemoteManager.RESULT_LOGIN_INFO_ERROR:
            error = R.string.login_logginfi;
            showLogFail();
            break;
        default:
            break;
        }
        if (error > 0) {
            int duration = Toast.LENGTH_SHORT;
            Toast t = Toast.makeText(this, error, duration);
            t.setMargin(0, 0.4f);
            t.show();
        }
    }
}
