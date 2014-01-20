package com.zm.epad.structure;

import java.util.Collection;

public class Device {

	private String userid;
	private String deviceid;
	private String wifi;//	Á¬½ÓµÄÈÈµãÃû³Æ£¬Èç¹ûÎ´Á¬½Ó½«Îª¿Õ
	private String bt;//	BluetoothµÄ×´Ì¬
	private String nfc;//	NFCµÄ×´Ì¬
	private String ip;//	IPµØÖ·£¬ÈçºÎÎ´Á¬Íø½«Îª¿Õ
	private String gps;//>	gpsÇé±¨
	private String amode;//	ÊÇ·ñÎªº½¿ÕÄ£Ê½
	private String mnet;//	Á¬½ÓµÄÒÆ¶¯ÍøÂçÃû³Æ(3g»ò4g)£¬Ã»ÓÐ½«Îª¿Õ

	
	private Collection<Environment> env;

	public String getDeviceid() {
		return deviceid;
	}
	public void setDeviceid(String deviceid) {
		this.deviceid = deviceid;
	}

	public String getUserid() {
		return userid;
	}
	public void setUserid(String userid) {
		this.userid = userid;
	}
	public Collection<Environment> getEnv() {
		return env;
	}
	public void setEnv(Collection<Environment> env) {
		this.env = env;
	}
	public String getWifi() {
		return wifi;
	}
	public void setWifi(String wifi) {
		this.wifi = wifi;
	}
	public String getBt() {
		return bt;
	}
	public void setBt(String bt) {
		this.bt = bt;
	}
	public String getNfc() {
		return nfc;
	}
	public void setNfc(String nfc) {
		this.nfc = nfc;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getGps() {
		return gps;
	}
	public void setGps(String gps) {
		this.gps = gps;
	}
	public String getAmode() {
		return amode;
	}
	public void setAmode(String amode) {
		this.amode = amode;
	}
	public String getMnet() {
		return mnet;
	}
	public void setMnet(String mnet) {
		this.mnet = mnet;
	}

	
	
}
