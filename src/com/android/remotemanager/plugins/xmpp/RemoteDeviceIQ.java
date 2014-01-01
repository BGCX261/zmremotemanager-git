package com.android.remotemanager.plugins.xmpp;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.xmlpull.v1.XmlPullParser;

public class RemoteDeviceIQ extends IQ{

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