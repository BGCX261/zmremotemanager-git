package com.android.remotemanager;

import com.android.logmanager.LogManager;
import com.android.remotemanager.plugins.RemotePkgsManager;
import com.android.remotemanager.plugins.XmppClient;
import com.android.remotemanager.plugins.xmpp.RemotePackageIQ;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;

public class DebugActivitySender extends Activity {
	public static final String TAG = "DebugActivityHome";
	
	private Button mDisableBtn = null;
	private Button mEnableBtn = null;
	private EditText mNameText = null; 
	private EditText mUserIdText = null;
	private Context mContext = null;
	private XMPPConnection testConnection = null;
	private String IP = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
   	
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        
		setContentView(R.layout.activity_sender);
		mContext = this;
		
		IP = getIntent().getExtras().getString("ServerIP");

		testConnectionOn();
		mDisableBtn = (Button)findViewById(R.id.button1);
		mEnableBtn = (Button)findViewById(R.id.button2);
		
		mNameText = (EditText)findViewById(R.id.editText1);
		mUserIdText = (EditText)findViewById(R.id.editText2);

		
		mDisableBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				try{
					String name = mNameText.getText().toString();
					String userId = mUserIdText.getText().toString();
					Log.v(TAG, "Disable name: "+ name+", userId: "+ userId);
					
		            RemotePackageIQ cmdIQ = new RemotePackageIQ();
		            cmdIQ.setTo("dengfanping@com.zm.openfire/Smack");
		            cmdIQ.setFrom("test@com.zm.openfire/Smack");
		            cmdIQ.setPacketID("xyzzd");
		            cmdIQ.setCmdType("disable");
		            cmdIQ.setCmdArgs(name);
		            LogManager.e("XmppClient", "test send msg " +cmdIQ.toString());
		            testConnection.sendPacket(cmdIQ);
				}
				catch(Exception e)
				{
					Log.e(TAG, e.getMessage());
				}

			}
			
		});

		mEnableBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				try{
					String name = mNameText.getText().toString();
					String userId = mUserIdText.getText().toString();
					Log.v(TAG, "Enable name: "+ name+", userId: "+ userId);
					
		            RemotePackageIQ cmdIQ = new RemotePackageIQ();
		            cmdIQ.setTo("dengfanping@com.zm.openfire/Smack");
		            cmdIQ.setFrom("test@com.zm.openfire/Smack");
		            cmdIQ.setPacketID("xyzzd");
		            cmdIQ.setCmdType("enable");
		            cmdIQ.setCmdArgs(name);
		            LogManager.e("XmppClient", "test send msg " +cmdIQ.toString());
		            testConnection.sendPacket(cmdIQ);
				}
				catch(Exception e)
				{
					Log.e(TAG, e.getMessage());
				}
			}
			
		});		

    }
    
    void testConnectionOn(){

        Thread testThread = new Thread(new Runnable(){

           @Override
           public void run() {
               // TODO Auto-generated method stub

	       		try{
	    	        testConnection= new XMPPConnection(IP, null);
	    	        testConnection.connect();
	    	        testConnection.login("test", "test");
	    		}catch(Exception e){
	        		Log.e(TAG, "testConnection"+e.toString());
	        	}
               
           }
           
       });
       testThread.start();

   }
    
    @Override
    protected void onDestroy() {
    	testConnection.disconnect();
    	
    	super.onDestroy();
    }
}
