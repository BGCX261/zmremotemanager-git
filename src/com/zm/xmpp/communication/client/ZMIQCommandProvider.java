package com.zm.xmpp.communication.client;

import com.zm.epad.core.LogManager;
import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.Constants;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

public class ZMIQCommandProvider implements IQProvider {

    private String TAG = "ZMIQCommandProvider";

    private final int CATEGORY_COMMON = 0;
    private final int CATEGORY_STRING = 1;

    private final String TAG_COMMAND = "command";
    private final String TAG_ID = "id";
    private final String TAG_ISSUE_TIME = "issuetime";

    @Override
    public IQ parseIQ(XmlPullParser parser) throws Exception {

        ZMIQCommand iq = new ZMIQCommand();

        try {
            String type = parser.getAttributeValue(null, "type");
            String namespace = parser.getNamespace();

            iq.setXmlns(namespace);
            LogManager.local(TAG, "receive command type:" + type
                    + " namespace:" + iq.getXmlns());

            ICommand command = null;
            switch (getIQCategory(type)) {
            case CATEGORY_COMMON:
                command = parseZMCommonIQ(parser, type);
                break;
            case CATEGORY_STRING:
                command = parseZMStringIQ(parser, namespace, type);
                break;
            default:
                throw new Exception("bad IQ command type:" + type);
            }

            command.setDirection(namespace);
            iq.setCommand(command);

        } catch (Exception e) {
            e.printStackTrace();
            iq = null;
        }

        return iq;
    }

    private int getIQCategory(String type) {
        int ret = CATEGORY_COMMON;
        if (type.equals(Constants.XMPP_COMMAND_POLICY)) {
            ret = CATEGORY_STRING;
        }

        return ret;
    }

    private ICommand parseZMCommonIQ(XmlPullParser parser, String type)
            throws Exception {
        ICommand command = ClientCommandUtil.createCommand(type);

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                ClientCommandUtil.parseCommand(parser.getName(),
                        parser.nextText(), command);
            } else if (eventType == XmlPullParser.END_TAG) {
                done = true;
            }
        }

        return command;
    }

    private ICommand parseZMStringIQ(XmlPullParser parser, String namespace,
            String type) throws Exception {
        ZMStringCommand command = new ZMStringCommand();
        boolean done = false;
        StringBuffer sb = new StringBuffer();
        try {
            // set command header
            sb.append("<command xmlns=\"" + namespace + "\" type=\"" + type
                    + "\">");
            while (!done) {
                // save all command body
                int eventType = parser.next();
                switch (eventType) {
                case XmlPullParser.START_DOCUMENT:
                    break;
                case XmlPullParser.END_DOCUMENT:
                    done = true;
                    break;
                case XmlPullParser.START_TAG:
                    sb.append("<");
                    sb.append(parser.getName());
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        sb.append(" ");
                        sb.append(parser.getAttributeName(i));
                        sb.append("=\"");
                        sb.append(parser.getAttributeValue(i));
                        sb.append("\"");
                    }
                    sb.append(">");
                    if (parser.getName().equals(TAG_ID)) {
                        String id = parser.nextText();
                        command.setId(id);
                        sb.append(id);
                        sb.append("</id>");
                    } else if (parser.getName().equals(TAG_ISSUE_TIME)) {
                        String issueTime = parser.nextText();
                        command.setIssueTime(issueTime);
                        sb.append(issueTime);
                        sb.append("</issuetime>");
                    }
                    break;
                case XmlPullParser.END_TAG:
                    sb.append("</");
                    sb.append(parser.getName());
                    sb.append(">");
                    if (parser.getName().equals(TAG_COMMAND)) {
                        done = true;
                    }
                    break;
                case XmlPullParser.TEXT:
                default:
                    sb.append(parser.getText());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            sb.setLength(0);
        }
        command.setType(type);
        command.setContent(sb.toString());
        return command;
    }
}
