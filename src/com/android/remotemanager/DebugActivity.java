package com.android.remotemanager;

import com.android.remotemanager.plugins.RemotePkgsManager;
import com.android.remotemanager.plugins.XmppClient;

import android.app.Activity;
import android.os.Bundle;
import android.os.Debug;

public class DebugActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        Thread testThread = new Thread(new Runnable(){

            @Override
            public void run() {
                // TODO Auto-generated method stub
                XmppClient xmppClient = XmppClient.getXmppClientInstance(getApplicationContext());
                xmppClient.connectToserver("192.168.0.100"); //"2011-20120430WG"
                xmppClient.loginToServer("dengfanping", "123");
                xmppClient.start(new RemotePkgsManager(getApplicationContext()));
            }
            
        });
        testThread.start();
        
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        Thread testThread = new Thread(new Runnable(){

            @Override
            public void run() {
                // TODO Auto-generated method stub
                XmppClient xmppClient = XmppClient.getXmppClientInstance(getApplicationContext());
                xmppClient.logout();
                xmppClient.destroy();
            }
            
        });
        testThread.start();
        super.onStop();
    }

}
