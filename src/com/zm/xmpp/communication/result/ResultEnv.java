package com.zm.xmpp.communication.result;

import java.util.Vector;

import com.zm.epad.structure.Application;
import com.zm.epad.structure.Configuration;
import com.zm.epad.structure.Environment;


public class ResultEnv extends AbstractResult implements IResult{
	
	private final static String type="env";
	private Vector<Environment> envs=new Vector<Environment>();
	
	public ResultEnv(){
		
	}

	public ResultEnv(String id,String status){
		this.id=id;
		this.status=status;
		this.errorCode="0";
	}
	
	public ResultEnv(String id,String status,String errorcode){
		this.id=id;
		this.status=status;
		this.errorCode=errorcode;
	}	
		
	public Vector<Environment> getEnvs() {
		return envs;
	}

	public void addEnv(Environment env) {
		this.envs.add(env);
	}

	public String getType(){
		return type;
	}
	
	public Environment getLastEnv(){
		return this.envs.get(envs.size()-1);
	}

	@Override
	public String toXML() {
		StringBuffer buf=new StringBuffer();
		buf.append("<result xmlns=\"");
		buf.append(this.direction);
		buf.append("\" type=\"");
		buf.append(type);		
		buf.append("\">");
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
			buf.append("<no_modify_account>");
			buf.append(env.getConf().getNoModifyAccount());
			buf.append("</no_modify_account>");
			buf.append("<no_config_wifi>");
			buf.append(env.getConf().getNoConfigWifi());
			buf.append("</no_config_wifi>");
			buf.append("<no_install_apps>");
			buf.append(env.getConf().getNoInstallApps());
			buf.append("</no_install_apps>");
			buf.append("<no_uninstall_apps>");
			buf.append(env.getConf().getNoUninstallApps());
			buf.append("</no_uninstall_apps>");
			buf.append("<no_share_location>");
			buf.append(env.getConf().getNoShareLocation());
			buf.append("</no_share_location>");
			buf.append("<no_install_unknown_sources>");
			buf.append(env.getConf().getNoInstallUnknownSources());
			buf.append("</no_install_unknown_sources>");
			buf.append("<no_config_bluetooth>");
			buf.append(env.getConf().getNoConfigBluetooth());
			buf.append("</no_config_bluetooth>");
			buf.append("<no_usb_file_transter>");
			buf.append(env.getConf().getNoUsbFileTranster());
			buf.append("</no_usb_file_transter>");
			buf.append("<no_config_credentials>");
			buf.append(env.getConf().getNoConfigCredentials());
			buf.append("</no_config_credentials>");
			buf.append("<no_remove_user>");
			buf.append(env.getConf().getNoRemoveUser());
			buf.append("</no_remove_user>");
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
		buf.append("/status=");
		buf.append(this.status);
		buf.append("/errorcode=");
		buf.append(this.errorCode);
		for(int i=0;i<this.envs.size();i++){
			buf.append("\r\n");
			buf.append("env=");
			buf.append(this.envs.get(i).getId());
			Configuration cfg=this.envs.get(i).getConf();
			buf.append("/no_modify_account=");
			buf.append(cfg.getNoModifyAccount());
			buf.append("/no_config_wifi=");
			buf.append(cfg.getNoConfigWifi());
			buf.append("/no_install_apps=");
			buf.append(cfg.getNoInstallApps());
			buf.append("/no_uninstall_apps=");
			buf.append(cfg.getNoUninstallApps());
			buf.append("/no_share_location=");
			buf.append(cfg.getNoShareLocation());
			buf.append("/no_install_unknown_sources=");
			buf.append(cfg.getNoInstallUnknownSources());
			buf.append("/no_config_bluetooth=");
			buf.append(cfg.getNoConfigBluetooth());
			buf.append("/no_usb_file_transter=");
			buf.append(cfg.getNoUsbFileTranster());
			buf.append("/no_config_credentials=");
			buf.append(cfg.getNoConfigCredentials());
			buf.append("/no_remove_user=");
			buf.append(cfg.getNoRemoveUser());
		}
		buf.append("\r\n]");		
		return buf.toString();
		
	}



	
}
