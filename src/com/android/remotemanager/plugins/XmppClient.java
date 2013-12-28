package com.android.remotemanager.plugins;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import android.content.Context;
import android.util.Log;

public class XmppClient {
    static private String TAG ="XmppClient";
    
    static private XmppClient mXmppClient = null;
    
    private SmackAndroid  mSmackAndroid = null;
    static public XmppClient getXmppClientInstance(Context context){
        if(mXmppClient == null)
            mXmppClient = new XmppClient(context);
        return mXmppClient;
    }
    Connection mXmppConnection = null;
    private XmppClient(Context context){
        Log.e(TAG,"XmppClient initilize SmackAndroid ");
        mSmackAndroid = SmackAndroid.init(context);
        
    }
    
    public void destroy(){
        if(mXmppConnection != null)
            logout();
        mSmackAndroid.onDestroy();
    }
    public boolean connectToserver(String serverName){
        try {
            Log.e(TAG,"connectToserver " + serverName);
            Connection.DEBUG_ENABLED = true;
            mXmppConnection = new XMPPConnection(serverName, null);
            mXmppConnection.connect();
            return true;
        } catch (XMPPException e) {
            Log.e(TAG, e.getMessage());
            mXmppConnection = null;
            return false;
        }
    }
    
    public boolean logout(){
        if(mXmppConnection == null)
            return true;
            
        try {
                Log.e(TAG,"logout");
                mXmppConnection.disconnect();
                mXmppConnection = null;
            } catch (Exception e) {
                Log.e(TAG,"logout " + e.getMessage());
                return false;
        }
        return true;
    }
    public boolean loginToServer(String username, String password){
        if(mXmppConnection == null)
            return false;
        boolean bConnected = mXmppConnection.isConnected();
        try {
            if(bConnected == false){
                mXmppConnection.connect();
                bConnected = true;
            }
            Log.e(TAG,"login as " + username + " password " + password);
            mXmppConnection.login(username, password);
            Log.e(TAG,"login successed");
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            if(bConnected == false){
                //connection is not establised. release XMPPConnection/
                mXmppConnection = null;
            }
            return false;
        }
       
    }
    
    
}
