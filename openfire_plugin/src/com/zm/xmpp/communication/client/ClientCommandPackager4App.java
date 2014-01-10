package com.zm.xmpp.communication.client;

import java.util.Collection;
import java.util.Iterator;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.DefaultPacketExtension;

import com.zm.epad.structure.Application;
import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.command.Command4App;
import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.command.ICommand4App;

public class ClientCommandPackager4App implements IClientCommandPackager{
	public final static String name="app";
	
	public void parseCommand(String paraName,String value,ICommand c){

		if(c instanceof ICommand4App)
		{
			((ICommand4App) c).configCommand(paraName, value);
		}
		return;
	}


	public void packCommand(org.jivesoftware.smack.packet.IQ p,ICommand c){
		((ZMIQCommand)p).setCommand(c);
	}
	
	public ICommand createCommand(){
		return new Command4App();
	}

}
