package com.zm.xmpp.communication.command;

import java.util.ArrayList;
import com.zm.epad.structure.IPolicy;
import com.zm.epad.structure.SwitchPolicy;

public class Command4Policy extends AbstractCommand implements ICommand {
	private final static String type = "policy";
	private ArrayList<IPolicy> ipolicies = new ArrayList<IPolicy>();
	private String expire;

	public Command4Policy(){
		
	}
	
	public Command4Policy(String to, String direction, String id,
			String issuetime, String expire) {
		super();
		this.to = to;
		this.direction = direction;
		this.id = id;
		this.issueTime = issuetime;
		this.expire = expire;
	}

	public void addPolicy(IPolicy ipolicy) {
		this.ipolicies.add(ipolicy);
	}

	public ArrayList<IPolicy> getIpolicies() {
		return ipolicies;
	}

	public void setIpolicies(ArrayList<IPolicy> ipolicies) {
		this.ipolicies = ipolicies;
	}

	public String getExpire() {
		return expire;
	}

	public void setExpire(String expire) {
		this.expire = expire;
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return type;
	}

	public String toString() {

		StringBuffer buf = new StringBuffer();
		buf.append("Policy Command:[");
		buf.append("\r\n");
		buf.append("id=");
		buf.append(this.id);
		buf.append("/issuetime=");
		buf.append(this.issueTime);
		buf.append("/expire=");
		buf.append(this.expire);
		for (int i = 0; i < ipolicies.size(); i++) {
			buf.append("\r\n");
			buf.append("policy");
			IPolicy ipolicy = ipolicies.get(i);
			if (ipolicy.getType().equals("switch")) {
				SwitchPolicy policy = (SwitchPolicy) (ipolicy);
				buf.append("\r\n");
				buf.append("type=");
				buf.append(policy.getType());
				buf.append("/action=");
				buf.append(policy.getAction());
				buf.append("/starttime=");
				buf.append(policy.getStarttime());
				buf.append("/endtime=");
				buf.append(policy.getEndtime());
				buf.append("/parameter=");
				buf.append(policy.getParameter());
			}
		}

		buf.append("\r\n]");

		return buf.toString();

	}

	@Override
	public String toXML() {
		StringBuffer buf = new StringBuffer();
		buf.append("<command xmlns=\"");
		buf.append(this.direction);
		buf.append("\" type=\"");
		buf.append(type);
		buf.append("\">");
		buf.append("<id>");
		buf.append(this.id);
		buf.append("</id>");
		buf.append("<issuetime>");
		buf.append(this.issueTime);
		buf.append("</issuetime>");
		buf.append("<expire>");
		buf.append(this.expire);
		buf.append("</expire>");

		for (int i = 0; i < this.ipolicies.size(); i++) {
			buf.append("<policy type=\"");

			IPolicy ipolicy = ipolicies.get(i);
			if (ipolicy.getType().equals("switch")) {
				SwitchPolicy policy = (SwitchPolicy) (ipolicy);
				buf.append(policy.getType());
				buf.append("\">");
				buf.append("<action>");
				buf.append(policy.getAction());
				buf.append("</action>");
				buf.append("<starttime>");
				buf.append(policy.getStarttime());
				buf.append("</starttime>");
				buf.append("<endtime>");
				buf.append(policy.getEndtime());
				buf.append("</endtime>");
				buf.append("<parameter>");
				buf.append(policy.getParameter());
				buf.append("</parameter>");
			}
			buf.append("</policy>");
		}
		buf.append("</command>");
		return buf.toString();
	}

	public static void main(String[] args) {
		ArrayList<SwitchPolicy> policies = new ArrayList<SwitchPolicy>();
		SwitchPolicy swtichpolicy = new SwitchPolicy();
		swtichpolicy.setType("switch");
		swtichpolicy.setAction("enableuser");
		policies.add(swtichpolicy);

		Command4Policy command = new Command4Policy("to", "direction", "id",
				"issuetime", "expire");
		command.addPolicy(swtichpolicy);
		command.addPolicy(swtichpolicy);
		// System.out.println(command.toString());
		System.out.println(command.toXML());
	}
}
