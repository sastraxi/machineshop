package com.sastraxi.machineshop.remote.actions;

import java.io.Serializable;
import java.util.UUID;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.sastraxi.machineshop.MachineShopApplication;
import com.sastraxi.machineshop.remote.Remote;

/**
 * An action that can be serviced by RemoteService.
 * Subclasses should only store basic types (e.g. String, int, boolean)
 * so that serialization is light and easy.
 * 
 * Each object is given a UUID that is maintained as it gets passed around
 * through receivers and services and all that jazz.
 */
public abstract class RemoteAction implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String INTENT_CALLBACK = "com.sastraxi.machineshop.intents.RemoteActionCallback";
    public static final String INTENT_CALLBACK_EXTRA_ACTION = "RemoteAction";
    
    private final String remoteName;
    private RemoteActionCallback callback;
    private final String uniqueID;

    private boolean success;
    private Exception exception = null;
    
    /**
     * @param callback code to run after this is done executing
     *                 iff not null. It is up to queues that process this
     *                 RemoteAction to call runCallback.
     *
     *                 Also a hint to the back-end that it should treat this
     *                 action as high-priority (e.g. put it at the front of
     *                 the task queue).
     */ 
    public RemoteAction(Remote remote, RemoteActionCallback callback) {
        this.callback = callback;
        this.remoteName = remote.getName();
        this.uniqueID = UUID.randomUUID().toString();
    }
    
    public Remote getRemote(MachineShopApplication app) {
        return app.getRemote(remoteName);
    }

    public boolean hasCallback() {
        return callback != null;
    }
    
    /**
     * This RemoteAction is serialized and sent along with a broadcast intent,
     * so any extra return information should be stored in the RemoteAction
     * itself (as you know what type to expect).
     */
    public void finish(MachineShopApplication app) {        
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(INTENT_CALLBACK);
        broadcastIntent.putExtra(INTENT_CALLBACK_EXTRA_ACTION, this);
        
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(app);
        manager.sendBroadcast(broadcastIntent);
    }
    
    
    
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof RemoteAction)) return false;
        RemoteAction other = (RemoteAction) o;
        return other.uniqueID.equals(uniqueID);
    }

    public RemoteActionCallback removeCallback() {
        RemoteActionCallback cb = callback;
        callback = null;
        return cb;
    }

    /**
     * Called on the correct thread, so that you can do all the
     * SSH magic that you want.
     * 
     * Return true if this was a triumph, otherwise false.
     */
    protected abstract boolean run(Remote remote);
    
    /**
     * Consumers of RemoteActions must call this, and not run.
     */
    public void execute(Remote remote) {
        assert(remote == getRemote(remote.getApp()));
        
        try {
            this.success = run(remote);
        } catch (Exception e) {
            this.success = false;
            this.exception = e;
        }
        
        this.finish(remote.getApp());        
    }
    
    public boolean wasSuccessful() {
        return success;
    }
    
    public Exception getException() {
        return exception;
    }
    
}
