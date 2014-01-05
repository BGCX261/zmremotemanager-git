package com.android.remotemanager.plugins;
import java.security.PublicKey;
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
             if(cmd == CMD_START){
                 if(mConnectionConfiguration == null){
                     mConnectionConfiguration = (ConnectionConfiguration)
                        mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_CONNECTION_BEFORE_CREATED);
                     if(mConnectionConfiguration == null){
                         mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_CONNECT, false);
                         return;
                     }
                 }
                 
                 try {
                     mXmppConnection = new XMPPConnection(mConnectionConfiguration);
                     mXmppConnectionListener = new XMPPConnectionListener();
                     mXmppConnection.addConnectionListener(mXmppConnectionListener);
                     mXmppConnection.connect();
                     mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_CONNECT, true,
                             mXmppConnection,ProviderManager.getInstance());
                } catch (Exception e) {
                    mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_CONNECT, false);
                }
                 return;
             }else if(cmd == CMD_LOGIN){
                 if(mXmppConnection == null){
                     mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_LOGIN,false);
                     return;
                 }
                 try {
                     if(mXmppConnection.isConnected() == false){
                         mXmppConnection.connect();
                     }
                     Bundle data = msg.getData();
                     String usrName = data.getString("username");
                     String usrPwd = data.getString("password");
                     String usrResource = data.getString("resource");
                     
                     mXmppConnection.login(usrName, usrPwd, usrResource);
                     mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_LOGIN,true);
                     data.putString("status","login");
                } catch (Exception e) {
                    mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_LOGIN,false);
                }
                return;
             }else if(cmd == CMD_LOGOUT){
                 try {
                     mXmppConnection.disconnect();
                     mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_LOGOUT,true);
                } catch (Exception e) {
                    mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_LOGOUT,false);
                }
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
                 int connected = msg.arg1;
                 if(connected == 0){
                     mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_CONNECTION_UPDATE_STATUS,0);
                     String currentStatus = myBundle.getString("status");
                     if(currentStatus.equals("login")){
                         try {
                            mXmppConnection.disconnect();
                        } catch (Exception e) {
                            // TODO: handle exception
                        }
                     }
                 }else {
                     String currentStatus = myBundle.getString("status");
                     if(currentStatus.equals("login")){
                         try {
                             mXmppConnection.connect();
                             mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_CONNECT, true);
                             String usrName = myBundle.getString("username");
                             String usrPwd = myBundle.getString("password");
                             String usrResource = myBundle.getString("resource");
                             mXmppConnection.login(usrName, usrPwd, usrResource);
                             mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_LOGIN,true);
                        } catch (Exception e) {
                            if(mXmppConnection.isConnected()){
                                mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_LOGIN,false);
                            }else{
                                mXmppClientCallback.reportXMPPClientEvent(XMPPCLIENT_EVENT_CONNECT,false);
                            }
                        }
                     }
               }
             }
        }
    }
    
    private XmppClientThreadHandler mXmppClientHandler = null;
    private HandlerThread  mXmppHandlerThread = null;
    private Context mContext;
    private XmppClientCallback mXmppClientCallback;
    private ConnectionConfiguration  mConnectionConfiguration = null;
    private Connection mXmppConnection = null;
    private Bundle myBundle = null;
    private XMPPConnectionListener mXmppConnectionListener = null;
    
    public void XmppClient(Context context, XmppClientCallback xmppClientCallback){
        mContext = context;
        mXmppClientCallback = xmppClientCallback;
    }
    
    public void start(){
        if(mXmppClientHandler != null)
            return;
        myBundle = new Bundle();
        myBundle.putString("status", "start");
        mXmppHandlerThread = new HandlerThread(TAG);
        mXmppHandlerThread.start();
        mXmppClientHandler = new XmppClientThreadHandler(mXmppHandlerThread.getLooper());
        mXmppClientHandler.sendEmptyMessage(CMD_START);
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
    
    public void login(String usrName, String password, String resource){
        if(mXmppClientHandler == null)
            return;
         
        String currentStatus = myBundle.getString("status");
        
        if(currentStatus.equals("login")) //if already logined, just return back directly
            return;
       
        myBundle.putString("username", usrName);
        myBundle.putString("password", password);
        myBundle.putString("resource", resource==null?"zhimotech":resource);
        Message msg = mXmppClientHandler.obtainMessage(CMD_LOGIN);
        msg.setData(myBundle);
        mXmppClientHandler.sendMessage(msg);
        return;
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
