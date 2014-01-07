package com.zm.xmpp.communication.client;

import com.zm.epad.structure.Application;
import com.zm.xmpp.communication.command.Command4App;
import com.zm.xmpp.communication.command.ICommand;
import com.zm.xmpp.communication.result.IResult;
import com.zm.xmpp.communication.result.ResultNormal;

public class ClientResultPackagerNormal implements IClientResultPackager{
	public final static String name="normal";
	
	public void parseResult(String paraName,String value,IResult r){
				
		if(paraName.equals("id")){
			r.setId(value);
		}else if(paraName.equals("status")){
			r.setStatus(value);
		}else if(paraName.equals("errorcode")){
			r.setErrorcode(value);
		}else if(paraName.equals("issuetime")){
			r.setIssuetime(value);
		}
	}


	public void packResult(org.jivesoftware.smack.packet.IQ p,IResult r){
		((ZMIQResult)p).setResult(r);
	}
	
	public IResult createResult(){
		return new ResultNormal();
	}

}
