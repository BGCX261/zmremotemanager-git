package com.android.remotemanager;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;

import com.android.remotemanager.plugins.RemotePkgsManager;
import com.android.remotemanager.plugins.XmppClient;
import com.android.remotemanager.plugins.xmpp.*;

import android.app.Activity;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;

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
                
                testXmppClient();
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
            RemotePackageIQ cmdIQ = new RemotePackageIQ();
            cmdIQ.setTo("dengfanping@com.zm.openfire/Smack");
            cmdIQ.setFrom("test@com.zm.openfire/Smack");
            cmdIQ.setPacketID("xyzzd");
            cmdIQ.setCmdType("enable");
            cmdIQ.setCmdArgs("com.android.browser");
            Log.e("XmppClient", "test send msg " +cmdIQ.toString());
            testConnection.sendPacket(cmdIQ);
        } catch (Exception e) {
            Log.e("XmppClient", "testXmppClient " + e.getMessage());
        }
        
        
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
