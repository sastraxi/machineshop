package com.sastraxi.lookmonster;

import android.graphics.Bitmap;

public class Detail {

	private final String title;
	private final String subtitle;
	private final String description;
	private final Bitmap picture;

	public Detail(String title, String subtitle, String description,
			Bitmap picture) {
		this.title = title;
		this.subtitle = subtitle;
		this.description = description;
		this.picture = picture;
	}

}
