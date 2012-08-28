package com.sastraxi.lookmonster;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.sastraxi.machineshop.R;

public class ManagedTask<Passable> {

	public interface FinishAction {
		public abstract void doAction();

		public abstract String getVerb();
	};

	public interface CompleteCallback<T> {
		public void onCompletion(T t);
	};

	public interface CancelCallback {
		public void onCancel();
	}

	private static final int NOTIFICATION_ID = 900;
	private static final int FINISHED_NOTIFICATION_ID = 901;

	private final AlertDialog dialog;
	private final ProgressBar progressBar;
	private final TextView message;

	private final String title;
	private final Callable<Passable> command;
	private CompleteCallback completeCallback = null;
	private CancelCallback cancelCallback = null;
	private FinishAction finishAction = null;

	private boolean cancelPressed = false;
	private boolean finished = false;

	protected Notification notification = null;

	private final Activity activity;

	protected Callable<Passable> getCommand() {
		return command;
	}

	/**
	 * 
	 * @param activity
	 * @param title
	 * @param command
	 */
	public ManagedTask(final Activity activity, String title,
			Callable<Passable> command, boolean cancelable) {
		this.activity = activity;
		this.title = title;
		this.command = command;
		register(this);

		LayoutInflater inflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View view = inflater.inflate(R.layout.progress_alert, null);
		progressBar = (ProgressBar) view.findViewById(R.id.progress);
		message = (TextView) view.findViewById(R.id.message);
		Builder builder = new AlertDialog.Builder(activity).setView(view)
				.setTitle(title)
				.setNeutralButton("Background", new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						showOngoing();
					}
				});

		if (cancelable) {
			builder.setNegativeButton("Cancel", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					cancelPressed = true;
					if (cancelCallback != null) {
						cancelCallback.onCancel();
					}
				}
			});
		}

		dialog = builder.create();
	}

	protected void setProgress(int max, int progress, boolean indeterminate) {
		if (notification != null) {

			NotificationManager notificationManager = (NotificationManager) activity
					.getSystemService(Context.NOTIFICATION_SERVICE);

			notification.contentView.setProgressBar(R.id.progress, max,
					progress, indeterminate);
			notificationManager.notify(tag, NOTIFICATION_ID, notification);

		} else {
			this.progressBar.setMax(max);
			this.progressBar.setProgress(progress);
			this.progressBar.setIndeterminate(indeterminate);
		}
	}

	protected void setMessage(String message) {
		if (notification != null) {

			NotificationManager notificationManager = (NotificationManager) activity
					.getSystemService(Context.NOTIFICATION_SERVICE);

			notification.contentView.setTextViewText(R.id.message, message);
			notificationManager.notify(tag, NOTIFICATION_ID, notification);

		} else {
			this.message.setText(message);
		}
	}

	/**
	 * Wire in code to be run when the command completes successfully. Note that
	 * this code runs on the UI thread.
	 */
	public void setCompleteCallback(CompleteCallback completeCallback) {
		this.completeCallback = completeCallback;
	}

	/**
	 * Wire in code to be run when the command is cancelled. Note that this code
	 * runs on the UI thread.
	 */
	public void setCancelCallback(CancelCallback cancelCallback) {
		this.cancelCallback = cancelCallback;
	}

	/**
	 * Wire in code that the user can optionally run after this task is
	 * complete. If this is set, the dialog will no longer automatically dismiss
	 * itself, and the buttons will change to reflect the new option available.
	 * Additionally, clicking the final notification (if the task is completes
	 * while in the background) will launch this action.
	 * 
	 * Note that this code runs on the UI thread.
	 */
	public void setFinishAction(FinishAction finishAction) {
		this.finishAction = finishAction;
	}

	public void start() {
		progressBar.setIndeterminate(true);
		dialog.setCanceledOnTouchOutside(false);
		showDialog();

		new AsyncTask<Void, Void, Passable>() {

			@Override
			protected Passable doInBackground(Void... params) {
				try {
					return command.call();
				} catch (Exception e) {
					Log.e("ManagedTask.start:AsyncTask.doInBackground",
							Log.getStackTraceString(e));
					return null;
				}
			}

			@Override
			protected void onPostExecute(Passable result) {
				try {
					finish(result);
				} catch (Exception e) {
					Log.e("ManagedTask.start:AsyncTask.onPostExecute",
							Log.getStackTraceString(e));
				}
			}

		}.execute();
	}

	protected void finish(Passable result) {
		finished = true;
		if (completeCallback != null) {
			completeCallback.onCompletion(result);
		}
		if (notification != null) {
			showFinishedNotification();
		} else {
			if (finishAction != null) {
				showFinishedDialog();
			} else {
				dialog.dismiss();
				deregister(ManagedTask.this);
			}
		}
	}

	private boolean isFinished() {
		return finished;
	}

	private void showDialog() {
		if (notification != null) {

			NotificationManager notificationManager = (NotificationManager) activity
					.getSystemService(Context.NOTIFICATION_SERVICE);

			notificationManager.cancel(tag, NOTIFICATION_ID);

		}
		dialog.show();
	}

	private void showOngoing() {

		dialog.dismiss();

		NotificationManager notificationManager = (NotificationManager) activity
				.getSystemService(Context.NOTIFICATION_SERVICE);

		Intent notificationIntent = new Intent(activity, activity.getClass());
		notificationIntent.putExtra(INTENT_EXTRA_TASK_KEY, this.id);

		// FIXME need to move R.layout.progress_notification from machineshop to lookmonster... somehow!?
		RemoteViews contentView = new RemoteViews(activity.getPackageName(),
				R.layout.progress_notification);
		int icon = android.R.drawable.stat_sys_download;

		notification = new Notification.Builder(activity)
				.setSmallIcon(icon)
				.setTicker(title)
				.setOngoing(true)
				.setContentIntent(
						PendingIntent.getActivity(activity, 0,
								notificationIntent, 0)).setContent(contentView)
				.setWhen(System.currentTimeMillis()).setAutoCancel(true)
				.getNotification();
		notification.flags |= Notification.FLAG_NO_CLEAR;

		contentView.setTextViewText(R.id.message, message.getText());
		contentView.setProgressBar(R.id.progress, progressBar.getMax(),
				progressBar.getProgress(), progressBar.isIndeterminate());
		contentView.setImageViewResource(R.id.image, icon);

		notificationManager.notify(tag, NOTIFICATION_ID, notification);

	}

	private void showFinishedNotification() {
		NotificationManager notificationManager = (NotificationManager) activity
				.getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.cancel(tag, NOTIFICATION_ID);

		Intent deleteIntent = new Intent(activity, TaskDeleteReceiver.class);
		deleteIntent.putExtra(INTENT_EXTRA_TASK_KEY, this.id);

		Intent notificationIntent = new Intent(activity, activity.getClass());
		notificationIntent.putExtra(INTENT_EXTRA_TASK_KEY, this.id);

		String contentText;
		if (finishAction == null) {
			contentText = "Select to open Machine Shop";
		} else {
			contentText = "Select to " + finishAction.getVerb();
		}

		notification = new Notification.Builder(activity)
				.setSmallIcon(android.R.drawable.stat_sys_download_done)
				.setTicker(title + " Complete")
				.setContentTitle(title + " Complete")
				.setContentText(contentText)
				.setContentIntent(
						PendingIntent.getActivity(activity, 0,
								notificationIntent, 0))
				.setDeleteIntent(
						PendingIntent
								.getBroadcast(activity, 0, deleteIntent, 0))
				.setWhen(System.currentTimeMillis()).setAutoCancel(true)
				.getNotification();

		notificationManager.notify(tag, FINISHED_NOTIFICATION_ID, notification);
	}

	private void showFinishedDialog() {
		message.setText("Finished");
		progressBar.setVisibility(ProgressBar.GONE);
		Button button = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
		button.setText(finishAction.getVerb());
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finishAction.doAction();
			}
		});
		button.invalidate();
	}

	public boolean isCancelled() {
		return cancelPressed;
	}

	/**
	 * We rate-limit to 0.5 seconds.
	 */
	private static final long RATE_LIMIT = 500;
	private Long lastTime = null;

	/**
	 * Executes a Runnable on the UI thread, but doesn't let Android get bogged
	 * down by the updates in case of being called too often.
	 * 
	 * @param runnable
	 *            the code to execute on the UI thread.
	 */
	protected void executeRateLimited(Runnable runnable) {
		long currentTime = System.currentTimeMillis();
		if (lastTime == null || (currentTime - lastTime) > RATE_LIMIT) {
			activity.runOnUiThread(runnable);
			lastTime = currentTime;
		}
	}

	private static final String INTENT_EXTRA_TASK_KEY = "com.sastraxi.lookmonster.ManagedTask.task_key";
	private static final Map<Integer, ManagedTask> all_tasks = new HashMap<Integer, ManagedTask>();
	private static int currentID = 0;
	private String tag;
	private Integer id;

	private static final void register(ManagedTask task) {
		task.tag = String.valueOf(currentID);
		task.id = currentID;
		all_tasks.put(task.id, task);
		currentID += 1;
	}

	private static final void deregister(ManagedTask task) {
		all_tasks.remove(task.id);
	}

	public static final void handleIntent(Activity activity) {
		int key = activity.getIntent().getIntExtra(INTENT_EXTRA_TASK_KEY, -1);
		if (key != -1) {
			ManagedTask task = all_tasks.get(key);
			if (task.isFinished()) {
				if (task.finishAction != null) {
					task.finishAction.doAction();
				}
			} else {
				task.showDialog();
			}
		}
	}

	public class TaskDeleteReceiver extends BroadcastReceiver {

		public TaskDeleteReceiver() {
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			int key = intent.getIntExtra(INTENT_EXTRA_TASK_KEY, -1);
			ManagedTask task = all_tasks.get(key);
			if (task == null) {
				Log.w("ManagedTask",
						"Got delete intent from non-existant task with id="
								+ key);
			} else {
				deregister(task);
			}
		}

	}

};
