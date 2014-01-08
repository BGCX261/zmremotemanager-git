package com.android.remotemanager.plugins;


import java.util.ArrayList;
import com.android.remotemanager.NetworkStatusMonitor;
import com.android.remotemanager.plugins.xmpp.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.XmlPullParser;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.VoicemailContract;
import android.provider.ContactsContract.Contacts.Data;
import android.util.Log;

public class XmppClient implements NetworkStatusMonitor.NetworkStatusReport{
    
    public static int XMPPCLIENT_EVENT_CONNECTION_BEFORE_CREATED = 0;
    public static int XMPPCLIENT_EVENT_CONNECT = 1;
    public static int XMPPCLIENT_EVENT_LOGIN   = 2;
    public static int XMPPCLIENT_EVENT_CONNECTION_UPDATE_STATUS = 3;
    public static int XMPPCLIENT_EVENT_LOGOUT = 4;
    public static int XMPPCLIENT_EVENT_SENDPACKET_RESULT = 5;
    
    static  int CMD_START = 0;
    static  int CMD_CONNECTION_STATUS_UPDATE = 1;
    static  int CMD_NETWORK_STATUS_UPDATE =2;
    static  int CMD_LOGIN = 3;
    static  int CMD_LOGOUT = 4;
    static  int CMD_QUIT = 5;
    static  int CMD_SEND_PACKET_ASYNC = 6;
    
    public interface XmppClientCallback{
        public Object reportXMPPClientEvent(int xmppClientEvent, Object...args);
    }
    static private String TAG ="XmppClient";

    static private SmackAndroid  mSmackAndroid = null;
  
    @Override
    public void reportNetworkStatus(boolean bConnected) {
        Message msg = mXmppClientHandler.obtainMessage(CMD_NETWORK_STATUS_UPDATE);
        msg.arg1 = bConnected?1:0;
        mXmppClientHandler.sendMessage(msg);
    }
    
    
    static public void  initializeXMPPEnvironment(Context context){
        if(mSmackAndroid == null){
            mSmackAndroid = SmackAndroid.init(context);
        }
        return ;
    }
    static public void destroyXMPPEnvironment(){
        if(mSmackAndroid != null){
            mSmackAndroid.onDestroy();
            mSmackAndroid = null;
        }
        return;
    }
    
    private class XMPPConnectionListener implements ConnectionListener {

       @Override
        public void connectionClosed() {
           Message msg = mXmppClientHandler.obtainMessage(CMD_CONNECTION_STATUS_UPDATE);
           msg.arg1 = 0;
           mXmppClientHandler.sendMessage(msg);
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            Message msg = mXmppClientHandler.obtainMessage(CMD_CONNECTION_STATUS_UPDATE);
            msg.arg1 = 0;
            msg.obj = e;
            mXmppClientHandler.sendMessage(msg);
        }

        @Override
        public void reconnectingIn(int seconds) {
           
        }
        @Override
        public void reconnectionSuccessful() {
            Message msg = mXmppClientHandler.obtainMessage(CMD_CONNECTION_STATUS_UPDATE);
            msg.arg1 = 1;
            mXmppClientHandler.sendMessage(msg);
        }

