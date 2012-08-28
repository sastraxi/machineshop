package com.sastraxi.machineshop.remote.actions;

import com.sastraxi.machineshop.remote.Remote;

public class DisconnectAction extends RemoteAction {
    private static final long serialVersionUID = 1L;

    public DisconnectAction(Remote remote, RemoteActionCallback callback) {
        super(remote, callback);
    }

    @Override
    protected boolean run(Remote remote) {
        remote.disconnect();
        return (!remote.isConnected());
    }

}
