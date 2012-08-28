package com.sastraxi.lookmonster;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.sastraxi.machineshop.R;

public abstract class GroupedAdapter<T> extends ArrayAdapter<Object> {

	// TODO don't extend ArrayAdapter but delegate to it,
	// instead implement BaseAdapter

	private final boolean isSpinner;
	private List<T> backing_list;

	protected class SectionHeader {
		public final String category;
		public final String extra;

		public SectionHeader(String category, String extra) {
			this.category = category;
			this.extra = extra;
		}
	};

	public GroupedAdapter(Context context, List<T> backing_list,
			boolean isSpinner) {
		super(context, 0);
		this.isSpinner = isSpinner;
		this.backing_list = backing_list;
		refresh();
	}

	/**
	 * If the backing list has changed, call this function to correctly update
	 * the UI.
	 */
	public void setInternalList(List<T> backing_list) {
		this.backing_list = backing_list;
		refresh();
	}

	/**
	 * If the backing list's contents have changed but the list itself has not,
	 * call this function to correctly update the UI.
	 */
	public final void refresh() {
		clear();
		// sort the list of items by category.
		Collections.sort(backing_list, new Comparator<T>() {
			public int compare(T lhs, T rhs) {
				return GroupedAdapter.this.compareCategories(lhs, rhs);
			}
		});
		// add items and section headers to the backing store.
		SectionHeader lastSection = null;
		for (T item : backing_list) {
			SectionHeader section = new SectionHeader(getCategory(item),
					getCategoryExtra(item));
			if (lastSection == null
					|| (section.category == null && lastSection.category == null)
					|| !section.category.equals(lastSection.category)
					|| (section.extra == null && lastSection.extra == null)
					|| !section.extra.equals(lastSection.extra)) {
				// TODO: add equals() and hashCode() methods to SectionHeader
				if (section.category != null) { // don't create a section for
												// the null category
					add(section);
				}
			}
			add(item);
			lastSection = section;
		}
	}

	/**
	 * Return a category to group items based on. Category headers will be
	 * created with this string as their text. Your implementation must always
	 * return the same string for a given item.
	 */
	protected abstract String getCategory(T item);

	/**
	 * The section headers can optionally provide another piece of information
	 * at the right side of the header view--we refer to this as "extra". For
	 * example, the Gmail app shows your email address at the right-hand side of
	 * the RECENT header in the Action Bar's dropdown spinner.
	 * 
	 * The default implementation simply returns null, so nothing is required
	 * from your subclass if you do not wish to use this feature.
	 */
	protected String getCategoryExtra(T item) {
		return null;
	}

	/**
	 * The default implementation sorts nulls first, then lexicographically on
	 * the String returned by getCategory(), and then on the String returned by
	 * getExtra() if the categories are identical.
	 */
	protected int compareCategories(T lhs, T rhs) {
		String lhsCategory = getCategory(lhs);
		String rhsCategory = getCategory(rhs);
		if (lhsCategory == null)
			return -1;
		if (rhsCategory == null)
			return 1;
		int cmp = lhsCategory.compareTo(rhsCategory);
		if (cmp != 0)
			return cmp;

		String lhsExtra = getCategoryExtra(lhs);
		String rhsExtra = getCategoryExtra(rhs);
		if (lhsExtra == null)
			return -1;
		if (rhsExtra == null)
			return 1;
		return lhsExtra.compareTo(rhsExtra);
	}

	/**
	 * Note: we only support 2 view types. If you would like your subclass to
	 * have more View types, you must re-implement both getViewTypeCount() and
	 * getItemViewType(int).
	 */
	public int getViewTypeCount() {
		return 2;
	}

	/**
	 * Note: we only support 2 view types. If you would like your subclass to
	 * have more View types, you must re-implement both getViewTypeCount() and
	 * getItemViewType(int).
	 * 
	 * Make sure to call this super-implementation so that section headers get
	 * the right view type (0).
	 */
	public int getItemViewType(int position) {
		if (getItem(position) instanceof GroupedAdapter.SectionHeader) {
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	public boolean isEnabled(int position) {
		return !(getItem(position) instanceof GroupedAdapter.SectionHeader);
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@SuppressWarnings("unchecked")
	protected final View forList(int position, View convertView,
			ViewGroup parent) {
		Object item = getItem(position);

		if (item instanceof GroupedAdapter.SectionHeader) {
			return createHeaderViewForList((SectionHeader) item, convertView,
					parent);
		} else {
			return createViewForList((T) item, convertView, parent);
		}
	}

	@SuppressWarnings("unchecked")
	protected final View forSpinner(int position, View convertView,
			ViewGroup parent) {
		Object item = getItem(position);

		if (item instanceof GroupedAdapter.SectionHeader) {
			return createEmptyViewForSpinner(convertView, parent);
		} else {
			return createViewForSpinner((T) item, convertView, parent);
		}
	}

	@Override
	public final View getView(int position, View convertView, ViewGroup parent) {
		if (isSpinner) {
			return forSpinner(position, convertView, parent);
		} else {
			return forList(position, convertView, parent);
		}
	}

	@Override
	public final View getDropDownView(int position, View convertView,
			ViewGroup parent) {
		return forList(position, convertView, parent);
	}

	/**
	 * Create a view The default implementation emulates Android 4+ section
	 * headers ala. the Gmail app.
	 */
	protected View createHeaderViewForList(SectionHeader item,
			View convertView, ViewGroup parent) {
		Context context = parent.getContext();

		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View view = inflater.inflate(R.layout.listitem_sectionheader, parent,
				false);
		TextView textView = (TextView) view.findViewById(R.id.category);
		textView.setText(item.category.toUpperCase());

		TextView descriptionView = (TextView) view.findViewById(R.id.extra);
		descriptionView.setText(item.extra);

		return view;
	}

	/**
	 * Create a view for an item when displayed as an item in a ListView.
	 * Implement like you would implement Adapter.getView.
	 */
	protected abstract View createViewForList(T item, View convertView,
			ViewGroup parent);

	/**
	 * Create a view for an item when displayed as the selected item in a
	 * Spinner. Implement like you would implement Adapter.getView.
	 */
	protected abstract View createViewForSpinner(T item, View convertView,
			ViewGroup parent);

	/**
	 * The default implementation at least saves you the trouble of creating
	 * this if you know you won't need it.
	 */
	protected View createEmptyViewForSpinner(View convertView, ViewGroup parent) {
		Context context = parent.getContext();
		TextView view = new TextView(context);
		view.setText("");
		return view;
	}

	/**
	 * Removes and re-inserts one item in the list. Avoids re-creation of the
	 * whole list if only one item has changed.
	 */
	public final void notifyItemModified(T item) {
		int position = getPosition(item);
		remove(item);
		insert(item, position);
	}

}
