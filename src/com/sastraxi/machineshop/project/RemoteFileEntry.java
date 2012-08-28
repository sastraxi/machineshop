package com.sastraxi.machineshop.project;

import java.io.File;

import android.content.Context;

import com.sastraxi.machineshop.MachineShopApplication;
import com.sastraxi.machineshop.remote.Remote;
import com.sastraxi.machineshop.util.AbsolutePath;
import com.sastraxi.machineshop.util.PathUtils;

public abstract class RemoteFileEntry implements Comparable<RemoteFileEntry> {
    
    public static final int STATE_UNKNOWN = 0;
    public static final int STATE_CACHED = 1;
    public static final int STATE_LOCAL_MODIFICATIONS = 2; // store in d.b.

    protected int state = STATE_UNKNOWN;
    protected long lastRefreshed = -1;
    
    private final Remote remote;
    private final AbsolutePath path;

    public RemoteFileEntry(Remote remote, AbsolutePath path) {
        this.remote = remote;
        this.path = path;
    }
    
    public Remote getRemote() {
        return remote;
    }
    
    public AbsolutePath getPath() {
        return path;
    }
    
    public File getLocalFile() {
        return PathUtils.getDescendantFile(remote.getFolder(), path.toString());
    }    

    public String getPathRelativeToProject(Project project) {
        return project.getRemotePath().getRelativePath(path);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o == this)
            return true;
        if (o.getClass() != getClass())
            return false;

        RemoteFileEntry rhs = (RemoteFileEntry) o;
        if (rhs.getRemote() != getRemote())
            return false;
        return rhs.getPath().equals(getPath());
    }

    @Override
    public int hashCode() {
        return getRemote().getName().hashCode() + getPath().hashCode();
    }
   
    public int getState() {
        return state;
    }
    
    protected void setState(int state) {
        this.state = state;
    }
    
    abstract public boolean isFolder();    
    abstract public void fetch(Context context, Runnable callback);
    abstract public void save(Context context, Runnable callback);
    abstract public void create();    
    abstract public void delete(Context context, boolean recursively, Runnable callback);
    
    // XXX: move this to an Activity or some RemoteFileTools
    public void fetchAndBlock(Context context, final Runnable callback) {
        final MachineShopApplication app = (MachineShopApplication) context.getApplicationContext();
        app.getBackgroundTaskManager().showDialog(true);
        fetch(context, new Runnable() {
            public void run() {
                app.getBackgroundTaskManager().hideDialog();
                callback.run();
            }            
        });
    }
    
    public String getName() {
        return getLocalFile().getName();
    }

    public int compareTo(RemoteFileEntry another) {
        if (isFolder() && !another.isFolder()) return -1;
        if (!isFolder() && another.isFolder()) return 1;
        return getName().compareTo(another.getName());
    }

    public RemoteFolder getParent() {
        return new RemoteFolder(getRemote(), getPath().getParent());
    }
    
}
