package com.zm.xmpp.communication.client;

import com.zm.xmpp.communication.result.IResult;


public interface IClientResultPackager {
	
	public void parseResult(String paraName,String value,IResult c);


	public void packResult(org.jivesoftware.smack.packet.IQ p,IResult c);
	
	public IResult createResult();
}
