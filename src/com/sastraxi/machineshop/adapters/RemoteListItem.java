package com.sastraxi.machineshop.adapters;

import android.view.View;

import com.sastraxi.machineshop.remote.Remote;

public abstract class RemoteListItem {

	private final String name;
	private final String extra;
	private boolean enabled = true;
	private Remote remote;

	@Override
	public boolean equals(Object o) {
		// XXX I know this isn't finished, but (Bill O'Reilly).
		if (o == null)
			return false;
		if (!(o instanceof RemoteListItem))
			return false;

		RemoteListItem o2 = (RemoteListItem) o;
		if (o2.remote != this.remote)
			return false;
		if (!o2.name.equals(this.name))
			return false;

		return true;
	}

	public RemoteListItem(Remote remote, String name, String extra) {
		this.remote = remote;
		this.name = name;
		this.extra = extra;
	}

	public String getExtra() {
		return extra;
	}

	public String getName() {
		return name;
	}

	public abstract void action(View viewInList);

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isCheckable() {
		return true;
	}

	public Remote getRemote() {
		return remote;
	}

}