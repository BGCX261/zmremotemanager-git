package com.zm.xmpp.communication.client;

import org.jivesoftware.smack.packet.IQ;

import com.zm.xmpp.communication.command.ICommand;

public class ZMIQCommand extends IQ {
	static private final String TAG = "XMPP-Client IQ Command";
	private ICommand command;

	public ICommand getCommand() {
		return command;
	}

	public void setCommand(ICommand command) {
		this.command = command;
	}

	@Override
	public String getChildElementXML() {
		return command.toXML();
	}

}
