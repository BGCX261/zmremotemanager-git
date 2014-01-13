package com.zm.xmpp.communication.command;

import com.zm.epad.structure.Application;
import com.zm.xmpp.communication.Constants;

public class Command4Query extends AbstractCommand implements ICommand{

	private final static String type="query";

	
	public Command4Query(){
		
	}
	
	public Command4Query(String direction,String id,String action,String time){
		this.direction=direction;
		this.id=id;
		this.action=action;
		this.issueTime=time;
	}


	public String getType(){
		return type;
	}
	
	public String toString(){
		
		StringBuffer buf=new StringBuffer();
		buf.append("Query Command:[");
		buf.append("id=");
		buf.append(this.id);
		buf.append("/action=");
		buf.append(this.action);
		buf.append("/issuetime=");
		buf.append(this.issueTime);		
		buf.append("]");
		return buf.toString();
		
	}
	
	public String toXML(){
		StringBuffer buf=new StringBuffer();
		buf.append("<command xmlns=\"");
		buf.append(this.direction);
		buf.append("\" type=\"");
		buf.append(type);		
		buf.append("\">");
		buf.append("<id>");
		buf.append(this.id);
		buf.append("</id>");
		buf.append("<action>");
		buf.append(this.action);
		buf.append("</action>");
		buf.append("<issuetime>");
		buf.append(this.issueTime);
		buf.append("</issuetime>");
		buf.append("</command>");
		return buf.toString();
	}

}
