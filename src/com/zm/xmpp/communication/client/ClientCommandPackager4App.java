package com.zm.xmpp.communication.client;

import com.zm.xmpp.communication.command.Command4App;
import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.command.ICommand4App;

public class ClientCommandPackager4App implements IClientCommandPackager {
    public final static String name = "app";

    public void parseCommand(String paraName, String value, ICommand c) {

        if (c instanceof ICommand4App) {
            ((ICommand4App) c).toCommand(paraName, value);
        }
        return;
    }

    public void packCommand(org.jivesoftware.smack.packet.IQ p, ICommand c) {
        ((ZMIQCommand) p).setCommand(c);
    }

    public ICommand createCommand() {
        return new Command4App();
    }

}
