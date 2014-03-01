package com.zm.xmpp.communication.client;

import com.zm.xmpp.communication.result.IResult;

import org.jivesoftware.smack.packet.IQ;

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
        this();
        if (requestIQ != null){
            setFrom(requestIQ.getTo());
            setTo(requestIQ.getFrom());
        }
    }

    public ZMIQResult() {
        super();
    }
}
