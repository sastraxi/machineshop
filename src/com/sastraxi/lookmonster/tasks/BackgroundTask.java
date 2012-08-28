package com.sastraxi.lookmonster.tasks;

public interface BackgroundTask {

	public boolean isActive();

	public boolean isCancelable();

	public boolean isIndeterminate();

	public int getProgress();

	public int getMaxProgress();

	public int getSubtask();

	public int getMaxSubtask();

	public String getMessage();

	public boolean showOnActionBar();

	public String getActionBarMessage();

	public void setManager(BackgroundTaskManager manager);

}