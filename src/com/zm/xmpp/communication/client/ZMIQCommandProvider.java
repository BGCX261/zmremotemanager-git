package com.zm.xmpp.communication.client;

import com.zm.epad.core.LogManager;
import com.zm.xmpp.communication.command.ICommand;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

public class ZMIQCommandProvider implements IQProvider {

    private String TAG = "ZMIQCommandProvider";

    @Override
    public IQ parseIQ(XmlPullParser parser) throws Exception {
        // System.out.println("Enter IQ Provider");
        ZMIQCommand iq = new ZMIQCommand();
        boolean done = false;
        try {
            String type = parser.getAttributeValue(null, "type");
            ICommand command = ClientCommandUtil.createCommand(type);
            String namespace = parser.getNamespace();
            command.setDirection(namespace);
            iq.setXmlns(namespace);
            LogManager.local(TAG, "receive command type:" + type
                    + " namespace:" + iq.getXmlns());

            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    ClientCommandUtil.parseCommand(parser.getName(),
                            parser.nextText(), command);
                } else if (eventType == XmlPullParser.END_TAG) {
                    done = true;
                }
            }
            iq.setCommand(command);
            LogManager.local(TAG, command.toString());

        } catch (Exception e) {
            e.printStackTrace();
            iq = null;
        }

        return iq;
    }

}
