package com.android.remotemanager.plugins;


public class XmppClient {
    
    static private XmppClient mXmppClient = null;
    static public XmppClient getXmppClientInstance(){
        if(mXmppClient == null)
            mXmppClient = new XmppClient();
        return mXmppClient;
    }
    private XmppClient(){
        
    }
    
}
