package com.zm.xmpp.communication.client;

import org.jivesoftware.smack.packet.IQ;

import com.zm.xmpp.communication.result.IResult;

public class ZMIQResult extends IQ {

	private IResult result;

	public IResult getResult() {
		return result;
	}

	public void setResult(IResult result) {
		this.result = result;
	}

	@Override
	public String getChildElementXML() {
        if (result != null)
            return result.toXML();
        else
            return null;
	}
	
    public ZMIQResult(IQ requestIQ) {
        setFrom(requestIQ.getTo());
        setTo(requestIQ.getFrom());
    }

    public ZMIQResult() {
        super();
    }
}
