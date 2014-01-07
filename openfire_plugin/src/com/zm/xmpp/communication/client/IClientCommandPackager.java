package com.zm.xmpp.communication.client;

import com.zm.xmpp.communication.command.ICommand;


public interface IClientCommandPackager {
	
	public void parseCommand(String paraName,String value,ICommand c);

	public void packCommand(org.jivesoftware.smack.packet.IQ p,ICommand c);
	
	public ICommand createCommand();
}
