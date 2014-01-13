package com.zm.epad.structure;

import java.util.Collection;
import java.util.Vector;

public class Environment {

	private String id;  //¿Í»§¶ËµÄ²Ù×÷ÏµÍ³ÓÃ»§id
	private String envId;//Êý¾Ý¿âÖÐ¸Ã»·¾³µÄ±êÊ¾id
	private Configuration conf;
	private Vector<Application> app=new Vector<Application>();
//	private Collection<Application> blackApp;
	public String getEnvId() {
		return envId;
	}
	public void setEnvId(String envid) {
		this.envId = envid;
	}

	public Configuration getConf() {
		return conf;
	}
	public void setConf(Configuration conf) {
		this.conf = conf;
	}
	public Vector<Application> getApp() {
		return app;
	}
	public void setApp(Vector<Application> app) {
		this.app = app;
	}

	public void addApp(Application app){
		this.app.add(app);
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	
	

}
