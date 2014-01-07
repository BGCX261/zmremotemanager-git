package com.zm.xmpp.communication.client;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import com.zm.xmpp.communication.result.IResult;

public class ZMIQResultProvider implements IQProvider {
	
	@Override
	public IQ parseIQ(XmlPullParser parser) throws Exception {
		// System.out.println("Enter IQ Provider");
		ZMIQResult iq = new ZMIQResult();
		boolean done = false;

		try {
			String type=parser.getAttributeValue(null,"type");
			System.out.println("receive result type:"+type);
			IResult result=ClientResultUtil.createResult(type);
			result.setDirection(parser.getNamespace());
			while (!done) {
				int eventType = parser.next();
				if (eventType == XmlPullParser.START_TAG) {
					ClientResultUtil.parseResult(parser.getName(),
							parser.nextText(), result);
				} else if (eventType == XmlPullParser.END_TAG) {
					done = true;
				}
			}
			iq.setResult(result);
			System.out.println(result.toString());
			
		} catch (Exception e) {
			e.printStackTrace();
			iq=null;
		}

		return iq;
	}
	
	

}
