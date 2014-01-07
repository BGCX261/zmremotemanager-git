package com.zm.xmpp.communication.client;


public interface IProcessor {

	public void process(ZMIQCommand iq);
	
	public void process(ZMIQResult rq);
}
