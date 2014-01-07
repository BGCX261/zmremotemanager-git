package com.zm.epad.structure;

import java.util.Collection;

public class Device {

	private String id;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Collection<Environment> getEnv() {
		return env;
	}
	public void setEnv(Collection<Environment> env) {
		this.env = env;
	}
	private Collection<Environment> env;
	
	
}
