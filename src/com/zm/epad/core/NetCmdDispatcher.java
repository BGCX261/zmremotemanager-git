package com.zm.epad.core;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.xmlpull.v1.XmlPullParser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

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

        public void handleError(IQ iq) {
            return;
        }

        public void destroy() {
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
            LogManager.local(TAG, "parseIQ: " + namespace);

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

            LogManager.local(TAG, "accept: " + namespace);
            LogManager.local(TAG, "pakcet info " + packet.toXML());

            if (packet instanceof IQ
                    && ((IQ) packet).getType().toString().equals("error")) {
                LogManager.local(TAG, "notify the error IQ");
                Set<String> keys = mCmdDispacherHashMap.keySet();
                for(String k : keys) {
                    mCmdDispacherHashMap.get(k).handleError((IQ)packet);
                }
            }

            if (namespace == null)
                return false;
            synchronized (mCmdDispacherHashMap) {
                return mCmdDispacherHashMap.containsKey(namespace);
            }
        }

        @Override
        public void processPacket(Packet packet) {

            String key = packet.getXmlns();
            LogManager.local(TAG, "processPacket: " + key);
            LogManager
                    .local(TAG, "processPacket pakcet info " + packet.toXML());
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
            Collection<CmdDispatchInfo> collection = mCmdDispacherHashMap
                    .values();
            for (Iterator<CmdDispatchInfo> i = collection.iterator(); i
                    .hasNext();) {
                CmdDispatchInfo info = (CmdDispatchInfo) i.next();
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
                LogManager.local(TAG,
                        "reportXMPPClientEvent connect to server : " + connSuc);

                if (connSuc == 1) {
                    mXmppConnection = (Connection) args[1];

                    // add packet Listener
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
                        LogManager.local(TAG, "addIQProvider: "
                                + info.mStrElementName + ", "
                                + info.mStrNameSpace);
                    }

                } else if (connSuc == 0)
                    mXmppConnection = null;
            }
        }
        return null;
    }

}
