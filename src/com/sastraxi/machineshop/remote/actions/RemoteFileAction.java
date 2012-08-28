package com.sastraxi.machineshop.remote.actions;

import com.sastraxi.machineshop.MachineShopApplication;
import com.sastraxi.machineshop.project.RemoteFile;
import com.sastraxi.machineshop.remote.Remote;
import com.sastraxi.machineshop.util.AbsolutePath;

public class RemoteFileAction extends RemoteAction {
    private static final long serialVersionUID = 1L;

    private final String remotePath;
    
    public RemoteFileAction(Remote remote, RemoteFile file, RemoteActionCallback callback) {
        super(remote, callback);
        this.remotePath = file.getPath().toString();
    }
    
    public RemoteFile getFile(MachineShopApplication app) {
        Remote remote = getRemote(app);
        return new RemoteFile(remote, new AbsolutePath(remotePath));
    }
    
    @Override
    protected boolean run(Remote remote) {
        // No-op: FileOperationsQueue does all the heavy lifting.
        // this is never called.
        return true;
    }

}
