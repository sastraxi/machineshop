package com.sastraxi.machineshop.remote;

import android.util.Log;

import com.jcraft.jsch.JSchException;
import com.sastraxi.machineshop.remote.actions.RemoteAction;

/**
 * 
 * @author Cameron Gorrie
 */
public class PortForward {

    private final Remote remote;
    private boolean isActive;
    private final int remotePort;
    private final int localPort;

    public PortForward(Remote remote, int localPort, int remotePort) {
        this.remote = remote;
        this.localPort = localPort;
        this.remotePort = remotePort;
        this.isActive = false;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void start() {
        if (isActive()) return;
        RemoteService.queue(remote.getApp(), new RemoteAction(remote, null) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean run(Remote remote) {
                try {
                    remote.getSession().setPortForwardingL(localPort, "localhost", remotePort);
                    isActive = true;
                    return true;
                } catch (JSchException e) {
                    Log.e("PortForward", "Could not set up", e);
                    return false;
                }
            }            
        });
    }

    public void stop() {
        if (!isActive()) return;
        RemoteService.queue(remote.getApp(), new RemoteAction(remote, null) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean run(Remote remote) {
                try {
                    remote.getSession().delPortForwardingL(localPort);
                    isActive = false;
                    return true;
                } catch (JSchException e) {
                    Log.e("PortForward", "Could not tear down", e);
                    return false;
                }
            }            
        });
    }
    
}
