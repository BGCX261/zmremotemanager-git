package com.zm.xmpp.communication.client;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.zm.xmpp.communication.command.Command4Query;
import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.command.ICommand4Query;

public class ClientCommandPackager4Query implements IClientCommandPackager{
	public final static String name="query";
	
	public void parseCommand(String paraName,String value,ICommand c){
				
		if(c instanceof ICommand4Query)
		{
			((ICommand4Query) c).toCommand(paraName, value);
		}
		return;
	}


	public void packCommand(org.jivesoftware.smack.packet.IQ p,ICommand c){
		((ZMIQCommand)p).setCommand(c);
	}
	
	public ICommand createCommand(){
		return new Command4Query();
	}

}
