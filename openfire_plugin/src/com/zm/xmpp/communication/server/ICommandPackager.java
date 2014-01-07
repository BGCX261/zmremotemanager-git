package com.zm.xmpp.communication.server;

import org.xmpp.packet.IQ;

import com.zm.xmpp.communication.command.ICommand;


public interface ICommandPackager {
	
	public void parseCommand(IQ iq,ICommand c);

	public void packCommand(org.xmpp.packet.IQ p,ICommand c);
	
	public ICommand createCommand();
}
