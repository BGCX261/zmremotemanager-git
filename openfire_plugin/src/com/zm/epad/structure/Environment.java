package com.zm.epad.structure;

import java.util.Collection;

public class Environment {

	private String username;
	private Collection<Application> whiteApp;
	private Collection<Application> blackApp;
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public Collection<Application> getWhiteApp() {
		return whiteApp;
	}
	public void setWhiteApp(Collection<Application> whiteApp) {
		this.whiteApp = whiteApp;
	}
	public Collection<Application> getBlackApp() {
		return blackApp;
	}
	public void setBlackApp(Collection<Application> blackApp) {
		this.blackApp = blackApp;
	}
	

}
