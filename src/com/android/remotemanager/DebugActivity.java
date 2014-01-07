package com.android.remotemanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;

import com.android.remotemanager.plugins.RemotePkgsManager;
import com.android.remotemanager.plugins.XmppClient;
import com.android.remotemanager.plugins.xmpp.*;


import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.os.Bundle;
import android.os.Debug;
import com.android.logmanager.LogManager;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class DebugActivity extends ListActivity {

	public static final String TAG = "DebugActivity";
    List<Map<String, String>> mTestFunctions = new ArrayList<Map<String, String>>();
    private static String TEST_XMPP = "test xmpp";
    private static String TEST_DEVICE_ADMIN = "test device admin";
    private String ip = null;
    private Context mContext = null; 
	
    LogManager mLogManager = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        
        ip = getIntent().getExtras().getString("ServerIP");
        Log.v(TAG,ip);

        connectLogManager(ip);
        mContext = this;
        
        Map<String, String> test1 = new HashMap<String, String>();
        test1.put("title", TEST_XMPP);
        test1.put("value", TEST_XMPP);
        mTestFunctions.add(test1);
        

        
        Map<String, String> test2 = new HashMap<String, String>();
        test2.put("title", TEST_DEVICE_ADMIN);
        test2.put("value", TEST_DEVICE_ADMIN);
        mTestFunctions.add(test2);
        
        setListAdapter(new SimpleAdapter(this, mTestFunctions,
                android.R.layout.simple_list_item_1, new String[] { "title" },
                new int[] { android.R.id.text1 }));
        getListView().setTextFilterEnabled(true); 
        
        testXMPP();
        
    }
    

    private void connectLogManager(String ip)
    {
    	try{
	        mLogManager = LogManager.getLogManagerInstance(ip, this);
	        mLogManager.start();
    	}catch(Exception e)
    	{
    		Log.e(TAG, "LogManager"+e.toString());
    	}
          	
    }
    
    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Map<String, String> map = (Map<String, String>)l.getItemAtPosition(position);
        String clickedItem  = map.get("value");
        if(clickedItem.equals(TEST_XMPP)){
            //testXMPP();
    		Intent intent = new Intent();
    		intent.setClass(mContext, DebugActivitySender.class);
    		intent.putExtra("ServerIP", ip);
    		startActivity(intent);
        }else if(clickedItem.equals(TEST_DEVICE_ADMIN)){
            android.content.Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.android.remotemanager", 
                    "com.android.remotemanager.plugins.remotedeviceadmin.RemoteDeviceAdmin"));
            startActivity(intent);
        }
        
    }
    void testXMPP(){
        
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.remotemanager", 
                "com.android.remotemanager.RemoteManagerService"));
        
        Bundle args = new Bundle();
        args.putString("server", ip);
        args.putString("username", "dengfanping");
        args.putString("password","123");
        
        intent.putExtras(args);
        
        try {
            if( startService(intent) != null)
                Log.e(TAG, "start service succeed");
            else
                Log.e(TAG, "start service failed");
        } catch (Exception e) {
            Log.e(TAG, "start service failed " + e.getMessage());
        }
       
        Thread testThread = new Thread(new Runnable(){

            @Override
            public void run() {
                try {
                    Thread.sleep(5000); //wait 5seconds to RemoteManagerService to get ready!
                    //testXmppClient();
                } catch (Exception e) {
                    // TODO: handle exception
                }
                
                
            }
            
        });
        testThread.start();

    }
    private void testXmppClient(){
        try {
            XMPPConnection testConnection= new XMPPConnection("192.168.0.100", null);
            testConnection.connect();
            testConnection.login("test", "test");
            Thread.sleep(5000);
            /*RemotePackageIQ cmdIQ = new RemotePackageIQ();
            cmdIQ.setTo("dengfanping@com.zm.openfire/Smack");
            cmdIQ.setFrom("test@com.zm.openfire/Smack");
            cmdIQ.setPacketID("xyzzd");
            cmdIQ.setCmdType("enable");
            cmdIQ.setCmdArgs("com.android.browser");
            LogManager.e("XmppClient", "test send msg " +cmdIQ.toString());
            testConnection.sendPacket(cmdIQ);*/
        } catch (Exception e) {
            LogManager.e("XmppClient", "testXmppClient " + e.getMessage());
        }
        
        
    }
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();       

        
    }

   
    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
        
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
    }

}
