package com.zm.epad.plugins;

import java.util.ArrayList;

import com.zm.epad.core.XmppClient;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.xmlpull.v1.XmlPullParser;

import android.util.Log;

public class RemotePackageIQ extends IQ{
    static private final String TAG ="XmppClient-RemotePackageIQ";
    
    static private final String[] CMD_STR_ARRAYS = {
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
    public void setCmdType(String cmdString){
        mCmdType = -1;
        for (String strCmd:CMD_STR_ARRAYS){
            mCmdType++;
            if(strCmd.equals(cmdString)){
                break;
            }
        }
        
        return;
    }
    
    public void setCmdArgs(String cmdArgs){
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
        
        StringBuilder buf = new StringBuilder();
        //buf.append("<" + XmppClient.XMPP_RMPACKAGE);
       // buf.append(" xmlns=\"").append(XmppClient.XMPP_NAMESPACE).append("\"");
        
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
        
        if(mStatus == 1){
            buf.append("<result>");
            buf.append(mResult?"1":"0");
            buf.append("</result>");
        }
       // buf.append("</" + XmppClient.XMPP_RMPACKAGE + ">");
        Log.e(TAG, "created xml = " + buf.toString());
        return buf.toString();

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
