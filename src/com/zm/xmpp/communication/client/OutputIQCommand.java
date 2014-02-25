package com.zm.xmpp.communication.client;

import org.jivesoftware.smack.packet.IQ;

public class OutputIQCommand extends IQ {
    static private final String TAG = "OutputCommand";
    protected String output = null;
    protected String commandType;
    protected String commandId;
    protected String issueTime;
    protected String direction;

    public String getOutput() {
        return output;
    }

    public void setOutput(String string) {
        output = string;
    }

    public void setXmlns(String xmlns) {
        this.xmlns = xmlns;
    }

    public String getCommandType() {
        return commandType;
    }

    public void setCommandType(String type) {
        this.commandType = type;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getId() {
        return commandId;
    }

    public void setId(String id) {
        this.commandId = id;
    }

    public String getIssueTime() {
        return issueTime;
    }

    public void setIssueTime(String issuetime) {
        this.issueTime = issuetime;
    }

    public String getChildElementXML() {
        return getOutput();
    }
}
