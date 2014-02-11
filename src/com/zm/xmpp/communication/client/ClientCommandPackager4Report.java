package com.zm.xmpp.communication.client;

import com.zm.xmpp.communication.command.Command4Report;
import com.zm.xmpp.communication.command.ICommand;

public class ClientCommandPackager4Report implements IClientCommandPackager {
    public final static String name = "Report";

    public void parseCommand(String paraName, String value, ICommand c) {

        if (c instanceof Command4Report) {
            ((Command4Report) c).toCommand(paraName, value);
        }
        return;
    }

    public void packCommand(org.jivesoftware.smack.packet.IQ p, ICommand c) {
        ((ZMIQCommand) p).setCommand(c);
    }

    public ICommand createCommand() {
        return new Command4Report();
    }
}
