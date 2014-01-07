package com.zm.xmpp.communication.client;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import com.zm.xmpp.communication.command.ICommand;

public class ZMIQCommandProvider implements IQProvider {
	

	@Override
	public IQ parseIQ(XmlPullParser parser) throws Exception {
		// System.out.println("Enter IQ Provider");
		ZMIQCommand iq = new ZMIQCommand();
		boolean done = false;
		try {
			String type=parser.getAttributeValue(null,"type");
			ICommand command = ClientCommandUtil.createCommand(type);
			command.setDirection(parser.getNamespace());
			System.out.println("receive command type:"+type);
			while (!done) {
				int eventType = parser.next();
				if (eventType == XmlPullParser.START_TAG) {
					ClientCommandUtil.parseCommand(parser.getName(),
							parser.nextText(), command);
				} else if (eventType == XmlPullParser.END_TAG) {
					done = true;
				}
			}
			iq.setCommand(command);
			System.out.println(command.toString());

		} catch (Exception e) {
			e.printStackTrace();
			iq=null;
		}


		return iq;
	}
	
	

}
