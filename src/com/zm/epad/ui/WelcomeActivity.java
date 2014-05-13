package com.zm.epad.ui;

import com.zm.epad.R;
import com.zm.epad.RemoteManager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class WelcomeActivity extends Activity {

    private static final String TAG = "WelcomeActivity";

    Button mBtnBack;
    Button mBtnNext;
    Button mBtnDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome);
        mBtnBack = (Button) findViewById(R.id.welcome_back);
        mBtnBack.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                finish();
            }

        });

        mBtnNext = (Button) findViewById(R.id.welcome_next);
        mBtnNext.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent();
                intent.setClass(getBaseContext(), LoginActivity.class);
                ;
                startActivity(intent);
            }

        });

        mBtnDone = (Button) findViewById(R.id.welcome_done);
        mBtnDone.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                finish();
            }

        });
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        RemoteManager rm = new RemoteManager();
        if (rm.isLogined()) {
            showAlreadyLogin();
        }
    }

    private void showAlreadyLogin() {
        TextView content = (TextView) findViewById(R.id.welcome_content);
        content.setVisibility(View.INVISIBLE);

        mBtnBack.setVisibility(View.INVISIBLE);
        mBtnNext.setVisibility(View.INVISIBLE);

        TextView already = (TextView) findViewById(R.id.welcome_alreadylogin);
        already.setVisibility(View.VISIBLE);
        mBtnDone.setVisibility(View.VISIBLE);
    }
}
