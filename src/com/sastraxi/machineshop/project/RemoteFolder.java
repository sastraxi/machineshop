package com.sastraxi.machineshop.project;

import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.sastraxi.machineshop.remote.Remote;
import com.sastraxi.machineshop.remote.RemoteService;
import com.sastraxi.machineshop.remote.actions.ListFilesAction;
import com.sastraxi.machineshop.remote.actions.RemoteAction;
import com.sastraxi.machineshop.remote.actions.RemoteActionCallback;
import com.sastraxi.machineshop.util.AbsolutePath;

public class RemoteFolder extends RemoteFileEntry {

    private List<RemoteFileEntry> children;
        
    public RemoteFolder(Remote remote, AbsolutePath path) {
        super(remote, path);
    }

    @Override
    public boolean isFolder() {
        return true;
    }

    @Override
    public void create() {
        getLocalFile().mkdirs();   
    }

    @Override
    public void delete(final Context context, final boolean recursively, final Runnable callback) {
        if (getState() == STATE_UNKNOWN) {
            fetch(context, new Runnable() {                
                public void run() {
                    fetch(context, callback);
                }
            });
        } else {
            if (!recursively && children.size() > 0) {
                Log.i("RemoteFolder", "Did not delete folder " + getPath() + " because it wasn't empty and recursively=false");
            }
        }
        
    }

    @Override
    public void fetch(Context context, final Runnable callback) {
        RemoteService.queue(context, new ListFilesAction(
                this,
                Remote.MODE_LIST_ALL,
                false,
                new RemoteActionCallback() {            
                    public void run(RemoteAction action) {
                        ListFilesAction lfa = (ListFilesAction) action;
                        children = lfa.getFolderEntries();                        
                        callback.run();
                    }
                }));        
    }

    @Override
    public void save(Context context, Runnable callback) {
        Log.wtf("RemoteFolder", "We really gotta get rid of save... why are you calling it?");
    }

    public RemoteFolder partial(int to) {
        return new RemoteFolder(getRemote(), getPath().partial(to));
    }

    public List<RemoteFileEntry> getChildren() {
        return children;
    }

}
