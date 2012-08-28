package com.sastraxi.machineshop.remote;

import java.io.Serializable;

import android.app.Activity;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.sastraxi.machineshop.MachineShopApplication;
import com.sastraxi.machineshop.R;
import com.sastraxi.machineshop.remote.actions.ConnectAction;
import com.sastraxi.machineshop.remote.actions.DisconnectAction;
import com.sastraxi.machineshop.remote.actions.RemoteAction;
import com.sastraxi.machineshop.remote.actions.RemoteActionCallback;

public class RemoteService extends IntentService {

    private static final String INTENT_EXTRA_ACTION = "RemoteAction";
    private static final int NOTIFICATION_ID = 9872;   
    
    public RemoteService() {
        super("RemoteService");
    }

    public MachineShopApplication getApp() {
        return (MachineShopApplication) getApplicationContext();
    }    
    
    /**
     * The callback is run on the same thread that called this function.
     * Will show the background tasks dialog for blocking actions,
     * so run on the UI thread if you're using a callback.
     */
    public static void queue(Context context, final RemoteAction action) {
        final MachineShopApplication app = (MachineShopApplication) context.getApplicationContext();
        
        Intent intent = new Intent(context, RemoteService.class);
        intent.putExtra(INTENT_EXTRA_ACTION, action);
    
        Log.i("Queue", action.toString());
        
        // remove the callback itself as we might have trouble serializing it
        // e.g. anonymous inner classes will pull in things we don't want
        final RemoteActionCallback callback = action.removeCallback();
            
        // XXX NOTE: this is pretty inefficient, but only when we get to thousands of 
        // computers being connected at one time. I doubt anyone will realistically have more
        // than 2 or 3, in VERY rare cases. Still, worth noting. 
        final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);            
        manager.registerReceiver(new BroadcastReceiver() {                
            @Override
            public void onReceive(Context context, Intent intent) {

                Serializable s = intent.getSerializableExtra(RemoteAction.INTENT_CALLBACK_EXTRA_ACTION);                    
                if (!(s instanceof RemoteAction)) {
                    Log.e("Remote Action Callback", "extra not instance of RemoteAction!");
                }
                
                final RemoteAction innerAction = (RemoteAction) s; 
                if (innerAction.equals(action)) {
                    
                    manager.unregisterReceiver(this);

                    Log.i("Finish", action.toString());
                    
                    // update the notification in case of 
                    if (action instanceof ConnectAction || action instanceof DisconnectAction) {
                        updateNotification(app.getCurrentActivity());
                    }
                    
                    // run on the current activity's UI thread.
                    if (callback != null) {
                        app.getCurrentActivity().runOnUiThread(
                            new Runnable() {
                                public void run() {
                                    callback.run(innerAction);   
                                }
                            });
                    }
                    
                }                    
            }
        }, new IntentFilter(RemoteAction.INTENT_CALLBACK));
        
        context.startService(intent);        
        
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        
        Serializable s = intent.getSerializableExtra(INTENT_EXTRA_ACTION);
        if (s == null || !(s instanceof RemoteAction)) {
            Log.e("RemoteService", "Need a RemoteAction extra (RemoteService.INTENT_ADD_EXTRA_ACTION)");
            return;
        }
        
        RemoteAction action = (RemoteAction) s;
        Remote remote = action.getRemote(getApp());
        remote.queueAction(action);
        
        // TODO update notification (startForeground/stopForeground)
        // depending on what remotes are currently connected.
        // e.g. "Connected to 4 remote machines"
        //      "Connecting to Tenochtitlan" (if first one)
        //      "Disconnecting from Gibraltar" (if last one)


    }

    private static void updateNotification(Activity activity) {
        // TODO necesitamos un opcion en el archivo preferencia
        // principal para eliminar este notification

        NotificationManager notificationManager = (NotificationManager) 
                activity.getSystemService(Context.NOTIFICATION_SERVICE);
        
        MachineShopApplication app = (MachineShopApplication) activity.getApplicationContext();
        
        String noticeText = "No connections";
        int numConnected = 0;
        for (Remote remote: app.getRemotes()) {
            if (remote.isConnected()) {
                noticeText = "Connected to " + remote.getName();
                numConnected += 1;
            }            
        }
        
        if (numConnected == 0) {
            notificationManager.cancel(NOTIFICATION_ID);
            return;
        }

        if (numConnected > 1) {
            noticeText = "Connected to " + numConnected + " remotes";
        }
        
        Notification notification = new Notification.Builder(activity)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Machine Shop")
                .setContentText(noticeText)
                .setTicker(noticeText)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .getNotification();
        notification.flags |= Notification.FLAG_NO_CLEAR;

        notificationManager.notify(NOTIFICATION_ID, notification);
        
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        // get rid of the notification.
        NotificationManager notificationManager = (NotificationManager) 
                getApp().getSystemService(Context.NOTIFICATION_SERVICE);        
        notificationManager.cancel(NOTIFICATION_ID);
    }
    

}
