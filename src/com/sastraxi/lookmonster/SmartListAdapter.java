package com.sastraxi.lookmonster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.ListView;

import com.sastraxi.machineshop.adapters.ListLayouts;
import com.sastraxi.machineshop.adapters.RemoteListItem;

/**
 * The BaseAdapter that should have come with android.
 * 
 * Combines a backing SmartList (so that the list can refresh itself when data
 * changes), a light-weight category header implementation (just implement
 * {@link SmartListAdapter#setCategories}), and a separation between backing
 * list and item list (so that model and view no longer enforce a one-to-one
 * relationship).
 * 
 * Important: you must use {@link SmartListAdapter#setAsAdapterOn(ListView)}
 * instead of {@link ListView#setAdapter(android.widget.ListAdapter)}.
 * 
 * {@code ListItem} should implement equals in a sane way, so that items can
 * remain checked even if {@link SmartListAdapter#recreateItemList()} is called.
 * 
 * @author Cameron Gorrie
 * 
 * @param <BackingItem>
 *            (super)type of backing list items.
 * @param <ListItem>
 *            (super)type of items that will become Views in the list.
 */
public abstract class SmartListAdapter<BackingItem, ListItem> extends
		BaseAdapter implements Observer, OnItemClickListener {

	// TODO: create getMinItemsChecked, so we can have lists that must have
	// e.g. at least 1 item checked. Impl.: after recreating items, if there
	// are m items checked and m < n, just check the first unchecked (n-m)
	// items!

	public class Category {
		public final String text;

		public Category(String text) {
			this.text = text;
		}
	}

	public static final int CLICK_MODE_NOT_CLICKABLE = 0;
	public static final int CLICK_MODE_INSTANTANEOUS = 1;
	public static final int CLICK_MODE_CHECKABLE = 2;

	public static final int NO_LIMIT = Integer.MAX_VALUE;

	private final SmartList<BackingItem> backingList;
	private final Context context;

	private final LinkedList<Integer> checkedItems = new LinkedList<Integer>();

	private List<Object> itemList = new ArrayList<Object>(); // category headers
																// are Category
																// objects.
	private ListView listView;
	private LayoutInflater layoutInflater;

	public SmartListAdapter(Context context, SmartList<BackingItem> backingList) {
		super();
		this.context = context;
		this.layoutInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.backingList = backingList;
		if (autoUpdate()) {
		    this.getBackingList().addObserver(this);
		}
		recreateItemList();
	}

	/**
	 * Turn a backing item into its representation in the list -- 0 or more
	 * items. Splits data model from view.
	 * 
	 * @param backingItem
	 *            the item to expand.
	 * @return the list of items
	 */
	protected abstract List<ListItem> expandItems(BackingItem backingItem);

	/**
	 * Override and return false if you want a one-to-one relationship between
	 * BackingItems and ListItems. ListItem must be assignable from BackingItem.
	 * Consider using {@link SimplerSmartListAdapter} instead.
	 */
	protected boolean useExpander() {
		return true;
	}

	/**
	 * 
	 * Return {@code null} to disable categories for a specific item, or for
	 * every item to disable categories altogether.
	 */
	protected abstract String getCategory(ListItem item);

	/**
	 * Create a view for the given {@code ListItem}. Please use the convertView
	 * paradigm; we keep a position-View cache and if there are many list items
	 * one's device could run out of memory rather quickly.
	 */
	protected abstract View getListItemView(int position, ListItem item,
			View convertView, ViewGroup parent);

	/**
	 * Return one of the following: - {@code CLICK_MODE_NOT_CLICKABLE}: the
	 * given item does not respond to clicks. Do not allow list item Views to
	 * change to PRESSED state. - {@code CLICK_MODE_INSTANTANEOUS}: the given
	 * item responds to instantaneous clicks. The item will transition to the
	 * PRESSED state, then off again. A call to
	 * {@link SmartListAdapter#onInstantaneousClick(int, Object, View)} will
	 * follow any clicks on this item's View. - {@code CLICK_MODE_CHECKABLE}:
	 * the given item can be toggled on and off. The maximum number of items
	 * toggled on at any given time is governed by
	 * {@link SmartListAdapter#getMaxCheckedItems()}. A call to
	 * {@link SmartListAdapter#onToggleOn(int, Object, View)} will follow any
	 * clicks on this item's View.
	 */
	protected abstract int getClickMode(int position, ListItem item);

	/**
	 * Return the maximum number of checked items at one time. 0 means no items
	 * can be checked. 1 means only one item can be checked (as if one item were
	 * currently "selected"). NO_LIMIT means any number of items can be checked.
	 */
	protected abstract int getMaxCheckedItems();

	/**
	 * Override to handle click events for clickable (but not checkable) items.
	 */
	public void onInstantaneousClick(int position, ListItem item, View view) {
	}

	/**
	 * If a list item does not implement Checkable, you can override this to
	 * provide your own custom toggle-on behaviour.
	 * 
	 * View may be null if check was called programmatically on an item that is
	 * not currently visible.
	 */
	public void onToggleOn(int position, ListItem item, View view) {
	}

	/**
	 * If a list item does not implement Checkable, you can override this to
	 * provide your own custom toggle-off behaviour.
	 * 
	 * If caused by reaching {@link SmartListAdapter#getMaxCheckedItems()}, will
	 * be called before the matching call to
	 * {@link SmartListAdapter#onToggleOn(int, Object, View)}.
	 * 
	 * {@code OldView} may be null, if the list item's view is currently
	 * off-screen (i.e. has been re-used).
	 */
	public void onToggleOff(int position, ListItem item, View view) {
	}

	/**
	 * Return true if the user's request to (un)check this item by clicking on
	 * it should go through. This will make the item (un)checked if it
	 * implements Checkable, and onToggle(On|Off) will fire.
	 * 
	 * If false, onInstantaneousClick will be called instead.
	 * 
	 * This method is not called if the item is checked programmatically. A use
	 * case for this is a file list, where clicking an item opens it, clicking
	 * it again brings it to the front, and clicking an X in the item itself
	 * un-checks it.
	 */
	public boolean allowClickCheckChanges(int position, ListItem item) {
		return true;
	}

	/**
	 * Override and sort if you'd like the list of items in a different order
	 * than they normally are in. Return null for no sorting.
	 */
	protected Comparator<? super ListItem> getComparator() {
		return null;
	}

	/**
	 * If your {@code View} does not implement Checkable, you can override this
	 * to update the UI of the view yourself. Otherwise, you're all set.
	 * 
	 * Note that best practice is to use something like
	 * {@CheckableRelativeLayout} and state lists to
	 * modify appearance, all in XML. However, sometimes you just want the
	 * quick-and-dirty that works, and for all I care you could set background
	 * colours, check CheckBoxes, whatever! Just don't let the purists see you.
	 */
	public void updateUIForChecked(View view, boolean isChecked) {
	}

	/**
	 * Internal use. Uses Checkable, only acts when the View isn't null, and
	 * chains to provided
	 * {@link SmartListAdapter#updateUIForChecked(View, boolean)}.
	 */
	protected void updateUIForCheckedInternal(View view, boolean isChecked) {
		if (view != null) {
			if (view instanceof Checkable) {
				((Checkable) view).setChecked(isChecked);
			}
			updateUIForChecked(view, isChecked);
		}
	}
	
	/**
	 * Return false if you'd like to control updates to the model yourself by
	 * calling recreateItemList(). The default implementation returns true
	 * (any changes to the backing list will cause a re-creation of the list
	 * and a subsequent UI refresh).
	 * 
	 * It's useful to return false here if you re-create the list a lot, and
	 * the repeated calls to recreateItemList() would be taxing.
	 */
	protected boolean autoUpdate() {
	    return true;
	}

	public final int getCount() {
		return itemList.size();
	}

	public final Object getItem(int position) {
		return itemList.get(position);
	}

	/**
	 * The default implementation simply returns (long) position.
	 */
	public long getItemId(int position) {
		return (long) position;
	}

	/**
	 * The default implementation simply returns false.
	 */
	@Override
	public boolean hasStableIds() {
		return false;
	}

	/**
	 * If you override, make sure to provide 1 + the number of types of views
	 * you will create.
	 */
	@Override
	public int getViewTypeCount() {
		return 2; // Category headers (Category) and list items (ListItem)
	}

	/**
	 * If you override, delegate to this method if the item is a Category.
	 */
	@Override
	public int getItemViewType(int position) {
		if (getItem(position) instanceof SmartListAdapter.Category) {
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	public final boolean isEnabled(int position) {
		return !(getItem(position) instanceof SmartListAdapter.Category);
	}

	@Override
	public final boolean areAllItemsEnabled() {
		return false;
	}

	public final View getView(int position, View convertView, ViewGroup parent) {
		if (getItem(position) instanceof SmartListAdapter.Category) {
			return getCategoryView(
					(SmartListAdapter.Category) getItem(position), convertView,
					parent);
		} else {
			ListItem item = (ListItem) getItem(position);
			View view = getListItemView(position, item, convertView, parent);

			// update the view's checked state.
			// we first set false and then conditionally set true.
			// this is because the re-used view might have been a CHECKABLE
			// item, whereas
			// the new view might not be. Therefore, we have to indiscriminately
			// uncheck
			// all Views.
			updateUIForCheckedInternal(view, false);
			if (getClickMode(position, item) == CLICK_MODE_CHECKABLE
					&& checkedItems.contains(position)) {
				updateUIForCheckedInternal(view, true);
			}

			return view;
		}
	}

	/**
	 * Return a UI view for the category label headers.
	 */
	protected View getCategoryView(Category category, View convertView,
			ViewGroup parent) {
		return ListLayouts.categoryHeader(layoutInflater, convertView, parent,
				category.text);
	}

	/**
	 * We are notified when the backing list changes through this function.
	 */
	public final void update(Observable observable, Object data) {
		assert (observable == this.getBackingList());
		recreateItemList();
	}

	/**
	 * Called when the backing list changes. You can also call this to refresh
	 * the list when the mapping from backing items to list items has changed,
	 * but the backing list has not been modified in any way.
	 */
	protected final void recreateItemList() {

		// get a list of items that should be checked.
		List<ListItem> shouldBeCheckedItems = new ArrayList<ListItem>();
		for (int position : checkedItems) {
			shouldBeCheckedItems.add((ListItem) getItem(position));
		}
		checkedItems.clear();

		// use the expander to turn BackingItems into ListItems.
		List<ListItem> rawItems = new ArrayList<ListItem>();
		if (useExpander()) {
			for (BackingItem backingItem : getBackingList()) {
				rawItems.addAll(expandItems(backingItem));
			}
		} else {
			for (BackingItem backingItem : getBackingList()) {
				rawItems.add((ListItem) backingItem);
			}
		}

		// sort, if requested by concrete class
		Comparator<? super ListItem> comparator = getComparator();
		if (comparator != null) {
			Collections.sort(rawItems, comparator);
		}

		itemList.clear();
		String lastCategory = null;
		for (Object object : rawItems) {

			// we know that they are listItems because they all come
			// from calls to expandItems()
			ListItem item = (ListItem) object;

			String currentCategory = getCategory(item);
			if (currentCategory != null && currentCategory.trim().equals(""))
				currentCategory = null;

			if (currentCategory != null
					&& !currentCategory.equals(lastCategory)) {
				itemList.add(new Category(currentCategory)); // category header.
				lastCategory = currentCategory;
			}
			itemList.add(item);

			// mark this item as checked if it should be
			// (i.e. if an equivalent ListItem was checked
			// before calling this method).
			if (shouldBeCheckedItems.contains(item)) {
				int position = itemList.size() - 1;
				checkedItems.add(position);
			}

		}

		notifyDataSetChanged();
	}

    /**
     * Use this instead of ListView.setAdapter.
     * If you're going to override this, YOU MUST CALL SUPER!
     */
    public void setAsAdapterOn(ListView listView) {
        listView.setOnItemClickListener(this);
        listView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        listView.setItemsCanFocus(false);
        listView.setAdapter(this);
        this.listView = listView;
    }        
    
    public final ListView getListView() {
        return this.listView;
    }

	public final void onItemClick(AdapterView<?> parent, View view,
			int position, long id) {

		// N.B. we're guaranteed to get a ListItem here, the category
		// Categorys are disabled and won't have click handlers executed.
		ListItem item = (ListItem) getItem(position);

		int clickMode = getClickMode(position, item);
		if (clickMode == CLICK_MODE_NOT_CLICKABLE)
			return;
		if (clickMode == CLICK_MODE_INSTANTANEOUS) {
			onInstantaneousClick(position, item, view);

		} else {
			assert (clickMode == CLICK_MODE_CHECKABLE);
			if (!allowClickCheckChanges(position, item)) {
			    onInstantaneousClick(position, item, view);
			    return;
			}
			
			if (getMaxCheckedItems() == 0)
				return;
			try {
				if (!uncheck(position)) { // i.e. wasn't checked!
					check(position);
				}
			} catch (WrongClickModeException e) {
			} catch (InvalidPositionException e) {
			}
		}

	}

	/**
	 * Returns the {@code View} currently showing at the given position, or null
	 * if either of the following hold true:
	 *  - that position isn't currently visible (the view has been recycled)
	 *  - this adapter isn't yet attached to a ListView
	 */
	public final View getViewAtPosition(int position) {
		// from:
		// http://stackoverflow.com/questions/257514/android-access-child-views-from-a-listview
	    if (listView == null) return null;
		int firstPosition = listView.getFirstVisiblePosition()
				- listView.getHeaderViewsCount();
		int wantedChild = position - firstPosition;
		if (wantedChild < 0 || wantedChild >= listView.getChildCount()) {
			return null;
		}
		return listView.getChildAt(wantedChild);
	}

	public final boolean isChecked(int position) {
		return checkedItems.contains(position);
	}

	public final int findItemPosition(ListItem toFind) {
		for (int i = 0; i < itemList.size(); ++i) {
			ListItem item = (ListItem) itemList.get(i);
			if (item.equals(toFind)) {
				return i;
			}
		}
		return -1;
	}

	public List<Integer> findItemPositions(Filter<ListItem> filter) {
		List<Integer> positions = new ArrayList<Integer>();
		for (int i = 0; i < getCount(); ++i) {
			Object item = getItem(i);
			if (item instanceof RemoteListItem) {
				if (filter.accept((ListItem) item)) {
					positions.add(i);
				}
			}
		}
		return positions;
	}

	public int checkMatching(Filter<ListItem> filter, boolean isChecked) {
		int numChanged = 0;

		for (Integer position : findItemPositions(filter)) {
			try {
				if (isChecked) {
					if (check(position))
						numChanged++;
				} else {
					if (uncheck(position))
						numChanged++;
				}
			} catch (WrongClickModeException e) {
			} catch (InvalidPositionException e) {
			}
		}

		return numChanged;
	}

	public int checkMatching(Filter<ListItem> filter) {
		return checkMatching(filter, true);
	}

	public int uncheckMatching(Filter<ListItem> filter) {
		return checkMatching(filter, false);
	}

	/**
	 * Check a list item. Obeys maximum rule as set by
	 * {@link SmartListAdapter#getMaxCheckedItems()}. Updates the UI to match.
	 * 
	 * DO NOT use this from onToggleOn or onInstantaneousClick! Let the adapter
	 * perform checking when items are clicked.
	 * 
	 * @param position
	 *            the position to check.
	 * @throws InvalidPositionException
	 *             if the item at the given position is a category header.
	 * @throws WrongClickModeException
	 *             if the item at the given position is not checkable.
	 * @return true if the item was checked, false if it was already checked.
	 */
	public final boolean check(int position) throws WrongClickModeException,
			InvalidPositionException {
		if (checkedItems.contains(new Integer(position))) {
			return false;
		}

		if (position == -1) {
		    throw new InvalidPositionException();
		}
		
		Object object = getItem(position);
		if (object instanceof SmartListAdapter.Category) {
			throw new InvalidPositionException();
		}

		ListItem item = (ListItem) object;
		if (getClickMode(position, item) != CLICK_MODE_CHECKABLE) {
			throw new WrongClickModeException();
		}

		View view = getViewAtPosition(position);
		check(position, item, view);
		return true;
	}
	
	/**
	 * Check an item. Will chain to uncheck See
	 * {@link SmartListAdapter#check(int)} for public-facing equivalent.
	 */
	protected final void check(int position, ListItem item, View view) {
		// uncheck least-recent if checkedItems is currently full.
		if (checkedItems.size() == getMaxCheckedItems()) {
			int removedPosition = checkedItems.removeLast();
			ListItem removedItem = (ListItem) getItem(position);
			uncheck(removedPosition, removedItem,
					getViewAtPosition(removedPosition));
		}

		checkedItems.addFirst(position);
		if (item instanceof Checkable) {
			((Checkable) item).setChecked(true);
		}
		onToggleOn(position, item, view);
		updateUIForCheckedInternal(view, true);
	}

	/**
	 * Uncheck a list item. Updates the UI to match.
	 * 
	 * DO NOT use this from onToggleOff or onInstantaneousClick! Let the adapter
	 * perform checking when items are clicked.
	 * 
	 * @param position
	 *            the position to uncheck.
	 * @throws WrongClickModeException
	 *             if the item at the given position is not checkable.
	 * @return true if the item was unchecked, false if it was already
	 *         unchecked.
	 */
	public final boolean uncheck(int position) throws WrongClickModeException {
		if (!checkedItems.contains(position)) {
			return false;
		}
		checkedItems.remove((Object) new Integer(position)); // force removal of
																// value, not
																// key

		ListItem item = (ListItem) getItem(position);
		if (getClickMode(position, item) != CLICK_MODE_CHECKABLE) {
			throw new WrongClickModeException();
		}

		View view = getViewAtPosition(position);
		uncheck(position, item, view);
		return true;
	}

	/**
	 * Un-check an item. Note: call this method only after removing the position
	 * from checkedItems. See {@link SmartListAdapter#uncheck(int)} for
	 * public-facing equivalent.
	 */
	protected final void uncheck(int position, ListItem item, View view) {
		if (item instanceof Checkable) {
			((Checkable) item).setChecked(false);
		}
		onToggleOff(position, item, view);
		updateUIForCheckedInternal(view, false);
	}

	public final void uncheckAll() {
		while (!checkedItems.isEmpty()) {
			int position = checkedItems.removeLast();
			ListItem item = (ListItem) getItem(position);
			uncheck(position, item, getViewAtPosition(position));
		}
	}

	public final void checkAll() {
		for (int position = 0; position < getCount(); ++position) {
			Object o = getItem(position);
			if (!(o instanceof SmartListAdapter.Category)) {
				check(position, (ListItem) o, getViewAtPosition(position));
			}
		}
	}

	public final List<Integer> getCheckedPositions() {
		return checkedItems;
	}

	public final List<ListItem> getCheckedItems() {
		List<ListItem> l = new ArrayList<ListItem>();
		for (Integer position : checkedItems) {
			l.add((ListItem) getItem(position));
		}
		return l;
	}

	protected final Context getContext() {
		return context;
	}

	protected LayoutInflater getLayoutInflater() {
		return layoutInflater;
	}

    protected SmartList<BackingItem> getBackingList() {
        return backingList;
    }

}
