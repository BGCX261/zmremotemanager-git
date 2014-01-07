package com.zm.xmpp.communication.server;

import java.util.HashMap;

import org.dom4j.Element;
import org.xmpp.packet.IQ;

import com.zm.epad.structure.Application;
import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.command.Command4App;
import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.result.IResult;

public class ResultUtil {

	private static HashMap packagers = new HashMap();
	private static boolean initialed = false;

	public static void init() {
		packagers.put(ResultPackagerNormal.name, new ResultPackagerNormal());
		initialed = true;
	}

	public static void addPackager(String name, IResultPackager p) {
		packagers.put(name, p);
	}

	public static void parseResult(IQ iq,
			IResult r) {
		if (!initialed)
			init();
		((IResultPackager) packagers.get(r.getType())).parseResult(iq, r);
	}

	public static IQ resultOK(IQ iq) {
		IQ rq=IQ.createResultIQ(iq);
		
		ICommand c=CommandUtil.parseCommand(iq);
		
		Element e=rq.setChildElement(Constants.XMPP_RESULT, Constants.XMPP_NAMESPACE_CENTER);
		e.addAttribute("type", "normal");
		e.addElement("id").addText(c.getId());
		e.addElement("issuetime").addText("current time");
		e.addElement("status").addText(Constants.RESULT_OK);
		e.addElement("errorcode").addText(Constants.ERRORCODE_SUCCESS);
		
		return rq;

	}

}
