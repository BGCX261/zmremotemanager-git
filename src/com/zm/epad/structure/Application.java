package com.zm.epad.structure;

public class Application {

	private String appName;
	private String version;
	private String url;
	private String status;
	
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

	
}
