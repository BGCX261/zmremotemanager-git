package com.zm.xmpp.communication.result;

import java.util.Vector;

public class ResultRunningApp extends AbstractResult implements IResult {
    public final static String PROCESS_FOREGROUND = "foreground";
    public final static String PROCESS_VISIBLE = "visible";
    public final static String PROCESS_SERVICE = "service";
    public final static String PROCESS_BACKGROUND = "background";
    public final static String PROCESS_EMPTY = "empty";
    public final static String PROCESS_UNKNOWN = "unknown";

    private final static String type = "appreport";

    private int mCurrentEnv = 0;
    private Vector<ProcessInfo> mProcessList = new Vector<ProcessInfo>();
    private String mMoment;

    public String getType() {
        return type;
    }

    public ResultRunningApp() {

    }

    public ResultRunningApp(int EnvId, String time) {
        mCurrentEnv = EnvId;
        mMoment = time;
    }

    public int getEnv() {
        return mCurrentEnv;
    }

    public String getTime() {
        return mMoment;
    }

    public int getProcessCount() {
        return mProcessList.size();
    }

    public String getProcessName(int order) {
        return mProcessList.get(order).getProcessName();
    }

    public String getProcessImportance(int order) {
        return mProcessList.get(order).getImportance();
    }

    public String getProcessLabel(int order) {
        return mProcessList.get(order).getLabel();
    }
    
    public String getProcessAppVersion(int order) {
        return mProcessList.get(order).getVersion();
    }

    public Vector<ProcessInfo> getProcessList() {
        return mProcessList;
    }

    public void addProcess(String processName, String label, String importance,
            String version) {
        ProcessInfo process = new ProcessInfo(processName, label, importance,
                version);
        this.mProcessList.add(process);
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
        buf.append("<issuetime>");
        buf.append(this.issueTime);
        buf.append("</issuetime>");
        buf.append("<envid>");
        buf.append(this.mCurrentEnv);
        buf.append("</envid>");

        for (int j = 0; j < mProcessList.size(); j++) {
            ProcessInfo process = (ProcessInfo) mProcessList.get(j);
            buf.append("<process>");
            buf.append("<name>");
            buf.append(process.getProcessName());
            buf.append("</name>");
            buf.append("<importance>");
            buf.append(process.getImportance());
            buf.append("</importance>");
            buf.append("<display>");
            buf.append(process.getLabel());
            buf.append("</display>");
            buf.append("<version>");
            buf.append(process.getVersion());
            buf.append("</version>");
            buf.append("</process>");
        }
        buf.append("<time>");
        buf.append(this.mMoment);
        buf.append("</time>");
        buf.append("</result>");
        return buf.toString();
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(type);
        buf.append(" Result:[");
        buf.append("\r\n");
        buf.append("id=");
        buf.append(this.id);
        buf.append("/deviceid=");
        buf.append(this.deviceId);
        buf.append("/issuetime=");
        buf.append(this.issueTime);
        buf.append("/envid=");
        buf.append(this.mCurrentEnv);

        for (int j = 0; j < mProcessList.size(); j++) {
            ProcessInfo process = (ProcessInfo) mProcessList.get(j);
            buf.append("\r\n");

            buf.append("/process name=");
            buf.append(process.getProcessName());
            buf.append(" importance=");
            buf.append(process.getImportance());
            buf.append(" display=");
            buf.append(process.getLabel());
            buf.append(" version=");
            buf.append(process.getVersion());
        }
        buf.append("\r\n");
        buf.append("/time");
        buf.append(this.mMoment);
        buf.append("\r\n]");
        return buf.toString();
    }

    public Object toResult(String paraName, String value) {
        Object ret = null;

        if (paraName.equals("id")) {
            this.setId(value);
        } else if (paraName.equals("deviceid")) {
            this.setDeviceId(value);
        } else if (paraName.equals("issuetime")) {
            this.setIssueTime(value);
        } else if (paraName.equals("envid")) {
            this.mCurrentEnv = Integer.valueOf(value);
        } else if (paraName.equals("time")) {
            this.mMoment = value;
        } else if (paraName.equals("process")) {
            ProcessInfo process = new ProcessInfo();
            this.mProcessList.add(process);
            ret = (Object) process;
        }

        return ret;
    }

    public void toResult_Process(Object process, String paraName, String value) {
        if (process instanceof ProcessInfo) {
            if (paraName.equals("name")) {
                ((ProcessInfo) process).setProcessName(value);
            } else if (paraName.equals("importance")) {
                ((ProcessInfo) process).setImportance(value);
            } else if (paraName.equals("display")) {
                ((ProcessInfo) process).setLabel(value);
            } else if (paraName.equals("version")) {
                ((ProcessInfo) process).setVersion(value);
            }
        }

        return;
    }

    public static class ProcessInfo {
        private String mName = null;
        private String mLabel = null;
        private String mImportance = PROCESS_UNKNOWN;
        private String mVersion = null;

        public ProcessInfo() {

        }

        public ProcessInfo(String processName, String label, String importance,
                String version) {
            mName = processName;
            mLabel = label;
            mImportance = importance;
            mVersion = version;
        }

        public String getProcessName() {
            return mName;
        }

        public String getLabel() {
            return mLabel;
        }

        public String getImportance() {
            return mImportance;
        }
        
        public String getVersion() {
            return mVersion;
        }

        public void setProcessName(String name) {
            this.mName = name;
        }

        public void setLabel(String label) {
            this.mLabel = label;
        }

        public void setImportance(String importance) {
            this.mImportance = importance;
        }
        
        public void setVersion(String version) {
            this.mVersion = version;
        }
    }
}
