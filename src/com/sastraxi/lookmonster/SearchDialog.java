package com.sastraxi.lookmonster;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;

/**
 * A dialog suitable for searching online sources, and selecting from the
 * objects returned. Caches query results for speed.
 */
public abstract class SearchDialog<T> extends AlertDialog {

	public interface PositiveActionListener<U> {
		public void onPositiveAction(Set<U> items);
	}

	protected static final long AUTO_SEARCH_DELAY = 1000;

	private Handler handler = new Handler();
	private final Runnable autoSearchRunnable;

	private SearchView searchView;
	private ArrayAdapter<T> adapter;
	private PositiveActionListener<T> positiveActionListener = null;
	private Set<T> selectedItems = new HashSet<T>();
	private String currentText = "";

	private ListView listView;

	private final String thingSearched;

	private final String positiveAction;

	private Button positiveButton;

	public SearchDialog(Context context, String thingSearched,
			String positiveAction) {
		super(context, THEME_HOLO_DARK);
		this.thingSearched = thingSearched;
		this.positiveAction = positiveAction;
		this.autoSearchRunnable = new Runnable() {
			public void run() {
				submitSearch(currentText);
			}
		};

		adapter = new ArrayAdapter<T>(context, 0) {
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {

				final T item = getItem(position);
				boolean isSelected = selectedItems.contains(item);

				View view = SearchDialog.this.renderView(item, convertView,
						parent);
				if (isSelected) {
					view.setBackgroundResource(R.color.holo_blue_bright);
				} else {
					view.setBackgroundResource(R.color.transparent);
				}

				CheckBox checkBox = getCheckboxFromView(view);
				checkBox.setChecked(isSelected);
				checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							selectedItems.add(item);
						} else {
							selectedItems.remove(item);
						}
						positiveButton.setText(SearchDialog.this.positiveAction
								+ " (" + selectedItems.size() + ")");
					}
				});

				return view;

			}
		};

		// attach a search bar where the title would normally be.
		searchView = new SearchView(getContext());
		searchView.setIconifiedByDefault(false);
		searchView.setActivated(true);
		searchView.setQueryHint("Search " + thingSearched);
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			public boolean onQueryTextSubmit(String query) {
				handler.removeCallbacks(autoSearchRunnable);
				Log.i("HELP", "ME");
				submitSearch(query);
				return true;
			}

			public boolean onQueryTextChange(String newText) {
				handler.removeCallbacks(autoSearchRunnable);
				currentText = newText;
				if (newText == "") {
					clearSearch();
				} else {
					// handler.postDelayed(autoSearchRunnable,
					// AUTO_SEARCH_DELAY);
				}
				return true;
			}
		});
		searchView.setQuery("bim", true);

		// requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setCustomTitle(searchView);

		// attach a checkable list.
		listView = new ListView(getContext());
		// listView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
		// LayoutParams.WRAP_CONTENT));
		listView.setId(android.R.id.list);
		listView.setAdapter(adapter);
		listView.setDivider(getContext().getResources().getDrawable(
				android.R.drawable.divider_horizontal_bright));
		// FIXME: using Machine Shop resources in LookMonster
		listView.setBackgroundResource(android.R.color.background_light);
		listView.setEmptyView(new ProgressBar(getContext()));
		setView(listView);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setInverseBackgroundForced(true);
		setCanceledOnTouchOutside(false);

		super.onCreate(savedInstanceState);

		// attach two buttons at the bottom.
		setButton(BUTTON_POSITIVE, positiveAction, new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (positiveActionListener != null) {
					positiveActionListener.onPositiveAction(selectedItems);
				}
			}
		});
		setButton(BUTTON_NEGATIVE, "Cancel", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				SearchDialog.this.dismiss();
			}
		});
		positiveButton = getButton(BUTTON_POSITIVE);
	}

	public void setOnPositiveActionListener(
			PositiveActionListener<T> positiveActionListener) {
		this.positiveActionListener = positiveActionListener;
	}

	protected Map<String, List<T>> cachedQueries = new HashMap<String, List<T>>();

	protected final void submitSearch(final String query) {
		adapter.clear();
		if (!cachedQueries.containsKey(query)) {
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					cachedQueries.put(query, doSearch(query));
					return null;
				}

				@Override
				protected void onPostExecute(Void result) {
					adapter.addAll(cachedQueries.get(query));
				}
			}.execute();
		} else {
			adapter.addAll(cachedQueries.get(query));
		}
	}

	protected final void clearSearch() {
		adapter.clear();
		adapter.addAll(selectedItems);
	}

	protected abstract List<T> doSearch(String query);

	/**
	 * 
	 * @param item
	 * @param convertView
	 * @param parent
	 * @return
	 */
	protected abstract View renderView(T item, View convertView,
			ViewGroup parent);

	protected abstract CheckBox getCheckboxFromView(View view);

}
