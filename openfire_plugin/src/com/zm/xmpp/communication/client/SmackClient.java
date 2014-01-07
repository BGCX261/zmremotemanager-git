package com.zm.xmpp.communication.client;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.ProviderManager;

import com.zm.xmpp.communication.Constants;
import com.zm.xmpp.communication.command.ICommand;

public class SmackClient {
	

	private static XMPPConnection conn;
	public static String debug;
	public static String destId;
	public static String clientId;
	public static String send;
	public static String listen;
	
	public static XMPPConnection getConn() {
		return conn;
	}

	public static XMPPConnection getConnection(ConnectionConfiguration config,
			String username, String password,IProcessor processor) throws XMPPException {
		conn = new XMPPConnection(config);
		boolean auth = conn.getSASLAuthentication().isAuthenticated();

		conn.connect();
		conn.login(username, password);

		System.out.println("Connected to domain:" + conn.getServiceName());

		PacketListener listener = new ClientPacketListener(processor);
		PacketFilter filter = new ClientPacketFilter();
		conn.addPacketListener(listener, filter);

		ZMIQCommandProvider cp = new ZMIQCommandProvider();
		ProviderManager.getInstance().addIQProvider(Constants.XMPP_COMMAND,
				listen, cp);

		
		ZMIQResultProvider rp = new ZMIQResultProvider();
		ProviderManager.getInstance().addIQProvider(Constants.XMPP_RESULT,
				listen, rp);

		return conn;
	}


}
