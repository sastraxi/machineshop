package com.sastraxi.machineshop.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Checkable;
import android.widget.RelativeLayout;

/**
 * RelativeLayout that implements the Checkable interface. Set this view's tag
 * as a Checkable, and this layout will delegate Checkable's interface methods
 * to the tag object.
 */
public class CheckableTagRelativeLayout extends RelativeLayout implements
		Checkable {

	@Override
	public boolean isClickable() {
		return false;
	}

	public CheckableTagRelativeLayout(Context context) {
		super(context);
	}

	public CheckableTagRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CheckableTagRelativeLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * Delegates to (Checkable) getTag().
	 */
	public boolean isChecked() {
		try {
			Checkable checkableTag = (Checkable) getTag();
			return checkableTag.isChecked();
		} catch (ClassCastException e) {
			Log.w("CheckableRelativeLayout",
					"Tag is not an instance of Checkable; this object won't do anything useful.");
		} catch (NullPointerException e) {
			Log.w("CheckableRelativeLayout",
					"Tag is null; this object won't do anything useful.");
		}
		return false;
	}

	/**
	 * Delegates to (Checkable) getTag().
	 */
	public void setChecked(boolean checked) {
		try {
			Checkable checkableTag = (Checkable) getTag();
			checkableTag.setChecked(checked);
			invalidate();
		} catch (ClassCastException e) {
			Log.w("CheckableRelativeLayout",
					"Tag is not an instance of Checkable; this object won't do anything useful.");
		} catch (NullPointerException e) {
			Log.w("CheckableRelativeLayout",
					"Tag is null; this object won't do anything useful.");
		}
	}

	/**
	 * Delegates to (Checkable) getTag().
	 */
	public void toggle() {
		try {
			Checkable checkableTag = (Checkable) getTag();
			checkableTag.toggle();
			invalidate();
		} catch (ClassCastException e) {
			Log.w("CheckableRelativeLayout",
					"Tag is not an instance of Checkable; this object won't do anything useful.");
		} catch (NullPointerException e) {
			Log.w("CheckableRelativeLayout",
					"Tag is null; this object won't do anything useful.");
		}
	}

	private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };

	/**
	 * Reflect the delegate Checkable's state in this View's state set.
	 */
	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isChecked()) {
			mergeDrawableStates(drawableState, CHECKED_STATE_SET);
		}
		return drawableState;
	}

}
