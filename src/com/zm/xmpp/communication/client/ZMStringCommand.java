package com.zm.xmpp.communication.client;

import com.zm.xmpp.communication.command.AbstractCommand;
import com.zm.xmpp.communication.command.ICommand;

public class ZMStringCommand extends AbstractCommand implements ICommand {
    private String mType;
    private String mContent;

    public ZMStringCommand() {

    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        mType = type;
    }

    public void setContent(String string) {
        mContent = string;
    }

    public String toString() {
        return mContent;
    }

    public String toXML() {
        return mContent;
    }
}
