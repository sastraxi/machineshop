package com.sastraxi.machineshop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.sastraxi.lookmonster.SmartList;
import com.sastraxi.lookmonster.tasks.BackgroundTaskManager;
import com.sastraxi.machineshop.remote.Remote;

public class MachineShopApplication extends Application implements Observer {

    private static final String PREFERENCE_REMOTE_NAMES = "remote_names";
    
	private BackgroundTaskManager backgroundTaskManager = new BackgroundTaskManager();
	private SmartList<Remote> remotes = null;
    private Activity currentActivity = null;
    private SharedPreferences settings;
	
	private void init() {
	    
		getRootFolder().mkdirs();		
		
		this.settings = getSharedPreferences(getPreferencesName(), 0);
        PreferenceManager.setDefaultValues(this, R.xml.main, false);
        
        // load remotes from preference string set.
        this.remotes = new SmartList<Remote>();
        for (String remoteName: this.settings.getStringSet(PREFERENCE_REMOTE_NAMES, new HashSet<String>())) {
            this.remotes.add(new Remote(this, remoteName));
        }
        this.remotes.addObserver(this);

	}

	private String getPreferencesName() {
        return "main";
    }

	public SharedPreferences getPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(this);
	}

	public SmartList<Remote> getRemotes() {
		return remotes;
	}

	public File getRootFolder() {
		// TODO config/persist
		File sdCard = Environment.getExternalStorageDirectory();
		File rootFolder = new File(sdCard.getAbsolutePath() + "/machineshop");
		rootFolder.mkdirs(); // so that it's always a directory.
		return rootFolder;
	}

	/**
	 * Load or generate a RSA keypair to use as a client for the given JSch.
	 */
	public boolean registerKeyPair(JSch jSch) {
		new File(getRootFolder().getAbsolutePath() + "/.ssh").mkdirs();

		File privateKey = new File(getRootFolder().getAbsolutePath() + "/.ssh/id_rsa");
		File publicKey = new File(getRootFolder().getAbsolutePath() + "/.ssh/id_rsa.pub");
		if (!privateKey.exists() || !publicKey.exists()) {
			try {
				KeyPair keyPair = KeyPair.genKeyPair(jSch, KeyPair.RSA);
				keyPair.writePrivateKey(privateKey.getAbsolutePath());
				keyPair.writePublicKey(publicKey.getAbsolutePath(),
						"Machine Shop");
			} catch (JSchException e) {
				Log.e("genKeyPair(RSA)", Log.getStackTraceString(e));
				return false;
			} catch (FileNotFoundException e) {
				Log.e("genKeyPair(RSA)", Log.getStackTraceString(e));
				return false;
			} catch (IOException e) {
				Log.e("genKeyPair(RSA)", Log.getStackTraceString(e));
				return false;
			}
		}

		try {
			jSch.addIdentity(privateKey.getAbsolutePath());
			return true;
		} catch (JSchException e) {
			Log.w("jSch.addIdentity", Log.getStackTraceString(e));
			return false;
		}
	}

	public InputStream getPublicKeyStream() {
		try {
			File file = new File(getRootFolder().getAbsolutePath()
					+ "/.ssh/id_rsa.pub");
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			Log.wtf("app.getPublicKeyString", Log.getStackTraceString(e));
			return null;
		}
	}

	public BackgroundTaskManager getBackgroundTaskManager() {
		return backgroundTaskManager;
	}

	/**
	 * Called by BaseActivity.
	 * This will be the first time we get into MachineShopApplication,
	 * so do our init() now.
	 */
	public void setCurrentActivity(Activity activity) {
        if (settings == null) init();

        this.currentActivity = activity;
		for (Remote remote : getRemotes()) {
			remote.setManager(getBackgroundTaskManager());
		}
		getBackgroundTaskManager().setActivity(activity);
	}	
	
	public Activity getCurrentActivity() {
	    return currentActivity;
	}
	
	public void addRemote(Remote remote) {
        remote.setManager(getBackgroundTaskManager());
	    remotes.add(remote);
	}

    /**
     * Updates the preferences file when we mess with the remote list.
     */
    public void update(Observable observable, Object data) {
        
        if (observable != this.remotes) {
            Log.w("MachineShopApplication.update", "got unknown observable" + observable.toString());
            return;
        }
        
        Set<String> remoteNameSet = new HashSet<String>();
        for (Remote remote: this.remotes) {
            remoteNameSet.add(remote.getName());
        }
        settings.edit().putStringSet(PREFERENCE_REMOTE_NAMES, remoteNameSet).commit();
        
    }

    public Remote getRemote(String name) {
        for (Remote remote: remotes) {
            if (remote.getName().equals(name)) {
                return remote;
            }
        }
        return null;
    }

}
