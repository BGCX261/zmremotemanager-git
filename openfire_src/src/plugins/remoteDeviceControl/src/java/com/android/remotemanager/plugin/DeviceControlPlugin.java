package com.android.remotemanager.plugin;

import java.lang.String;

import org.jivesoftware.openfire.IQHandlerInfo;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.IQRouter;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.handler.IQHandler;
import org.xmpp.packet.IQ;

import java.io.File;
public class DeviceControlPlugin  implements Plugin {

    
    @Override
    public void initializePlugin(PluginManager manager, File pluginDirectory) {
        // TODO Auto-generated method stub
        
       System.out.println("Starting RemoteDevice Control Plugin");
       XMPPServer xmppServer =  XMPPServer.getInstance();
       IQRouter iqRouter = xmppServer.getIQRouter();
       
       iqRouter.addHandler(new DeviceControlIQHandler(null));
    }

    @Override
    public void destroyPlugin() {
        // TODO Auto-generated method stub
        
    }
    
    class DeviceControlIQHandler extends IQHandler{
        
        public DeviceControlIQHandler(String moduleName){
            super(moduleName);
        }
        @Override
        public IQ handleIQ(IQ packet){
            System.out.println(packet.toXML());
            return null;
        }
        
        @Override
        public IQHandlerInfo getInfo(){
            return null;
        }
    }

}

