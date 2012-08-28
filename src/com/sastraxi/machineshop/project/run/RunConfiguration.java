package com.sastraxi.machineshop.project.run;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.sastraxi.machineshop.MachineShopApplication;
import com.sastraxi.machineshop.R;
import com.sastraxi.machineshop.project.Project;

public abstract class RunConfiguration {

    private final String name;
    private final Project project;
    private final SharedPreferences settings;

    public RunConfiguration(Project project, String name) {
        this.project = project;
        this.name = name;

        this.settings = getApp().getSharedPreferences(getPreferencesName(), 0);
        PreferenceManager.setDefaultValues(getApp(), R.xml.run, false);
    }
    
    protected final SharedPreferences getSettings() {
        return settings;
    }

    public final String getPreferencesName() {
        return project.getPreferencesName() + ".run." + getName();
    }

    private final String getName() {
        return name;
    }
    
    public final Project getProject() {
        return project;
    }

    private final MachineShopApplication getApp() {
        return project.getApp();
    }

    public abstract Run createRun();
    
        
}
