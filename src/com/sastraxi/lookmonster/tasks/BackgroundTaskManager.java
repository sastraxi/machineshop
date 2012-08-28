package com.sastraxi.lookmonster.tasks;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sastraxi.lookmonster.SmartList;
import com.sastraxi.machineshop.R;

public class BackgroundTaskManager {

	protected class ActionBarView extends LinearLayout {

		private static final String SEPARATOR = " · ";

		private ImageView indicator;
		private Animation clockwiseRefresh;
		private TextView textView;
		private boolean alreadySpinning = false;

        private int nActive = 0;

		public ActionBarView(boolean clickForDialog) {
			super(getActivity());
			setOrientation(HORIZONTAL);
			setPadding(0, 12, 0, 12);
			if (clickForDialog) {
				setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						if (nActive > 0) { 
					        showDialog(false);
						}
					}
				});
			}
			addUI();
		}
		
		@Override
		protected void onAttachedToWindow() {
		    super.onAttachedToWindow();
		    activeViews.add(this);
		}
		
		@Override
		protected void onDetachedFromWindow() {
		    super.onDetachedFromWindow();
		    activeViews.remove(this);
		}

		private void addUI() {
			LayoutInflater inflater = (LayoutInflater) getContext()
					.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);

			indicator = new ImageView(getContext());
			indicator.setImageResource(R.drawable.icon_refresh);
			addView(indicator, new LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT));

			clockwiseRefresh = AnimationUtils.loadAnimation(getContext(),
					R.anim.clockwise_refresh);
			clockwiseRefresh.setRepeatCount(Animation.INFINITE);

			textView = (TextView) inflater.inflate(R.layout.actionbar_textview, null);
			textView.setPadding(12, 6, 12, 6);
			addView(textView, new LayoutParams(LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT));

		}

		public void refresh() {
		    executeRateLimited(new Runnable() {
                public void run() {
                    refreshImpl();
                }
            });
		}
		    
		private void refreshImpl() {
		
			String currentText = "";

			nActive = 0;
			for (BackgroundTask task : tasks) {
				if (task.isActive()) {
					nActive++;
					if (task.showOnActionBar()) {
						String message = task.getActionBarMessage();
						if (message != null && message.length() > 0) {
							if (currentText.length() > 0)
								currentText += SEPARATOR;
							currentText += message;
						}
					}
				}
			}

			if (nActive > 0) {
				if (!alreadySpinning) {
			        indicator.startAnimation(clockwiseRefresh);
			        alreadySpinning = true;
				}
				if (currentText.length() == 0) {
					textView.setText(nActive + " tasks");
				} else {
					textView.setText(currentText);
				}
			} else {
			    alreadySpinning = false;
				indicator.clearAnimation();
				textView.setText("Inactive");
			}
			
		}

	}

	private Activity activity = null;
	private final SmartList<BackgroundTask> tasks = new SmartList<BackgroundTask>();
	protected final List<ActionBarView> activeViews = new ArrayList<ActionBarView>();
	
	private Dialog dialog = null;
	private BackgroundTaskAdapter adapter = null;
	private boolean isDialogBlocking;
	private Handler handler = null;

	public BackgroundTaskManager() {
	}

	public Activity getActivity() {
		return activity;
	}

	public void setActivity(Activity activity) {
		this.activity = activity;
		this.handler = new Handler(getActivity().getMainLooper());
	}

	public void addTask(BackgroundTask task) {
		tasks.add(task);
	}

	public void removeTask(BackgroundTask task) {
		tasks.remove(task);
	}

	/**
	 * Retrieves/creates a {@link View} suitable for display in a Honeycomb
	 * action bar (or perhaps even in ActionBarSherlock, though this has not
	 * been tested). The given view refreshes itself periodically.
	 */
	public View getActionBarView(boolean clickForDialog) {
		return new ActionBarView(clickForDialog);
	}

	/**
	 * Shows a dialog detailing the various completion states of background
	 * tasks managed by this {@link BackgroundTaskManager} instance.
	 * 
	 * @param isBlocking
	 *            if true, the dialog is not cancelable; it will stay in the
	 *            foreground until dismissed by the caller.
	 */
	public Dialog showDialog(boolean isBlocking) {

		// if the dialog is already showing but has a different
		// configuration, destroy it and re-create
		if (dialog != null) {
			if (isBlocking != isDialogBlocking) {
				dialog.dismiss();
			} else {
				return dialog;
			}
		}

		Builder builder = new AlertDialog.Builder(getActivity());
		if (isBlocking) {
			builder.setTitle("Waiting for Tasks");
			builder.setCancelable(false);
		} else {
			builder.setTitle("Active Tasks");
			builder.setCancelable(true);
			builder.setNegativeButton("Background",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							hideDialog();
						}
					});			
			builder.setOnCancelListener(new OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    hideDialog();
                }
            });
		}

		adapter = new BackgroundTaskAdapter(getActivity(), tasks);
		builder.setAdapter(adapter, null);

		isDialogBlocking = isBlocking;
		dialog = builder.create();
		dialog.show();
		return dialog;
	}
	
	public void hideDialog() {
		if (dialog != null) {
			dialog.dismiss();
			dialog = null;
			adapter = null;
		}
	}

	/**
	 * Called by {@link BackgroundTask}s.
	 */
	public void onProgress(final BackgroundTask task) {
		if (dialog != null) {
			executeRateLimited(new Runnable() {
				public void run() {
					adapter.onProgress(task);
				}
			});
		}
		for (ActionBarView view: activeViews) {
		    view.refresh();
		}
	}

	/**
	 * Executes a Runnable on the UI thread, but doesn't let Android get bogged
	 * down by the updates in case of being called too often.
	 * 
	 * @param runnable the code to execute on the UI thread.
	 */
	protected void executeRateLimited(Runnable runnable) {
		long currentTime = System.currentTimeMillis();
		if (lastTime == null || (currentTime - lastTime) > RATE_LIMIT) {
		    handler.post(runnable);
			lastTime = currentTime;
		}
		// FIXME: this does not work. It needs to schedule the next task to happen in
		// currentTime + (RATE_LIMIT - sinceLastTime) millseconds, or not if there's
		// already a "next" runnable queued. Needs
		// a map from Runnable to isNextScheduled()
	}

	private static final int RATE_LIMIT = 200; // 5 hz
	private Long lastTime = null;

}
