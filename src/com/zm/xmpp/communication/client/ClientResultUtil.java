package com.zm.xmpp.communication.client;

import java.util.HashMap;

import org.jivesoftware.smack.packet.IQ;

import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.result.IResult;
import com.zm.xmpp.communication.result.ResultNormal;

public class ClientResultUtil {
	private static HashMap packagers = new HashMap();
	private static boolean initialed = false;

	public static void init() {
		packagers.put(ClientResultPackagerNormal.name, new ClientResultPackagerNormal());
		initialed = true;
	}

	public static void addPackager(String name, IClientResultPackager p) {
		packagers.put(name, p);
	}

	public static void parseResult(String paraName, String value,
			IResult r) {
		if (!initialed)
			init();
		((IClientResultPackager) packagers.get(r.getType())).parseResult(paraName, value, r);
	}

	public static IResult createResult(String type){
		if (!initialed)
			init();		
		return ((IClientResultPackager)packagers.get(type)).createResult();
//		return ResultPackagerNormal.createResult();
	}
	
	public static IQ resultOK(org.jivesoftware.smack.packet.IQ iq){
		System.out.println("enter ResultUtil resultOK");
		if(iq instanceof ZMIQCommand){
			ICommand command=((ZMIQCommand)iq).getCommand();
			ZMIQResult rq=new ZMIQResult();
			rq.setFrom(iq.getTo());
			rq.setTo(iq.getFrom());
			rq.setType(IQ.Type.RESULT);
			ResultNormal result=new ResultNormal(command.getId(),Constants.RESULT_OK);
			result.setDirection(SmackClient.send);
			rq.setResult(result);
			
			
			return rq;
		}else{
			//TODO need throw exception
			return null;
		}
	}
}
