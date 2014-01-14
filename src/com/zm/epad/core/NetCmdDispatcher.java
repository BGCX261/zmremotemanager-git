package com.zm.epad.core;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.xmlpull.v1.XmlPullParser;

import com.zm.epad.core.NetCmdDispatcher.CmdDispatchInfo;

import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class NetCmdDispatcher implements XmppClient.XmppClientCallback {
    public static final String TAG = "NetCmdDispatcher";

    public static class CmdDispatchInfo {
        public String mStrElementName;
        public String mStrNameSpace;

        public IQ parseXMLStream(XmlPullParser parser) {
            return null;
        }

        public boolean handlePacket(Packet packet) {
            return false;
        }

        public String getKey() {
            return mStrNameSpace /* + ":" + mStrElementName */; // can not use
                                                                // element
        }
        
        public void destroy()
        {
        	return;
        }
    }

    private HashMap<String, CmdDispatchInfo> mCmdDispacherHashMap = null;
    private Connection mXmppConnection = null;
    private NetCmdHandler mCmdHandler = null;

    private class NetCmdHandler implements PacketListener, PacketFilter,
            IQProvider {

        @Override
        public IQ parseIQ(XmlPullParser parser) {

            String namespace = parser.getNamespace();
            Log.v(TAG, "parseIQ: " + namespace);

            CmdDispatchInfo cmdDispatchInfo = null;
            synchronized (mCmdDispacherHashMap) {
                cmdDispatchInfo = mCmdDispacherHashMap.get(namespace);
                if (cmdDispatchInfo == null)
                    return null;
            }

            return cmdDispatchInfo.parseXMLStream(parser);
        }

        @Override
        public boolean accept(Packet packet) {
            String namespace = packet.getXmlns();

            Log.v(TAG, "accept: " + namespace);
            Log.v(TAG, "pakcet info " + packet.toXML());

            if (namespace == null)
                return false;
            synchronized (mCmdDispacherHashMap) {
                return mCmdDispacherHashMap.containsKey(namespace);
            }

        }

        @Override
        public void processPacket(Packet packet) {

            String key = packet.getXmlns();
            Log.v(TAG, "processPacket: " + key);
            Log.v(TAG, "processPacket pakcet info " + packet.toXML());
            if (key == null) {
                return;
            }
            CmdDispatchInfo cmdDispatchInfo = null;
            synchronized (mCmdDispacherHashMap) {
                cmdDispatchInfo = mCmdDispacherHashMap.get(key);
                if (cmdDispatchInfo == null)
                    return;
            }
            cmdDispatchInfo.handlePacket(packet);
        }
    }

    public NetCmdDispatcher() {
        mCmdDispacherHashMap = new HashMap<String, CmdDispatchInfo>(10);
        mCmdHandler = new NetCmdHandler();
    }

    public void registerDispatcher(CmdDispatchInfo infos[]) {
        synchronized (mCmdDispacherHashMap) {
            for (CmdDispatchInfo info : infos) {
                mCmdDispacherHashMap.put(info.getKey(), info);
            }
        }

    }

    public void registerDispacher(CmdDispatchInfo info) {
        synchronized (mCmdDispacherHashMap) {
            mCmdDispacherHashMap.put(info.getKey(), info);
        }

    }

    public void start() {
        // do nothing. just have a start function.
    }
    public void stop() {
        synchronized (mCmdDispacherHashMap) {
        	Collection<CmdDispatchInfo> collection = mCmdDispacherHashMap.values();
        	for (Iterator<CmdDispatchInfo> i = collection.iterator(); i.hasNext();){
        		CmdDispatchInfo info = (CmdDispatchInfo)i.next();
        		info.destroy();
        	}
            mCmdDispacherHashMap.clear();
        }
    }

    @Override
    public Object reportXMPPClientEvent(int xmppClientEvent, Object... args) {
        if (xmppClientEvent == XmppClient.XMPPCLIENT_EVENT_CONNECT) {
            if (args.length > 1) {
                int connSuc = ((Integer) args[0]).intValue();
                Log.v(TAG, "reportXMPPClientEvent connect to server : "
                        + connSuc);

                if (connSuc == 1) {
                    mXmppConnection = (Connection) args[1];
                    mXmppConnection.addPacketListener(mCmdHandler, mCmdHandler);

                    ProviderManager prdManager = (ProviderManager) args[2];
                    Collection<CmdDispatchInfo> collection = mCmdDispacherHashMap
                            .values();
                    for (Iterator iterator = collection.iterator(); iterator
                            .hasNext();) {
                        CmdDispatchInfo info = (CmdDispatchInfo) iterator
                                .next();
                        prdManager.addIQProvider(info.mStrElementName,
                                info.mStrNameSpace, mCmdHandler);
                        Log.v(TAG, "addIQProvider: " + info.mStrElementName
                                + ", " + info.mStrNameSpace);
                    }

                } else if (connSuc == 0)
                    mXmppConnection = null;
            }
        }
        return null;
    }

}
