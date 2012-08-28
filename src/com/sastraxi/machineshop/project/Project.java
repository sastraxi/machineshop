package com.sastraxi.machineshop.project;

import java.io.File;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.sastraxi.machineshop.MachineShopApplication;
import com.sastraxi.machineshop.R;
import com.sastraxi.machineshop.remote.Remote;
import com.sastraxi.machineshop.util.AbsolutePath;
import com.sastraxi.machineshop.util.PathUtils;

public class Project {

	private static final String PREFERENCE_REMOTE_PATH = "remote_path";
	
    private final Remote remote;
    private SharedPreferences settings;
    private final String name;

	public Project(Remote remote, String name) {
		this.remote = remote;
        this.name = name;

        this.settings = getApp().getSharedPreferences(getPreferencesName(), 0);
        PreferenceManager.setDefaultValues(getApp(), R.xml.project, false);
	}

	public String getPreferencesName() {
	    return remote.getPreferencesName() + "." + getName();
	}
	
	public MachineShopApplication getApp() {
	    return remote.getApp();
	}

	public File getFolder() {
		return PathUtils.getDescendantFile(remote.getFolder(), getRemotePath().toString());
	}

	public String getName() {
		return name;
	}

    public String getLocalPath() {
        return getFolder().getAbsolutePath();
    }

	public AbsolutePath getRemotePath() {
		return new AbsolutePath(settings.getString(PREFERENCE_REMOTE_PATH, null));
	}

    public RemoteFolder getRemoteFolder() {
        return new RemoteFolder(getRemote(), getRemotePath());
    }   

    private void setRemotePath(AbsolutePath remotePath) {
        settings.edit().putString(PREFERENCE_REMOTE_PATH, remotePath.toString()).commit();
    }
    
	public static Project create(Remote remote, String name, RemoteFolder remoteFolder) {
		Project project = new Project(remote, name);
		project.setRemotePath(remoteFolder.getPath());
		return project;
	}

	public Remote getRemote() {
		return remote;
	}

	
    public void setOnline(boolean isOnline) {
        // TODO Auto-generated method stub       
    }

    public boolean isOnline() {
        // TODO Auto-generated method stub
        return false;
    }

}
