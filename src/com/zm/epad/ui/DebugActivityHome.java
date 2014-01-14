package com.zm.epad.ui;

import com.zm.epad.R;

import org.jivesoftware.smack.Connection;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.zm.epad.core.LogManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class DebugActivityHome extends Activity {
    public static final String TAG = "DebugActivityHome";

    private Button mConnectBtn = null;
    private Button mAdvancedBtn = null;
    private EditText mIpText = null;
    private EditText mNameText = null;
    private EditText mPwdText = null;
    private Context mContext = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mContext = this;

        Connection.DEBUG_ENABLED = true;

        mConnectBtn = (Button) findViewById(R.id.button1);
        mIpText = (EditText) findViewById(R.id.editText1);
        mNameText = (EditText) findViewById(R.id.editText2);
        mPwdText = (EditText) findViewById(R.id.editText3);
        
        mAdvancedBtn = (Button) findViewById(R.id.button2);

        mConnectBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                String ip = mIpText.getText().toString();
                String username = mNameText.getText().toString();
                String password = mPwdText.getText().toString();
                LogManager.local(TAG, "ip: " + ip);
                Intent intent = new Intent();
                intent.setClass(mContext, DebugActivity.class);
                intent.putExtra("ServerIP", ip);
                intent.putExtra("UserName", username);
                intent.putExtra("Password", password);
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

}
