package com.zm.xmpp.communication.result;

import com.zm.epad.structure.Device;

public class ResultDevice extends AbstractResult implements IResult {

    private final static String type = "device";
    private Device device = new Device();

    public ResultDevice() {

    }

    public ResultDevice(String id, String status) {
        this.id = id;
        this.status = status;
        this.errorCode = "0";
    }

    public ResultDevice(String id, String status, String errorcode) {
        this.id = id;
        this.status = status;
        this.errorCode = errorcode;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toXML() {
        StringBuffer buf = new StringBuffer();
        toXMLBase(buf, type);
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
        buf.append("<manuf>");
        buf.append(this.device.getManufacturer());
        buf.append("</manuf>");
        buf.append("<brand>");
        buf.append(this.device.getBrand());
        buf.append("</brand>");
        buf.append("<model>");
        buf.append(this.device.getModel());
        buf.append("</model>");
        buf.append("<os>");
        buf.append(this.device.getOSVersion());
        buf.append("</os>");
        buf.append("<battery>");
        buf.append(this.device.getBattery());
        buf.append("</battery>");
        buf.append("<elapsed>");
        buf.append(this.device.getElapsedTime());
        buf.append("</elapsed>");
        buf.append("</result>");
        return buf.toString();
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
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
        buf.append("/manufacturer=");
        buf.append(this.device.getManufacturer());
        buf.append("/brand=");
        buf.append(this.device.getBrand());
        buf.append("/model=");
        buf.append(this.device.getModel());
        buf.append("/os=");
        buf.append(this.device.getOSVersion());
        buf.append("/battery=");
        buf.append(this.device.getBattery());
        buf.append("/elapsed");
        buf.append(this.device.getElapsedTime());
        buf.append("]");
        return buf.toString();

    }

}
