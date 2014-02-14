package com.zm.xmpp.communication.command;

import com.zm.epad.structure.Application;
import com.zm.xmpp.communication.Constants;

public class Command4Query extends AbstractCommand implements ICommand4Query{

	private final static String type="query";

	protected String url = null;
	
	public Command4Query(){
		
	}
	
	public Command4Query(String direction,String id,String action,String time){
		this.direction=direction;
		this.id=id;
		this.action=action;
		this.issueTime=time;
	}

	public Command4Query(String direction,String id,String action,String time,String url){
	    this(direction, id, action, time);
	    this.url = url;
	}

	public String getType(){
		return type;
	}
	
    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
	
    public String toString() {

        StringBuffer buf = new StringBuffer();
        buf.append("Query Command:[");
        buf.append("id=");
        buf.append(this.id);
        buf.append("/action=");
        buf.append(this.action);
        buf.append("/issuetime=");
        buf.append(this.issueTime);
        buf.append("/url=");
        buf.append(this.url);
        buf.append("]");
        return buf.toString();

    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();
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
        buf.append("<url>");
        buf.append(this.url);
        buf.append("</url>");
        buf.append("</command>");
        return buf.toString();
    }

	@Override
	public void toCommand(String paraName, String value) {
		// TODO Auto-generated method stub
		if(paraName.equals("action")){
			this.setAction(value);
		}else if(paraName.equals("id")){
			this.setId(value);
		}else if(paraName.equals("issuetime")){
			this.setIssueTime(value);
		}else if(paraName.equals("url")){
		    this.setUrl(value);
		}
	}

}
