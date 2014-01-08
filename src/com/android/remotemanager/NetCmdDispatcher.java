package com.android.remotemanager;

import android.util.Log;

import com.android.remotemanager.plugins.XmppClient;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.ProviderManager;
import org.xmlpull.v1.XmlPullParser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;

public class NetCmdDispatcher implements XmppClient.XmppClientCallback {
	public static final String TAG="NetCmdDispatcher";
    
    public static class CmdDispatchInfo{
        public String mStrElementName;
        public String mStrNameSpace;
        public IQ parseXMLStream(XmlPullParser parser){
            return null;
        }
        public boolean handlePacket(Packet packet){
            return false;
        }
        public String  getKey(){
            return mStrNameSpace /*+ ":" + mStrElementName*/; //can not use element
        }
    }
    
    private HashMap<String, CmdDispatchInfo> mCmdDispacherHashMap = null;
    private Connection mXmppConnection = null;
    private NetCmdHandler mCmdHandler = null;
    
    private class NetCmdHandler implements PacketListener, PacketFilter,IQProvider{
        
        @Override
        public IQ parseIQ(XmlPullParser parser){
            
            String namespace = parser.getNamespace();
            Log.v(TAG, "parseIQ: "+namespace);
            
            CmdDispatchInfo cmdDispatchInfo = mCmdDispacherHashMap.get(namespace);
            if(cmdDispatchInfo == null) return null;
            
            return cmdDispatchInfo.parseXMLStream(parser);
        }
        @Override
        public boolean accept(Packet packet) {
            String namespace = packet.getXmlns();
            Log.v(TAG, "accept: "+namespace);
            return mCmdDispacherHashMap.containsKey(namespace);
        }

        @Override
        public void processPacket(Packet packet) {
           
            String key = packet.getXmlns();
            Log.v(TAG, "processPacket: "+key);
            
            CmdDispatchInfo cmdDispatchInfo = mCmdDispacherHashMap.get(key);
            if(cmdDispatchInfo == null)
                return;
            cmdDispatchInfo.handlePacket(packet);
        }
    }
         
    public NetCmdDispatcher(){
        mCmdDispacherHashMap = new HashMap<String, CmdDispatchInfo>(10);
        mCmdHandler = new NetCmdHandler();
    }
    public void registerDispatcher(CmdDispatchInfo infos[]){
        for(CmdDispatchInfo info:infos){
            mCmdDispacherHashMap.put(info.getKey(),info);
        }
    }
    public void registerDispacher(CmdDispatchInfo info){
        mCmdDispacherHashMap.put(info.getKey(),info);
    }
    
    
    @Override
    public Object reportXMPPClientEvent(int xmppClientEvent, Object... args) {
         if(xmppClientEvent == XmppClient.XMPPCLIENT_EVENT_CONNECT){
            if(args.length > 1){
                int connSuc = ((Integer)args[0]).intValue();
                Log.v(TAG, "reportXMPPClientEvent: "+connSuc);
                
                if(connSuc == 1){
                    mXmppConnection = (Connection)args[1];
                    mXmppConnection.addPacketListener(mCmdHandler, mCmdHandler);
                    
                    ProviderManager prdManager = (ProviderManager)args[2];
                    Collection<CmdDispatchInfo> collection = mCmdDispacherHashMap.values();
                    for (Iterator iterator = collection.iterator(); iterator
                            .hasNext();) {
                        CmdDispatchInfo info = (CmdDispatchInfo) iterator.next();
                        prdManager.addIQProvider(info.mStrElementName, info.mStrNameSpace, mCmdHandler);
                        Log.v(TAG, "addIQProvider: "+info.mStrElementName+", "+info.mStrNameSpace);
                    }
                   
                }else if(connSuc == 0)
                    mXmppConnection = null;
            }
        }
        return null;
    }
    
}
