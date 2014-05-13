package com.zm.epad.ui;

import com.zm.epad.core.CoreConstants;
import com.zm.epad.core.LogManager;
import com.zm.epad.core.SubSystemFacade;

import org.jivesoftware.smack.Connection;

import com.zm.epad.R;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    private Button mLoginBtn = null;
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
        mDesktopBtn = (Button) findViewById(R.id.button_api_testing);
        mAdvancedBtn = (Button) findViewById(R.id.button2);
        mLoginBtn = (Button) findViewById(R.id.button_login_page);

        mConnectBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                setDefaultValue(false);
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
                intent.putExtra("ServerIP", mIpText.getText().toString());
                startActivity(intent);
            }
        });

        mDesktopBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setClass(mContext, PreferencesForApiTesting.class);
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

        mLoginBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                LogManager.local(TAG, "login page");
                Intent intent = new Intent();
                intent.setClass(mContext, WelcomeActivity.class);
                startActivity(intent);
            }

        });
        setDefaultValue(true);

    }
    private void setDefaultValue(boolean bGetValue){
      SharedPreferences sharePref =  getSharedPreferences(CoreConstants.CONSTANT_DEFAULTSET, 
                Context.MODE_PRIVATE);
      if(bGetValue){
          if(sharePref.contains(CoreConstants.CONSTANT_SERVER)){
              mIpText.setText(sharePref.getString(CoreConstants.CONSTANT_SERVER, ""));
          }
          if(sharePref.contains(CoreConstants.CONSTANT_USRNAME)){
              mNameText.setText(sharePref.getString(CoreConstants.CONSTANT_USRNAME, ""));
          }
          if(sharePref.contains(CoreConstants.CONSTANT_PASSWORD)){
              mPwdText.setText(sharePref.getString(CoreConstants.CONSTANT_PASSWORD, ""));
          } 
      }else{
          SharedPreferences.Editor editor = sharePref.edit();
          editor.putString(CoreConstants.CONSTANT_SERVER, mIpText.getText().toString());
          editor.putString(CoreConstants.CONSTANT_USRNAME, mNameText.getText().toString());
          editor.putString(CoreConstants.CONSTANT_PASSWORD, mPwdText.getText().toString());
          editor.commit();
          editor.apply();
      }
      return;
      
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

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if(SubSystemFacade.getInstance() != null) {
            mPrompt.setVisibility(View.VISIBLE);
        } else {
            mPrompt.setVisibility(View.INVISIBLE);
        }
    }
}
