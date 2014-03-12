package com.zm.epad.ui;

import com.zm.epad.core.LogManager;

import org.jivesoftware.smack.Connection;

import com.zm.epad.R;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class DebugActivityHome extends Activity {
    public static final String TAG = "DebugActivityHome";

    private Button mConnectBtn = null;
    private Button mDisconnectBtn = null;
    private Button mDebugBtn = null;
    private Button mAdvancedBtn = null;
    private Button mDesktopBtn = null;
    private EditText mIpText = null;
    private EditText mNameText = null;
    private EditText mPwdText = null;
    private TextView mPrompt = null;
    private Context mContext = null;

    String ip = null;
    String username = null;
    String password = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mContext = this;

        Connection.DEBUG_ENABLED = true;

        mConnectBtn = (Button) findViewById(R.id.button1);
        mDisconnectBtn = (Button) findViewById(R.id.button3);
        mIpText = (EditText) findViewById(R.id.editText1);
        mNameText = (EditText) findViewById(R.id.editText2);
        mPwdText = (EditText) findViewById(R.id.editText3);

        mPrompt = (TextView) findViewById(R.id.textView4);

        mDebugBtn = (Button) findViewById(R.id.button4);
        mDesktopBtn = (Button) findViewById(R.id.button_remote_desktop);
        mAdvancedBtn = (Button) findViewById(R.id.button2);

        mConnectBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                ip = mIpText.getText().toString();
                username = mNameText.getText().toString();
                password = mPwdText.getText().toString();
                LogManager.local(TAG, "ip: " + ip);
                Intent intent = new Intent();

                Bundle args = new Bundle();
                args.putString("server", ip);
                args.putString("username", username);
                args.putString("password", password);
                args.putString("resource", "zhimotech");

                intent.putExtras(args);

                OpenXMPP(intent);
                /*
                 * intent.setClass(mContext, DebugActivity.class);
                 * intent.putExtra("ServerIP", ip); intent.putExtra("UserName",
                 * username); intent.putExtra("Password", password);
                 * startActivity(intent);
                 */
            }

        });

        mDisconnectBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                CloseXMPP();
            }

        });

        mDebugBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(mContext, DebugActivitySender.class);
                intent.putExtra("ServerIP", ip);
                startActivity(intent);
            }
        });

        mDesktopBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(mContext, DebugActivityRemoteDesktop.class);
                startActivity(intent);
            }
        });

        mAdvancedBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                LogManager.local(TAG, "advanced");
                Intent intent = new Intent("android.settings.USER_SETTINGS");

                startActivity(intent);
            }

        });

    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        Connection.DEBUG_ENABLED = false;
    }

    private void OpenXMPP(Intent intent) {

        intent.setComponent(new ComponentName("com.zm.epad",
                "com.zm.epad.core.RemoteManagerService"));

        try {
            if (startService(intent) != null) {
                mPrompt.setVisibility(View.VISIBLE);
                LogManager.local(TAG, "start service succeed");
            } else
                LogManager.local(TAG, "start service failed");
        } catch (Exception e) {
            LogManager.local(TAG, "start service failed " + e.getMessage());
        }

    }

    private void CloseXMPP() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.zm.epad",
                "com.zm.epad.core.RemoteManagerService"));
        try {
            stopService(intent);
            mPrompt.setVisibility(View.INVISIBLE);

        } catch (Exception e) {
            LogManager.local(TAG, "stop service failed " + e.getMessage());
        }
    }
}
