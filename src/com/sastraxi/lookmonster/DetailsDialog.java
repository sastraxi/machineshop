package com.sastraxi.lookmonster;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public class DetailsDialog extends AlertDialog {

	private ListView listView;

	private class DetailAdapter extends ArrayAdapter<Detail> {

		public DetailAdapter(Context context, List<Detail> objects) {
			super(context, 0, 0, objects);
		}

	}

	public DetailsDialog(Context context, List<Detail> details) {
		super(context);
		listView = new ListView(context);
		listView.setAdapter(new DetailAdapter(context, details));
		setContentView(listView);
	}

}
