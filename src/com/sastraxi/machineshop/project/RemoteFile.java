package com.sastraxi.machineshop.project;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.sastraxi.machineshop.MachineShopApplication;
import com.sastraxi.machineshop.remote.Remote;
import com.sastraxi.machineshop.remote.RemoteService;
import com.sastraxi.machineshop.remote.actions.LoadAction;
import com.sastraxi.machineshop.remote.actions.RemoteAction;
import com.sastraxi.machineshop.remote.actions.RemoteActionCallback;
import com.sastraxi.machineshop.remote.actions.SaveAction;
import com.sastraxi.machineshop.util.AbsolutePath;

/**
 * @author Cameron Gorrie
 */
public class RemoteFile extends RemoteFileEntry {    

	private static final String ARG_REMOTE_NAME = "remote_name";
    private static final String ARG_REMOTE_PATH = "remote_path";    
    
    // TODO maximum cache time should be a user pref.
    public static final long MAXIMUM_CACHE_TIME = 12 * 60 * 60; // 12 hours    

	public RemoteFile(Remote remote, AbsolutePath path) {
	    super(remote, path);
	}
	
    public File getAutosaveFile() {
        return new File(getLocalFile().getAbsolutePath() + ".ms-autosave");
    }
    
    public boolean hasAutosaveModifications() {
        return getAutosaveFile().exists();
    }
    
    public void saveFromAutosave() {
        try {
            getLocalFile().delete();
            FileUtils.moveFile(getAutosaveFile(), getLocalFile());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void discardAutosave() {
        getAutosaveFile().delete();
    }    
    
    public void toBundle(String argPrefix, Bundle outState) {
        outState.putString(argPrefix + ARG_REMOTE_NAME, getRemote().getName());
        outState.putString(argPrefix + ARG_REMOTE_PATH, getPath().toString());        
    }    
    
    public static RemoteFile fromBundle(Context context, String argPrefix, Bundle inState) {
        MachineShopApplication app = (MachineShopApplication) context.getApplicationContext();
        
        String remoteName = inState.getString(argPrefix + ARG_REMOTE_NAME);
        Remote remote = app.getRemote(remoteName);
        
        String remotePath = inState.getString(argPrefix + ARG_REMOTE_PATH);
        return new RemoteFile(remote, new AbsolutePath(remotePath));        
    }

    @Override
    public boolean isFolder() {
        return false;
    }

    @Override
    public void fetch(Context context, final Runnable callback) {
        RemoteService.queue(context, new LoadAction(getRemote(), this, new RemoteActionCallback() {            
            @Override
            public void run(RemoteAction action) {
                callback.run();
            }
        }));
    }

    @Override
    public void save(Context context, final Runnable callback) {
        RemoteService.queue(context, new SaveAction(getRemote(), this, new RemoteActionCallback() {            
            @Override
            public void run(RemoteAction action) {
                callback.run();
            }
        }));
    }

    @Override
    public void create() {
        try {
            getLocalFile().createNewFile();
        } catch (IOException e) {
            Log.w("Couldn't create file", "For Some Reason:", e);
        }
    }

    @Override
    public void delete(Context context, boolean recursively, final Runnable callback) {
        getLocalFile().delete();
        save(context, callback);
    }

}
