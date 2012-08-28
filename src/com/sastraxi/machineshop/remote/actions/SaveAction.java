package com.sastraxi.machineshop.remote.actions;

import com.sastraxi.machineshop.project.RemoteFile;
import com.sastraxi.machineshop.remote.Remote;

public class SaveAction extends RemoteFileAction {
    private static final long serialVersionUID = 1L;

    public SaveAction(Remote remote, RemoteFile file, RemoteActionCallback callback) {
        super(remote, file, callback);
    }
    
}
