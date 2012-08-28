package com.sastraxi.machineshop;

import android.app.Activity;

import com.sastraxi.lookmonster.ManagedTask;

public class BaseActivity extends Activity {

	public MachineShopApplication getApp() {
		return (MachineShopApplication) getApplicationContext();
	}

	@Override
	protected void onStart() {
		super.onStart();

		// mark this as the foreground activity for the app.
		getApp().setCurrentActivity(this);

		// run actions if we just finished a task and
		// were brought here by clicking a notification.
		ManagedTask.handleIntent(this);
	}
	
}
