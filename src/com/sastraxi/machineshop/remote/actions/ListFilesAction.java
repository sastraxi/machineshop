package com.sastraxi.machineshop.remote.actions;

import java.util.List;

import com.sastraxi.machineshop.project.RemoteFileEntry;
import com.sastraxi.machineshop.project.RemoteFolder;
import com.sastraxi.machineshop.remote.NotConnectedException;
import com.sastraxi.machineshop.remote.Remote;

public class ListFilesAction extends RemoteAction {

    private static final long serialVersionUID = 1L;
    private final String path;
    private final int mode;
    private final boolean showHiddenFiles;
    
    private List<RemoteFileEntry> folderEntries;

    public ListFilesAction(RemoteFolder folder, int mode, boolean showHiddenFiles, RemoteActionCallback callback) {
        super(folder.getRemote(), callback);
        this.path = folder.getPath().toString();
        this.mode = mode;
        this.showHiddenFiles = showHiddenFiles;
    }

    public String getPath() {
        return path;
    }

    public int getMode() {
        return mode;
    }

    public boolean showHiddenFiles() {
        return showHiddenFiles;
    }

    public void setFolderEntries(List<RemoteFileEntry> folderEntries) {
        this.folderEntries = folderEntries;
    }
    
    public List<RemoteFileEntry> getFolderEntries() {
        return folderEntries;
    }
    
    @Override
    protected boolean run(Remote remote) {
        try {
            setFolderEntries(remote.listFolder(getPath(), getMode(), showHiddenFiles()));
            return true;
        } catch (NotConnectedException e) { 
            // XXX order of ops guarantees we don't get here
            assert(false);
        }
        return false;
    }

}
