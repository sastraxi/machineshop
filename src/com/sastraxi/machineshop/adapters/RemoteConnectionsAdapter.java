package com.sastraxi.machineshop.adapters;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.sastraxi.lookmonster.SimplerSmartListAdapter;
import com.sastraxi.machineshop.MachineShopApplication;
import com.sastraxi.machineshop.R;
import com.sastraxi.machineshop.RemoteSettingsActivity;
import com.sastraxi.machineshop.remote.Remote;
import com.sastraxi.machineshop.remote.RemoteService;
import com.sastraxi.machineshop.remote.actions.ConnectAction;
import com.sastraxi.machineshop.remote.actions.DisconnectAction;
import com.sastraxi.machineshop.remote.actions.RemoteAction;
import com.sastraxi.machineshop.remote.actions.RemoteActionCallback;

public class RemoteConnectionsAdapter extends SimplerSmartListAdapter<Remote> implements OnCheckedChangeListener {

    public RemoteConnectionsAdapter(Context context) {
        super(context, ((MachineShopApplication) context.getApplicationContext()).getRemotes());
    }

    @Override
    protected String getCategory(Remote item) {
        return null;
    }

    @Override
    protected View getListItemView(final int position, final Remote remote, View convertView, ViewGroup parent) {
        View view = ListLayouts.iconTextSwitch(getLayoutInflater(), convertView, parent,
                remote.getName(), 
                R.drawable.icon_settings,
                new OnClickListener() {
                    public void onClick(View v) {
                        RemoteSettingsActivity.launchIntent(getContext(), remote);
                    }
                },
                remote.isConnected(),
                this);
        view.setTag(remote);
        return view;
    }

    @Override
    protected int getClickMode(int position, Remote item) {
        return CLICK_MODE_NOT_CLICKABLE;
    }

    @Override
    protected int getMaxCheckedItems() {
        return 0;
    }

    private void setCheckedInListener(CompoundButton button, boolean isChecked) {
        button.setOnCheckedChangeListener(null);
        button.setChecked(isChecked);
        button.setOnCheckedChangeListener(this);
    }    
    
    @Override
    public void onCheckedChanged(final CompoundButton buttonView, boolean isChecked) {               
        final Remote remote = (Remote) ((ViewGroup) buttonView.getParent()).getTag();
        
        if (isChecked) {
            
            // show as false for time being, connect to remote
            buttonView.setEnabled(false);
            RemoteService.queue(getContext(), new ConnectAction(remote, new RemoteActionCallback() {
                @Override
                public void run(RemoteAction action) {
                    setCheckedInListener(buttonView, remote.isConnected());
                    buttonView.setEnabled(true);
                }
            }));
            
        } else {
            
            // show as true for the time being, disconnect from remote
            buttonView.setEnabled(false);
            RemoteService.queue(getContext(), new DisconnectAction(remote, new RemoteActionCallback() {                                
                @Override
                public void run(RemoteAction action) {
                    setCheckedInListener(buttonView, !remote.isConnected());
                    buttonView.setEnabled(true);
                }
            }));
            
        }       
    }

}
