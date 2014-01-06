package com.android.remotemanager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class DebugActivityHome extends Activity {
	public static final String TAG = "DebugActivityHome";
	
	private Button mConnectBtn = null;
	private Button mAdvancedBtn = null;
	private EditText mIpText = null; 
	private Context mContext = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
   	
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        
		setContentView(R.layout.activity_main);		
		mContext = this;
		
		mConnectBtn = (Button)findViewById(R.id.button1);
		mIpText = (EditText)findViewById(R.id.editText1);
		mAdvancedBtn = (Button)findViewById(R.id.button2);

		
		mConnectBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				String ip = mIpText.getText().toString();
				Log.v(TAG, "ip: "+ ip);
				Intent intent = new Intent();
				intent.setClass(mContext, DebugActivity.class);
				intent.putExtra("ServerIP", ip);
				startActivity(intent);
			}
			
		});

		mAdvancedBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Log.v(TAG, "advanced");
				Intent intent = new Intent("android.settings.USER_SETTINGS");
				
				startActivity(intent);
			}
			
		});		

    }

}
