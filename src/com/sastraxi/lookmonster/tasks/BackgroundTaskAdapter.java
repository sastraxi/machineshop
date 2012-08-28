package com.sastraxi.lookmonster.tasks;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.sastraxi.lookmonster.SmartList;
import com.sastraxi.lookmonster.SmartListAdapter;
import com.sastraxi.machineshop.adapters.ListLayouts;

public class BackgroundTaskAdapter extends
		SmartListAdapter<BackgroundTask, BackgroundTask> {

	public BackgroundTaskAdapter(Context context,
			SmartList<BackgroundTask> backingList) {
		super(context, backingList);
	}

	@Override
	protected String getCategory(BackgroundTask item) {
		return null;
	}

	@Override
	protected List<BackgroundTask> expandItems(BackgroundTask backingItem) {
		ArrayList<BackgroundTask> list = new ArrayList<BackgroundTask>();
		if (backingItem.isActive()) {
		    list.add(backingItem);
		}
		return list;
	}

	@Override
	protected View getListItemView(int position, BackgroundTask task,
			View convertView, ViewGroup parent) {
		return ListLayouts.progress(
				getLayoutInflater(),
				convertView,
				parent,
				task.getMessage(),
				task.isIndeterminate(), task.getProgress(), task.getMaxProgress(),
				task.isCancelable());
	}

	@Override
	protected int getClickMode(int position, BackgroundTask item) {
		return CLICK_MODE_NOT_CLICKABLE;
	}

	@Override
	protected int getMaxCheckedItems() {
		return 0;
	}

	public void onProgress(BackgroundTask task) {
	    recreateItemList();
	    /*
		int position = findItemPosition(task);
		if (position != -1) {
			View progressView = getViewAtPosition(position);
			if (progressView != null) {
				// because we're using ListLayouts which re-uses views
				// we can just re-call the view function which will re-use
				// the view and just update the child views with new info.
				getListItemView(position, task, progressView, null);
			} else {
				// OK if it's null, just means not visible right now!
				// As we're just here to update the UI, we're done.
			}
		} else {
			Log.w("BackgroundTasksAdapter.refreshTask", "Couldn't find task "
					+ task.toString());
		}
		*/
	}

}
