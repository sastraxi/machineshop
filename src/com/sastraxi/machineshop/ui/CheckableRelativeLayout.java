package com.sastraxi.machineshop.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Checkable;
import android.widget.RelativeLayout;

/**
 * RelativeLayout that implements the Checkable interface, recording its checked
 * state internally and updating its Drawable states with
 * {@link android.R.attr.state_checked}.
 */
public class CheckableRelativeLayout extends RelativeLayout implements
		Checkable {

	private boolean checked = false;

	@Override
	public boolean isClickable() {
		return false;
	}

	public CheckableRelativeLayout(Context context) {
		super(context);
	}

	public CheckableRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CheckableRelativeLayout(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	public boolean isChecked() {
		return this.checked;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
		refreshDrawableState();
	}

	public void toggle() {
		setChecked(!isChecked());
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
