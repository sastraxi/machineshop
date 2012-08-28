package com.sastraxi.machineshop.project.run;

import android.view.View;

import com.sastraxi.machineshop.project.Project;

public class ShellRunConfiguration extends RunConfiguration {
    
    public ShellRunConfiguration(Project project, String name) {
        super(project, name);
    }
    
    @Override
    public Run createRun() {
        return new ShellRun(this);
    }

}
