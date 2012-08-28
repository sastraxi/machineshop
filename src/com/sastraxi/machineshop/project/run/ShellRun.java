package com.sastraxi.machineshop.project.run;

import android.view.View;

import com.sastraxi.machineshop.remote.Remote;

public class ShellRun extends Run {
    
    private final ShellRunConfiguration config;

    public ShellRun(ShellRunConfiguration config) {
        this.config = config;        
    }
    
    @Override
    public void run() {
        Remote remote = config.getProject().getRemote();
        config.getSettings().getString("working_path", null);
    }

    @Override
    public void terminate() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public boolean wasSuccessful() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public View createMonitorView() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public View createFinishedView() {
        // TODO Auto-generated method stub
        return null;
    }

}
