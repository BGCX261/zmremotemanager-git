package com.zm.xmpp.communication.client;

import com.zm.xmpp.communication.command.Command4FileTransfer;
import com.zm.xmpp.communication.command.ICommand;

public class ClientCommandPackager4FileTransfer implements
        IClientCommandPackager {
    public final static String name = "push";

    public void parseCommand(String paraName, String value, ICommand c) {

        if (c instanceof Command4FileTransfer) {
            ((Command4FileTransfer) c).toCommand(paraName, value);
        }
        return;
    }

    public void packCommand(org.jivesoftware.smack.packet.IQ p, ICommand c) {
        ((ZMIQCommand) p).setCommand(c);
    }

    public ICommand createCommand() {
        return new Command4FileTransfer();
    }
}
