package com.sastraxi.machineshop.remote.actions;

import com.sastraxi.machineshop.remote.Remote;

public class ConnectAction extends RemoteAction {
    private static final long serialVersionUID = 1L;

    public ConnectAction(Remote remote, RemoteActionCallback callback) {
        super(remote, callback);
    }

    @Override
    protected boolean run(Remote remote) {
        remote.connect();
        return (remote.isConnected());
    }

}