        @Override
        public void reconnectionFailed(Exception e) {
        }
    }
    private class XmppClientThreadHandler extends Handler{
        public XmppClientThreadHandler(Looper looper){
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
             int cmd = msg.what;
             Log.d(TAG,"handleMessage cmd:"+cmd);
             if(cmd == CMD_START){
                 handleStartCmd();
             }else if(cmd == CMD_LOGIN){
                handleLoginCmd();
             }else if(cmd == CMD_LOGOUT){
                 handleLogoutCmd();
             }else if(cmd == CMD_SEND_PACKET_ASYNC){
                 try {
                     mXmppConnection.sendPacket((Packet)msg.obj);
                     mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_SENDPACKET_RESULT,true, msg.obj);
                } catch (Exception e) {
                    mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_SENDPACKET_RESULT,false,msg.obj);
                }
             }else if(cmd == CMD_CONNECTION_STATUS_UPDATE){
                 int connected = msg.arg1;
                 if(connected == 0){
                     mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_CONNECTION_UPDATE_STATUS,
                             0,msg.obj);
                 }else{
                     mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_CONNECTION_UPDATE_STATUS,1);
                 }
                 return;
             }else if (cmd == CMD_NETWORK_STATUS_UPDATE){
                 handleNetworkAvailable(msg.arg1);
             }
             
             return;
        }
    }
    private void handleNetworkAvailable(int networkAvailable){
        if(networkAvailable == 0){
            Log.e(TAG, "handleNetworkAvailable : network is down, need logout");
            handleLogoutCmd();
        } else if(networkAvailable == 1){
            String  prevStatus = myBundle.getString("status");
            Log.e(TAG, "handleNetworkAvailable : network is up");
            if(prevStatus.equals("idle")){
                Log.e(TAG, "\t prevstatus = " + prevStatus + ": do nothing");
                return;
            }else if(prevStatus.equals("starting") || prevStatus.equals("started")){
                Log.e(TAG, "\t prevstatus = " + prevStatus + ": restart");
                handleStartCmd();
            }else if(prevStatus.equals("logining") || prevStatus.equals("logined")){
                Log.e(TAG, "\t prevstatus = " + prevStatus + ": re-login");
                handleStartCmd();
                handleLoginCmd();
            }
        }
        return ;
    }
    private void handleStartCmd(){
        try {
            Log.d(TAG, "handle CMD_START");
            String serverName = myBundle.getString("server");
            Log.d(TAG, "connect to server:" + serverName);
            mXmppConnection = new XMPPConnection(serverName);
            mXmppConnection.connect();
            
            mXmppConnectionListener = new XMPPConnectionListener();
            mXmppConnection.addConnectionListener(mXmppConnectionListener);

            mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_CONNECT, 1,
                    mXmppConnection,ProviderManager.getInstance());
            myBundle.putString("status", "started");
            
       } catch (Exception e) {
           Log.e(TAG,"handleStartCmd: "+e.toString());
           mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_CONNECT, 0);
           myBundle.putString("status", "idle");
           myBundle.putString("exception", "connected failed: " + e.getMessage());
       }
    }
    private void handleLoginCmd(){
        Log.d(TAG, "handle CMD_LOGIN");
        String currentStatus = myBundle.getString("status");
        if(!currentStatus.equals("started")) {
            Log.e(TAG, "xmppclient failed to login with current status = " + currentStatus);
            return ;
        }

        try {
            String usrName = myBundle.getString("username");
            String usrPwd = myBundle.getString("password");
            String usrResource = myBundle.getString("resource");
            mXmppConnection.login(usrName, usrPwd, usrResource);
            mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_LOGIN,true);
            myBundle.putString("status","logined");
       } catch (Exception e) {
           mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_LOGIN,false);
           myBundle.putString("status", "started");
           myBundle.putString("exception", "login failed: " + e.getMessage());
       }
    }
    private void handleLogoutCmd(){
        mXmppConnection.disconnect();
        mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_LOGOUT,true);
        myBundle.putString("status", "idle");
    }
    
    
    private XmppClientThreadHandler mXmppClientHandler = null;
    private HandlerThread  mXmppHandlerThread = null;
    private Context mContext;
    private XmppClientCallback mXmppClientCallback;
    private ConnectionConfiguration  mConnectionConfiguration = null;
    private Connection mXmppConnection = null;
    private Bundle myBundle = null;
    private XMPPConnectionListener mXmppConnectionListener = null;
    
    public XmppClient(Context context, XmppClientCallback xmppClientCallback){
        mContext = context;
        mXmppClientCallback = xmppClientCallback;
        myBundle = new Bundle();
        myBundle.putString("status", "idle");
    }
    
    public boolean start(String serverName){
        if(mXmppClientHandler != null){
            Log.e(TAG, "already started");
            return true;
        }
        if(serverName == null){
            Log.e(TAG, "FAILED: start without serverName");
            return false;
        }
        Log.e(TAG, "xmppclient start with servername :" + serverName);
        
        myBundle.putString("status", "starting"); //set status to starting
        myBundle.putString("server", serverName);
        mXmppHandlerThread = new HandlerThread(TAG);
        mXmppHandlerThread.start();
        mXmppClientHandler = new XmppClientThreadHandler(mXmppHandlerThread.getLooper());
        mXmppClientHandler.sendEmptyMessage(CMD_START);
        
        return true;
    }
    public void stop(){
        if(mXmppClientHandler == null)
            return;
        logout();
        synchronized (mXmppHandlerThread) {
            try {
                mXmppHandlerThread.quit();
                mXmppHandlerThread.join();
            } catch (Exception e) {
                // TODO: handle exception
            }
            
            
        }
        mXmppHandlerThread = null;
        mXmppClientHandler = null;
        mXmppConnection = null;
        mConnectionConfiguration = null;
        
    }
    
    public boolean login(String usrName, String password, String resource){
        if(mXmppClientHandler == null){
            
            return false;
        }
        if(usrName == null || password == null){
            Log.e(TAG, "xmppclient login failed,either username or password is null");
            return false;
        }
        
        Log.e(TAG, "xmppclient login username " + usrName + " password " + password + " resource " + resource);
        myBundle.putString("username", usrName);
        myBundle.putString("password", password);
        myBundle.putString("resource", resource==null?"zhimotech":resource);
        myBundle.putString("status", "logining");
        Message msg = mXmppClientHandler.obtainMessage(CMD_LOGIN);
        msg.setData(myBundle);
        mXmppClientHandler.sendMessage(msg);
        
        return true;
    }
    public void logout(){
        if(mXmppClientHandler==null)
            return;
        mXmppClientHandler.sendEmptyMessage(CMD_LOGOUT);
        return;
    }
    
    
    public boolean sendPacket(Packet packet){
        if(mXmppConnection == null)
            return false;
        try {
            mXmppConnection.sendPacket(packet);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public boolean sendPacketAsync(Packet packet){
        if(mXmppConnection == null)
            return false;
        Message msg = mXmppClientHandler.obtainMessage(CMD_SEND_PACKET_ASYNC, packet);
        mXmppClientHandler.sendMessage(msg);
        return true;
    }
    
    
    
  
}
