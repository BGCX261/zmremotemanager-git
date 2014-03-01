package com.zm.epad.structure;

import java.util.Collection;

public class Device {

	private String userid;
	private String deviceid;
	private String wifi;//wifi
	private String bt;//	Bluetooth
	private String nfc;//	NFC
	private String ip;//	IP
	private String gps;//	gps
	private String amode;//	airflight mode
	private String mnet;//3g,4g

	public String toString(){
	    StringBuilder sb = new StringBuilder();
	    sb.append("Device info: device-id" + deviceid +"\n");
	    sb.append("\t Wifi:" + wifi);
	    sb.append("\t BT:" + bt);
	    sb.append("\t NFC:" + nfc);
	    sb.append("\t IP:" + ip);
	    sb.append("\t GPS:" + gps);
	    sb.append("\t Airplane mode:" + amode);
	    sb.append("\t Mobile Network:" + mnet);
	    return sb.toString();
	}
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
