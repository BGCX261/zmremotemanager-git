package com.zm.xmpp.communication.result;

import com.zm.xmpp.communication.Constants;


public class ResultNormal extends AbstractResult implements IResult{
	
	private final static String type="normal";
	
	public ResultNormal(){
		
	}

	public ResultNormal(String id,String status){
		this.id=id;
		this.status=status;
		this.errorcode="0";
	}
	
	public ResultNormal(String id,String status,String errorcode){
		this.id=id;
		this.status=status;
		this.errorcode=errorcode;
	}	
	
	public String getType(){
		return type;
	}

	@Override
	public String toXML() {
		StringBuffer buf=new StringBuffer();
		buf.append("<result xmlns=\"");
		buf.append(this.direction);
		buf.append("\" type=\"normal\">");
		buf.append("<id>");
		buf.append(this.id);
		buf.append("</id>");
		buf.append("<status>");
		buf.append(this.status);
		buf.append("</status>");
		buf.append("<errorcode>");
		buf.append(this.errorcode);
		buf.append("</errorcode>");
		
		buf.append("</result>");
		return buf.toString();
	}

	@Override
	public String toString(){
		StringBuffer buf=new StringBuffer();
		buf.append("Normal Result:[");
		buf.append("id=");
		buf.append(this.id);
		buf.append("/status=");
		buf.append(this.status);
		buf.append("/errorcode=");
		buf.append(this.errorcode);
		buf.append("]");		
		return buf.toString();
		
	}



	
}
