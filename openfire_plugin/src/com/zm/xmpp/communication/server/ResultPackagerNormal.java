package com.zm.xmpp.communication.server;

import org.dom4j.Element;
import org.xmpp.packet.IQ;

import com.zm.epad.structure.Application;
import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.command.Command4App;
import com.zm.xmpp.communication.result.IResult;
import com.zm.xmpp.communication.result.ResultNormal;


public class ResultPackagerNormal implements IResultPackager{
	public final static String name="normal";
	
	@Override
	public void parseResult(IQ iq,IResult c){
		ResultNormal ac=(ResultNormal)c;
		
		ac.setId(iq.getChildElement().element("id").getText());
		ac.setStatus(iq.getChildElement().element("status").getText());
		ac.setDirection(iq.getChildElement().getNamespaceURI());
		ac.setIssuetime(iq.getChildElement().element("issuetime").getText());
		ac.setErrorcode(iq.getChildElement().element("errorcode").getText());
	}

	@Override
	public void packResult(IQ iq, IResult c) {
		Element e=iq.setChildElement(Constants.XMPP_RESULT,c.getDirection());
		e.addAttribute("type", c.getType());
		e.addElement("id").addText(c.getId());
		e.addElement("status").addText(c.getStatus());
		e.addElement("errorcode").addText(c.getErrorcode());
		e.addElement("issuetime").addText(c.getIssuetime());
		
	}
	
	public IResult createResult(){
		return new ResultNormal();
	}

}
