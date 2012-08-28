package com.sastraxi.machineshop.util;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ProgressMonitor;

import android.app.Activity;
import android.util.Log;

import com.sastraxi.lookmonster.ManagedTask;

public class GitTask extends ManagedTask<Git> implements ProgressMonitor {

	private int currentWorked = 0;
	private int totalWork = 0;

	public GitTask(Activity activity, String title, Callable<Git> command) {
		super(activity, title, command, true);
	}

	@Override
	public void start() {
		setAsProgressMonitor();
		super.start();
	}

	/**
	 * Tricky code that uses the setProgressMonitor method to work around the
	 * lack of a common superclass with the setProgressMonitor method.
	 */
	private void setAsProgressMonitor() {
		try {
			Method method = getCommand().getClass().getDeclaredMethod(
					"setProgressMonitor", ProgressMonitor.class);
			method.invoke(getCommand(), this);
		} catch (NoSuchMethodException e) {
			Log.w("GitTask.setAsProgressMonitor",
					"command doesn't implement setProgressMonitor -- you're on your own!");
		} catch (Exception e) {
			Log.d("GitTask.setAsProgressMonitor", Log.getStackTraceString(e));
		}
	}

	public void update(int completed) {
		currentWorked += completed;
		// Log.d("GitTask.update", currentWorked + "/" + totalWork);
		executeRateLimited(new Runnable() {
			public void run() {
				setProgress(totalWork, currentWorked, false);
			}
		});
	}

	public void start(int totalTasks) {
		// ignore total tasks.
	}

	public void endTask() {
		// handled in AsyncTask.onPostExecute,
		// as this will be on the AsyncTask's thread
		// and we want to do things on the UI thread.
		// Log.d("GitTask.endTask", "");
		return;
	}

	public void beginTask(final String taskMessage, int total) {
		// Log.d("GitTask.beginTask", "\"" + taskMessage + "\" total:" + total);
		currentWorked = 0;
		totalWork = total;

		executeRateLimited(new Runnable() {
			public void run() {
				if (totalWork == UNKNOWN) {
					setProgress(0, 0, true);
				} else {
					setProgress(totalWork, 0, false);
				}
				setMessage(taskMessage);
			}
		});
	}

	@Override
	public boolean isCancelled() {
		return super.isCancelled();
	}

};
