package com.zm.xmpp.communication.server;

import org.xmpp.packet.IQ;

import com.zm.xmpp.communication.result.IResult;


public interface IResultPackager {
	
	public void parseResult(IQ p,IResult c);

	public void packResult(IQ p,IResult c);
	
	public IResult createResult();
}
