package com.zm.xmpp.communication.client;

import com.zm.epad.core.LogManager;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

public class OutputIQCommandProvider implements IQProvider {

    private String TAG = "OutputCommandProvider";
    private final String TAG_COMMAND = "command";
    private final String TAG_ID = "id";
    private final String TAG_ISSUE_TIME = "issuetime";

    @Override
    public IQ parseIQ(XmlPullParser parser) throws Exception {
        OutputIQCommand iq = new OutputIQCommand();

        String type = parser.getAttributeValue(null, "type");
        iq.setCommandType(type);
        String namespace = parser.getNamespace();
        iq.setXmlns(namespace);
        LogManager.local(TAG, "receive command type:" + iq.getCommandType()
                + " namespace:" + iq.getXmlns());
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
                        iq.setId(id);
                        sb.append(id);
                        sb.append("</id>");
                    } else if (parser.getName().equals(TAG_ISSUE_TIME)) {
                        String issueTime = parser.nextText();
                        iq.setIssueTime(issueTime);
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

        iq.setOutput(sb.toString());
        return iq;
    }
}
