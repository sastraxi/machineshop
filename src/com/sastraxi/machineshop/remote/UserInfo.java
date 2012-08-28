package com.sastraxi.machineshop.remote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.text.InputType;
import android.text.TextUtils.TruncateAt;
import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.Toast;

import com.jcraft.jsch.UIKeyboardInteractive;

public class UserInfo implements com.jcraft.jsch.UserInfo,
		UIKeyboardInteractive {

	/**
     * 
     */
    private final Remote remote;
	private Boolean b;
	private String text;

	public UserInfo(Remote remote) {
		this.remote = remote;
	}

	public String getPassphrase() {
		return null;
	}

	public String getPassword() {
		return text;
	}

	public boolean promptPassphrase(String arg0) {
		return false;
	}

	public boolean promptPassword(final String message) {
	    
	    final Activity activity = remote.getApp().getCurrentActivity();
		activity.runOnUiThread(new Runnable() {

			public void run() {

				final EditText input = new EditText(activity);
				input.setEllipsize(TruncateAt.END);
				input.setSingleLine();
				input.setInputType(InputType.TYPE_CLASS_TEXT
						| InputType.TYPE_TEXT_VARIATION_PASSWORD);
				input.setTransformationMethod(PasswordTransformationMethod
						.getInstance());

				new AlertDialog.Builder(activity)
						.setTitle(UserInfo.this.remote.name)
						.setCancelable(false)
						.setMessage(message)
						.setView(input)
						.setPositiveButton("OK", new OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								synchronized (remote) {
									text = input.getText().toString();
									b = true;
									remote.notify();
								}
							}
						})
						.setNegativeButton("Cancel", new OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								synchronized (remote) {
									b = false;
									remote.notify();
								}
							}
						}).show();
			}

		});

		synchronized (this.remote) {
			while (true) {
				try {
					this.remote.wait();
					return b;
				} catch (InterruptedException e) {
					// do nothing.
				}
			}
		}

	}

	public boolean promptYesNo(final String message) {

	    final Activity activity = remote.getApp().getCurrentActivity();
		activity.runOnUiThread(new Runnable() {

			public void run() {
				new AlertDialog.Builder(activity).setTitle(UserInfo.this.remote.name)
						.setCancelable(false).setMessage(message)
						.setPositiveButton("Yes", new OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								synchronized (remote) {
									b = true;
									remote.notify();
								}
							}
						}).setNegativeButton("No", new OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								synchronized (remote) {
									b = false;
									remote.notify();
								}
							}
						}).show();
			}

		});

		synchronized (this.remote) {
			while (true) {
				try {
					this.remote.wait();
					return b;
				} catch (InterruptedException e) {
					// do nothing.
				}
			}
		}

	}

	public void showMessage(String message) {
	    final Activity activity = remote.getApp().getCurrentActivity();
		Toast.makeText(activity, message, Toast.LENGTH_LONG);
	}

	public String[] promptKeyboardInteractive(String destination,
			String name, String instruction, String[] prompt, boolean[] echo) {

		String[] responses = new String[prompt.length];
		for (int i = 0; i < prompt.length; ++i) {
			String response = doPrompt(prompt[i], echo[i]);
			if (response == null)
				return null;
			responses[i] = response;
		}
		return responses;

	}

	private String doPrompt(final String prompt, final boolean echo) {
	    final Activity activity = remote.getApp().getCurrentActivity();
		activity.runOnUiThread(new Runnable() {

			public void run() {

				final EditText input = new EditText(activity);
				input.setEllipsize(TruncateAt.END);
				input.setSingleLine();
				if (!echo) {
					input.setInputType(InputType.TYPE_CLASS_TEXT
							| InputType.TYPE_TEXT_VARIATION_PASSWORD);
					input.setTransformationMethod(PasswordTransformationMethod
							.getInstance());
				}

				new AlertDialog.Builder(activity)
						.setTitle(UserInfo.this.remote.name)
						.setCancelable(false)
						.setMessage(prompt)
						.setView(input)
						.setPositiveButton("OK", new OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								synchronized (remote) {
									text = input.getText().toString();
									b = true;
									remote.notify();
								}
							}
						})
						.setNegativeButton("Cancel", new OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								synchronized (remote) {
									b = false;
									remote.notify();
								}
							}
						}).show();
			}

		});

		synchronized (this.remote) {
			while (true) {
				try {
					this.remote.wait();
					return text;
				} catch (InterruptedException e) {
					// do nothing.
				}
			}
		}
	}

}