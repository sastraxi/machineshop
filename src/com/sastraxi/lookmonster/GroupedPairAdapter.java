package com.sastraxi.lookmonster;

import java.util.List;

import com.sastraxi.machineshop.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Shows key/value when in the list, key/category when in the spinner.
 * 
 * @author Cameron Gorrie
 * @param <T>
 *            A class which a pair can be extracted from, see
 *            {@link extractPair}.
 */
public abstract class GroupedPairAdapter<T> extends GroupedAdapter<T> {

	protected int getListLayout() {
		return R.layout.listitem_textandnumber;
	}

	protected int getSpinnerLayout() {
		return R.layout.listitem_spinner;
	}

	protected int getKeyId() {
		return R.id.key;
	}

	protected int getValueId() {
		return R.id.value;
	}

	protected int getCategoryId() {
		return R.id.category;
	}

	protected int getProgressId() {
		return R.id.progress;
	}

	public GroupedPairAdapter(Context context, List<T> items, boolean isSpinner) {
		super(context, items, isSpinner);
	}

	/**
	 * Return a Pair<String, String> (a key/value mapping). If the second string
	 * is null, denotes a "loading" state.
	 */
	protected abstract Pair<String, String> extractPair(T item);

	protected View createViewForList(T item, View convertView, ViewGroup parent) {
		Pair<String, String> pair = extractPair(item);

		// TODO set background colour to blue/foreground colour to white
		// if the list item is selected.

		Context context = parent.getContext();
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View view = inflater.inflate(getListLayout(), parent, false);

		TextView keyView = (TextView) view.findViewById(getKeyId());
		keyView.setText(pair.getKey());

		if (pair.getValue() != null) {
			view.findViewById(getProgressId()).setVisibility(View.GONE);

			TextView valueView = (TextView) view.findViewById(getValueId());
			valueView.setVisibility(View.VISIBLE);
			valueView.setText(pair.getValue());
		} else {
			view.findViewById(getProgressId()).setVisibility(View.VISIBLE);
			view.findViewById(getValueId()).setVisibility(View.GONE);
		}

		return view;
	}

	protected View createViewForSpinner(T item, View convertView,
			ViewGroup parent) {
		Pair<String, String> pair = extractPair(item);

		Context context = parent.getContext();
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View view = inflater.inflate(getSpinnerLayout(), parent, false);

		TextView keyView = (TextView) view.findViewById(getKeyId());
		keyView.setText(pair.getKey());

		TextView categoryView = (TextView) view.findViewById(getCategoryId());
		categoryView.setText(getCategory(item));

		return view;
	}

}
