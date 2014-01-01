package com.android.remotemanager.plugins;
import java.util.ArrayList;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.provider.*;
import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.util.Log;

public class XmppClient {
    static private String TAG ="XmppClient";
    
    private final String XMPP_NAMESPACE = "com.zm.epad.xmpp";
    private final String XMPP_RMDEVICE = "remotedevice";
    private final String XMPP_RMPACKAGE= "remotepackage";
    
    static private XmppClient mXmppClient = null;
    
    private SmackAndroid  mSmackAndroid = null;
    private PacketListener  mSmackPktListener = null;
    private ConnectionListener  mSmackConnectionListener = null;
    private IQProvider mIQProvider = null;  
    
    
    
    static public XmppClient getXmppClientInstance(Context context){
        if(mXmppClient == null)
            mXmppClient = new XmppClient(context);
        return mXmppClient;
    }
    Connection mXmppConnection = null;
    private XmppClient(Context context){
        Log.e(TAG,"XmppClient initilize SmackAndroid ");
        mSmackAndroid = SmackAndroid.init(context);
        mIQProvider = new XmppIQProvider();
        
        ProviderManager.getInstance().addIQProvider(XMPP_RMDEVICE, XMPP_NAMESPACE,mIQProvider);
        ProviderManager.getInstance().addIQProvider(XMPP_RMPACKAGE, XMPP_NAMESPACE,mIQProvider);
        
    }
    class XmppIQProvider implements IQProvider{
        @Override
        public IQ parseIQ(XmlPullParser parser){
            String elementName = parser.getName();
            String namespace = parser.getNamespace();
            Log.e(TAG, "element: " + elementName + " namespace: " + namespace);
            if(namespace.equals(XMPP_NAMESPACE) == false){
                return null;
            }
            if(elementName.equals(XMPP_RMDEVICE)){
                RemoteDeviceIQ remoteDeviceIQ = new RemoteDeviceIQ();
                
                if(remoteDeviceIQ.parse(parser) == false)
                    return null;
                
                return remoteDeviceIQ;
            }else if (elementName.equals(XMPP_RMPACKAGE)){
                RemotePackageIQ remotePackageIQ = new RemotePackageIQ();
                
                if(remotePackageIQ.parse(parser) == false)
                    return null;
                
                return remotePackageIQ;
                
            }
            else
                return null;
         }
    }
    
    class RemoteDeviceIQ extends IQ{

        @Override
        public String getChildElementXML() {
            // TODO Auto-generated method stub
            return null;
        }
        public boolean parse(XmlPullParser xmlPullParser){
            boolean done = false;
            return done;
        }
        public Packet buildResultPacket(){
            return null;
        }
    }
    class RemotePackageIQ extends IQ{
        
        private final String[] CMD_STR_ARRAYS = {
            "update","enable","disable","uninstall","install"};
        
        static public final int CMD_INT_UPDATE = 0;
        static public final int CMD_INT_ENABLE = 1;
        static public final int CMD_INT_DISABLE = 2;
        static public final int CMD_INT_UNINSTALL = 3;
        static public final int CMD_INT_INSTALL = 4;
        
        
        private int mCmdType = -1;
        private int mCmdArgsCount = 0;
        
        private int mStatus = 0; //0 means unhandled, 1 means handled
        private boolean mResult = false;
        private ArrayList<String> mCmdArgs = new ArrayList<String>();
        
        public int getCmdType(){
            return mCmdType;
        }
        private void setCmdType(String cmdString){
            mCmdType = -1;
            for (String strCmd:CMD_STR_ARRAYS){
                mCmdType++;
                if(strCmd.equals(cmdString)){
                    break;
                }
            }
            
            return;
        }
        
        private void setCmdArgs(String cmdArgs){
            String[] args = cmdArgs.split(";");
            for(String arg:args)
                mCmdArgs.add(arg);
            return;
        }
        
        public final ArrayList<String> getCmdArgs(){
            return mCmdArgs;
        }
        
