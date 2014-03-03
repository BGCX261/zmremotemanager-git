package com.zm.xmpp.communication.result;

import java.util.List;
import java.util.ArrayList;

public class ResultAppUsage extends AbstractResult implements IResult {

    private final static String type = "usagereport";
    private List<AppUsage> appUsageList = new ArrayList<AppUsage>() {
    };
    private long start = 0;
    private long end = 0;

    public class AppUsage {
        public String appname = "";
        public String pkgname = "";
        long elapsed = 0;
    }

    public ResultAppUsage() {

    }

    public ResultAppUsage(String id, long start, long end) {
        this.id = id;
        this.start = start;
        this.end = end;
    }

    public String getType() {
        return type;
    }

    public void addAppUsage(String appname, String pkgname, long elapsed) {
        AppUsage au = new AppUsage();
        au.appname = appname;
        au.pkgname = pkgname;
        au.elapsed = elapsed;
        appUsageList.add(au);
    }

    public List<AppUsage> getAppUsageList() {
        return appUsageList;
    }

    public long getStartTime() {
        return start;
    }

    public void setStartTime(long time) {
        this.start = time;
    }

    public long getEndTime() {
        return end;
    }

    public void setEndTime(long time) {
        this.end = time;
    }

    @Override
    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<result xmlns=\"");
        buf.append(this.direction);
        buf.append("\" type=\"");
        buf.append(type);
        buf.append("\">");
        buf.append("<id>");
        buf.append(this.id);
        buf.append("</id>");
        buf.append("<deviceid>");
        buf.append(this.deviceId);
        buf.append("</deviceid>");
        buf.append("<start>");
        buf.append(this.start);
        buf.append("</start>");
        buf.append("<end>");
        buf.append(this.end);
        buf.append("</end>");
        for (AppUsage au : appUsageList) {
            buf.append("<app>");
            buf.append("<appname>");
            buf.append(au.appname);
            buf.append("</appname>");
            buf.append("<pkgname>");
            buf.append(au.pkgname);
            buf.append("</pkgname>");
            buf.append("<elapsed>");
            buf.append(au.elapsed);
            buf.append("</elapsed>");
            buf.append("</app>");
        }

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
        buf.append("/deviceid=");
        buf.append(this.deviceId);
        buf.append("/start=");
        buf.append(this.start);
        buf.append("/end=");
        buf.append(this.end);
        for (AppUsage au : appUsageList) {
            buf.append("/app appname=");
            buf.append(au.appname);
            buf.append(" pkgname=");
            buf.append(au.pkgname);
            buf.append(" elapsed=");
            buf.append(au.elapsed);
        }

        buf.append("]");
        return buf.toString();

    }

}
