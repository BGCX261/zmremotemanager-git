package com.zm.xmpp.communication.client.test;

import org.jivesoftware.smack.packet.IQ;

import com.zm.xmpp.communication.client.ClientResultUtil;
import com.zm.xmpp.communication.client.IProcessor;
import com.zm.xmpp.communication.client.SmackClient;
import com.zm.xmpp.communication.client.ZMIQCommand;
import com.zm.xmpp.communication.client.ZMIQResult;

public class ReceiverCommandProcessor implements IProcessor {
	
	public void process(ZMIQCommand iq){
		IQ rq=ClientResultUtil.resultOK(iq);
		SmackClient.getConn().sendPacket(rq);
		System.out.println("reply:"+rq.toXML());
	}
	
	public void process(ZMIQResult iq){
		
	}

}
