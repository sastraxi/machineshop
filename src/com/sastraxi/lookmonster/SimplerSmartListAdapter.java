package com.sastraxi.lookmonster;

import java.util.List;

import android.content.Context;

/**
 * Like the SmartListAdapter, but doesn't use expanding items. i.e., the backing
 * items *are* the list items. Simply for convenience/pretty code.
 */
public abstract class SimplerSmartListAdapter<T> extends SmartListAdapter<T, T> {

	public SimplerSmartListAdapter(Context context, SmartList<T> backingList) {
		super(context, backingList);
	}

	@Override
	protected final boolean useExpander() {
		return false;
	}

	@Override
	protected final List<T> expandItems(T backingItem) {
		return null;
	}

}
