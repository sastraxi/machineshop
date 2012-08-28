package com.sastraxi.machineshop.project.run;

import java.util.ArrayList;
import java.util.List;

import android.view.View;

public abstract class Run {

    public interface FinishListener {
        public void onFinish(Run run);
    }

    private List<FinishListener> finishListeners = new ArrayList<FinishListener>();
    
    public void addFinishListener(FinishListener l) {
        finishListeners.add(l);
    }
    
    public void removeFinishListener(FinishListener l) {
        finishListeners.remove(l);
    }
    
    protected void notifyFinishListeners() {
        for (FinishListener l: finishListeners) {
            l.onFinish(this);
        }
    }
    
    /**
     * It is your responsibility to notify the finishListeners
     * when isRunning changes from true to false.
     */
    public abstract void run();
    
    /**
     * It is your responsibility to notify the finishListeners
     * when isRunning changes from true to false.
     */
    public abstract void terminate();
    
    public abstract boolean isRunning();    
    public abstract boolean wasSuccessful();
    
    /**
     * Create a View that will automatically update as new information about
     * this Run becomes available (e.g. more content from stdout/err, 
     */
    public abstract View createMonitorView();    
    public abstract View createFinishedView();
    
}
