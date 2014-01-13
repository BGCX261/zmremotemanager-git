package com.zm.epad.structure;

public class Application {
	
	private String name;
	private String appName;
	private String version="N/A";
	private String url="N/A";
	private String status="N/A";
	private String install="N/A";
	private String enabled="N/A";
	private String validation="N/A";/*Ó¦ÓÃÐ£ÑéÐÅÏ¢*/
	private String flag="N/A";
	
	public Application(){
		
	}
	
	public Application(String appName,String version,String url){
		this.appName=appName;
		this.version=version;
		this.url=url;
	}
	
	public String getAppName() {
		return appName;
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}

	public String getInstall() {
		return install;
	}

	public void setInstall(String install) {
		this.install = install;
	}

	public String getEnabled() {
		return enabled;
	}

	public void setEnabled(String enabled) {
		this.enabled = enabled;
	}



	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFlag() {
		return flag;
	}

	public void setFlag(String flag) {
		this.flag = flag;
	}

	public String getValidation() {
		return validation;
	}

	public void setValidation(String validation) {
		this.validation = validation;
	}

	
}
