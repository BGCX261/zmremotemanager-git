package com.zm.xmpp.communication.result;

import com.zm.epad.structure.Device;


public class ResultDevice extends AbstractResult implements IResult{
	
	private final static String type="device";
	private Device device=new Device();
	
	public ResultDevice(){
		
	}

	public ResultDevice(String id,String status){
		this.id=id;
		this.status=status;
		this.errorCode="0";
	}
	
	public ResultDevice(String id,String status,String errorcode){
		this.id=id;
		this.status=status;
		this.errorCode=errorcode;
	}	
	
	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public String getType(){
		return type;
	}

	@Override
	public String toXML() {
		StringBuffer buf=new StringBuffer();
		buf.append("<result xmlns=\"");
		buf.append(this.direction);
		buf.append("\" type=\"");
		buf.append(type);		
		buf.append("\">");
		buf.append("<deviceid>");
		buf.append(this.deviceId);
		buf.append("</deviceid>");
		buf.append("<id>");
		buf.append(this.id);
		buf.append("</id>");
		buf.append("<status>");
		buf.append(this.status);
		buf.append("</status>");
		buf.append("<errorcode>");
		buf.append(this.errorCode);
		buf.append("</errorcode>");
		buf.append("<wifi>");
		buf.append(this.device.getWifi());
		buf.append("</wifi>");
		buf.append("<bt>");
		buf.append(this.device.getBt());
		buf.append("</bt>");		
		buf.append("<nfc>");
		buf.append(this.device.getNfc());
		buf.append("</nfc>");
		buf.append("<ip>");
		buf.append(this.device.getIp());
		buf.append("</ip>");
		buf.append("<gps>");
		buf.append(this.device.getGps());
		buf.append("</gps>");
		buf.append("<amode>");
		buf.append(this.device.getAmode());
		buf.append("</amode>");
		buf.append("<mnet>");
		buf.append(this.device.getMnet());		
		buf.append("</mnet>");
		buf.append("</result>");
		return buf.toString();
	}

	@Override
	public String toString(){
		StringBuffer buf=new StringBuffer();
		buf.append(type);
		buf.append(" Result:[");
		buf.append("id=");
		buf.append(this.id);
		buf.append("/status=");
		buf.append(this.status);
		buf.append("/errorcode=");
		buf.append(this.errorCode);
		buf.append("/deviceid=");
		buf.append(this.deviceId);	
		buf.append("/wifi=");
		buf.append(this.device.getWifi());
		buf.append("/bt=");
		buf.append(this.device.getBt());
		buf.append("/nfc=");
		buf.append(this.device.getNfc());
		buf.append("/ip=");
		buf.append(this.device.getIp());
		buf.append("/gps=");
		buf.append(this.device.getGps());
		buf.append("/amode=");
		buf.append(this.device.getAmode());
		buf.append("/mnet=");
		buf.append(this.device.getMnet());
		buf.append("]");		
		return buf.toString();
		
	}



	
}
