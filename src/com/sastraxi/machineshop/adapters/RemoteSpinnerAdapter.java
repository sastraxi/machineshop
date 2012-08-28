package com.sastraxi.machineshop.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.sastraxi.lookmonster.SimplerSmartListAdapter;
import com.sastraxi.machineshop.MachineShopApplication;
import com.sastraxi.machineshop.remote.Remote;

public class RemoteSpinnerAdapter extends SimplerSmartListAdapter<Remote> {

    public RemoteSpinnerAdapter(Context context) {
        super(context, ((MachineShopApplication) context.getApplicationContext()).getRemotes());
    }

    @Override
    protected String getCategory(Remote item) {
        return null;
    }

    @Override
    protected View getListItemView(int position, final Remote remote, View convertView, ViewGroup parent) {
        return ListLayouts.textAndNumber(getLayoutInflater(), convertView, parent, remote.getName(), "");
    }

    @Override
    protected int getClickMode(int position, Remote item) {
        return CLICK_MODE_INSTANTANEOUS;
    }

    @Override
    protected int getMaxCheckedItems() {
        return 0;
    }

}