        /*
        <remotepackage xmlns="com.zm.epad.xmpp">
            <cmdtype>install</cmdtype>
            <cmdargs>com.android.email;com.android.browser</cmdargs>
            <result>false;true<result>   //only for result packet
        </remotepackage>
        */
        @Override
        public String getChildElementXML() {
            // TODO Auto-generated method stub
            if(mStatus == 0) // unhandled, return null
                return null;
            else if(mStatus == 1){
                StringBuilder buf = new StringBuilder();
                buf.append("<" + XMPP_RMPACKAGE);
                if (getXmlns() != null) {
                    buf.append(" xmlns=\"").append(getXmlns()).append("\"");
                }
                buf.append(">");
                
                buf.append("<cmdtype>");
                buf.append(CMD_STR_ARRAYS[mCmdType]);
                buf.append("</cmdtype>");
                
                buf.append("<cmdargs>");
                for(int arg = 0; arg<mCmdArgs.size(); arg++){
                    buf.append(mCmdArgs.get(arg));
                    buf.append(";");
                }
                buf.append("</cmdargs>");
                
                buf.append("<result>");
                buf.append(mResult?"1":"0");
                buf.append("</result>");
                
                buf.append("/" + XMPP_RMPACKAGE + ">");
                return buf.toString();
            }
            else
                return null;
        }
        public boolean parse(XmlPullParser parser){
            boolean done = false;
            try{
                while (!done) {
                    int eventType = parser.next();
                    if (eventType == XmlPullParser.START_TAG) {
                        if (parser.getName().equals("cmdtype")) {
                            setCmdType(parser.nextText());
                        }
                        //cmdargs="arg1;arg2;arg3"
                        else if (parser.getName().equals("cmdargs")) {
                            setCmdArgs(parser.nextText());
                        }
                    }
                    else if (eventType == XmlPullParser.END_TAG) {
                             done = true;
                    }
                }
            }catch (Exception e) {
                Log.e(TAG,"RemotePackageIQ parse fail: " + e.getMessage());
                return false;
            }
           
            return done;
        }
        
        public Packet buildResultPacket(boolean bResult){
            mResult = bResult;
            mStatus = 1;
            return this;
        }
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
            mSmackConnectionListener = new XmppConnectionListener();
            mXmppConnection.addConnectionListener(mSmackConnectionListener);
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
    
    class XmppConnectionListener implements ConnectionListener{

        @Override
        public void connectionClosed() {
            // TODO Auto-generated method stub
            Log.e(TAG, "connection closed");
            
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            // TODO Auto-generated method stub
            Log.e(TAG, "connection closed on error");
            
            
        }

        @Override
        public void reconnectingIn(int seconds) {
            // TODO Auto-generated method stub
            Log.e(TAG, "reconnectingIn " + seconds + " seconds");
            
        }

        @Override
        public void reconnectionSuccessful() {
            // TODO Auto-generated method stub
            Log.e(TAG, "reconnectionSuccessful");
        }

        @Override
        public void reconnectionFailed(Exception e) {
            // TODO Auto-generated method stub
            Log.e(TAG, "reconnectionFailed " + e.getMessage());
        }
        
    }
    class XmppPacketListener implements PacketListener{

        @Override
        public void processPacket(Packet packet) {
            Log.e(TAG, "received a xmpp packet " + packet.toXML());
/*            
            if(packet.getXmlns().contains(XMPP_NAMESPACE) == false)
                return;
            */
            if(packet instanceof RemotePackageIQ){
                RemotePackageIQ remotePackageIQ = (RemotePackageIQ)packet; 
                handleIQCmd(remotePackageIQ);
                
            }else if(packet instanceof RemoteDeviceIQ){
                RemoteDeviceIQ remoteDeviceIQ = (RemoteDeviceIQ)packet;
                handleIQCmd(remoteDeviceIQ);
                
            }
            
        }
        
    }
    
    
    private void handleIQCmd(RemotePackageIQ remotePackageIQ){
        int cmdType = remotePackageIQ.getCmdType();
        ArrayList<String> cmdArgs = remotePackageIQ.getCmdArgs();
        switch (cmdType) {
        case RemotePackageIQ.CMD_INT_ENABLE:{
              String pkgName = cmdArgs.get(0);
              Log.e(TAG, "enable pkg: " + pkgName);
              boolean bResult = mRPM.enablePkg(pkgName);
              Packet resultPacket = remotePackageIQ.buildResultPacket(bResult);
              mXmppConnection.sendPacket(resultPacket);
              Log.e(TAG, "Result:" + resultPacket.toXML());
            }
            break;
        case RemotePackageIQ.CMD_INT_DISABLE:{
            String pkgName = cmdArgs.get(0);
            Log.e(TAG, "disable pkg: " + pkgName);
            boolean bResult = mRPM.disablePkg(pkgName);
            Packet resultPacket = remotePackageIQ.buildResultPacket(bResult);
            mXmppConnection.sendPacket(resultPacket);
            Log.e(TAG, "Result:" + resultPacket.toXML());
            
            }
            break;
        default:
            break;
        }
        return;
    }
    
    private void handleIQCmd(RemoteDeviceIQ remoteDeviceIQ){
        return;
    }
    
    
    
    
    /*
     * 
     * */
    
    
    RemotePkgsManager mRPM = null;
    public void start(RemotePkgsManager rpm){
        if(mXmppConnection == null)
            return;
        mRPM = rpm;
        try {
            mSmackPktListener = new XmppPacketListener();
            mXmppConnection.addPacketListener(mSmackPktListener, null/*new PacketFilter() {
                
                @Override
                public boolean accept(Packet packet) {
                    Log.e(TAG, "accept packet?:" + packet.toXML());
                    if(packet.getXmlns().contains(XMPP_NAMESPACE))
                        return true;
                    return false;
                }
            }*/);
        } catch (Exception e) {
            // TODO: handle exception
            Log.e(TAG, e.getMessage());
        }
        return;
    }
    
    
}
