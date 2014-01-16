package com.zm.xmpp.communication.result;

import java.util.Vector;

import com.zm.epad.structure.Application;
import com.zm.epad.structure.Environment;

public class ResultApp extends AbstractResult implements IResult{
	
	private final static String type="app";
	private Vector<Environment> envs=new Vector<Environment>();
	
	public ResultApp(){
		
	}

	public ResultApp(String id,String status){
		this.id=id;
		this.status=status;
		this.errorCode="0";
	}
	
	public ResultApp(String id,String status,String errorCode){
		this.id=id;
		this.status=status;
		this.errorCode=errorCode;
	}	
	
	public Vector<Environment> getEnvs() {
		return envs;
	}
	
	public Environment getLastEnv(){
		return this.envs.get(envs.size()-1);
	}

	public void addEnv(Environment env){
		this.envs.add(env);
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
		for(int i=0;i<this.envs.size();i++){
			buf.append("<env id=\"");
			Environment env=this.envs.get(i);
			buf.append(env.getId());
			buf.append("\">");
			for(int j=0;j<env.getApp().size();j++){
				Application app=(Application)env.getApp().get(j);
				buf.append("<app");
				buf.append(" name=\"");
				buf.append(app.getName());
				buf.append("\" appname=\"");
				buf.append(app.getAppName());
				buf.append("\" enabled=\"");
				buf.append(app.getEnabled());
				buf.append("\" flag=\"");
				buf.append(app.getFlag());
				buf.append("\" version=\"");
				buf.append(app.getVersion());	
				buf.append("\"></app>");
			}
			buf.append("</env>");
		}	
		buf.append("</result>");
		return buf.toString();
	}

	@Override
	public String toString(){
		StringBuffer buf=new StringBuffer();
		buf.append(type);
		buf.append(" Result:[");
		buf.append("\r\n");		
		buf.append("id=");
		buf.append(this.id);
		buf.append("/deviceid=");
		buf.append(this.deviceId);
		buf.append("/status=");
		buf.append(this.status);
		buf.append("/errorcode=");
		buf.append(this.errorCode);
		for(int i=0;i<this.envs.size();i++){
			buf.append("\r\n");
			buf.append("env=");
			Environment env=this.envs.get(i);
			buf.append(env.getId());
			for(int j=0;j<env.getApp().size();j++){
				Application app=(Application)env.getApp().get(j);
				buf.append("\r\n");
				buf.append("name=");
				buf.append(app.getName());
				buf.append("/appname=");
				buf.append(app.getAppName());
				buf.append("/enabled=");
				buf.append(app.getEnabled());
				buf.append("/flag=");
				buf.append(app.getFlag());
				buf.append("/version=");
				buf.append(app.getVersion());
			}
		}
		buf.append("\r\n]");		
		return buf.toString();	
	}
}
