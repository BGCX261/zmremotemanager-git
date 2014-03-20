package com.zm.xmpp.communication.result;

import java.util.List;
import java.util.ArrayList;

public class ResultAppUsage extends AbstractResult implements IResult {

    private final static String type = "usagereport";
    private List<User> UserList = new ArrayList<User>() {
    };
    private long start = 0;
    private long end = 0;

    public class AppUsage {
        public String appname = "";
        public String pkgname = "";
        public String version = "";
        long elapsed = 0;
    }

    public class User {
        public User(int id) {
            userId = id;
        }

        int userId;
        public List<AppUsage> appUsageList = new ArrayList<AppUsage>() {
        };
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

    public void addUser(int userId) {
        UserList.add(new User(userId));
    }

    public void addAppUsage(int userId, String appname, String pkgname,
            String version, long elapsed) {
        for (User u : UserList) {
            if (u.userId == userId) {
                AppUsage au = new AppUsage();
                au.appname = appname;
                au.pkgname = pkgname;
                au.elapsed = elapsed;
                au.version = version;
                u.appUsageList.add(au);
            }
        }
    }

    public List<User> getUserList() {
        return UserList;
    }

    public List<AppUsage> getAppUsageList(int userId) {
        List<AppUsage> list = null;
        for (User u : UserList) {
            if (u.userId == userId) {
                list = u.appUsageList;
            }
        }
        return list;
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
        toXMLBase(buf, type);
        buf.append("<start>");
        buf.append(this.start);
        buf.append("</start>");
        buf.append("<end>");
        buf.append(this.end);
        buf.append("</end>");
        for (User u : UserList) {
            buf.append("<env id=\"" + u.userId + "\">");
            for (AppUsage au : u.appUsageList) {
                buf.append("<app>");
                buf.append("<appname>");
                buf.append(au.appname);
                buf.append("</appname>");
                buf.append("<pkgname>");
                buf.append(au.pkgname);
                buf.append("</pkgname>");
                buf.append("<version>");
                buf.append(au.version);
                buf.append("</version>");
                buf.append("<elapsed>");
                buf.append(au.elapsed);
                buf.append("</elapsed>");
                buf.append("</app>");
            }
            buf.append("</env>");
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
        for (User u : UserList) {
            buf.append("/env=");
            buf.append(u.userId);
            for (AppUsage au : u.appUsageList) {
                buf.append("/app appname=");
                buf.append(au.appname);
                buf.append(" pkgname=");
                buf.append(au.pkgname);
                buf.append(" elapsed=");
                buf.append(au.elapsed);
            }
        }

        buf.append("]");
        return buf.toString();

    }

}
