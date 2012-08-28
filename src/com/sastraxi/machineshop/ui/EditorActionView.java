package com.sastraxi.machineshop.ui;

import android.view.View;
import android.widget.LinearLayout;

import com.sastraxi.machineshop.EditorActivity;
import com.sastraxi.machineshop.MachineShopApplication;

public class EditorActionView extends LinearLayout {

    private View taskView;

    public EditorActionView(EditorActivity activity) {
        super(activity);

        // TODO create standard editor/run/log/collaborate spinner
        //ProjectActivitySpinner spinner = new ProjectActivitySpinner(activity, activity.getProject());
        //addView(spinner):
        
        // TODO add divider
        
        MachineShopApplication app = (MachineShopApplication) getContext().getApplicationContext();        
        taskView = app.getBackgroundTaskManager().getActionBarView(true);        
        addView(taskView);        
    }

}
