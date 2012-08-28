package com.sastraxi.machineshop.remote;

import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.util.Log;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpProgressMonitor;
import com.sastraxi.lookmonster.tasks.BackgroundTask;
import com.sastraxi.lookmonster.tasks.BackgroundTaskManager;
import com.sastraxi.machineshop.project.FileDatabase;
import com.sastraxi.machineshop.project.RemoteFile;
import com.sastraxi.machineshop.remote.actions.LoadAction;
import com.sastraxi.machineshop.remote.actions.RemoteFileAction;
import com.sastraxi.machineshop.remote.actions.SaveAction;

public class FileOperationsQueue {

	/**
	 * Note: all methods are called on the UI thread.
	 */
	public interface ProgressListener {

		/**
		 * Called at the start of a batch load/save operation, and whenever the
		 * number of files in the batch has changed while the batch is
		 * transferring.
		 * 
		 * i.e. Implementations should use this to update the total number of
		 * files.
		 */
		public void onUpdateBatch(int operaton, int numFiles);

		/**
		 * 
		 * Called when a file is about to start transferring.
		 */
		public void onStartFile(int operation, RemoteFile file);

		/**
		 * Called when we know more about a file's transfer progress. The file
		 * we're referring to is the last {@link RemoteFile} given to
		 * {@link ProgressListener#onStartFile(int, RemoteFile)} with the same
		 * {@code operation}.
		 */
		public void onProgress(int operation, long bytesTransferred,
				long bytesTotal);

		/**
		 * Called when a file has finished transferring. This method is called
		 * after the individual file callbacks registered with
		 * {@link FileOperationsQueue#save(RemoteFile, Runnable)} or
		 * {@link FileOperationsQueue#load(RemoteFile, Runnable)}.
		 */
		public void onFinishFile(int operation, RemoteFile file);

		/**
		 * Called after all operations have finished for a specific batch.
		 */
		public void finishedBatch(int operation);

	};

	protected class InternalProgressListener implements SftpProgressMonitor {

		private Task task;
		private long max;

		public InternalProgressListener(Task task) {
			this.task = task;
		}

		public boolean count(final long count) {
			task.progress = (float) count / (float) max;
			getActivity().runOnUiThread(new Runnable() {
				public void run() {
					for (ProgressListener progressListener : progressListeners) {
						progressListener.onProgress(task.operation, count, max);
					}
					if (task.manager != null) {
						task.manager.onProgress(task);
					}
				}
			});
			return true;
		}

		public void end() {
		}

		public void init(int op, String src, String dest, long max) {
			this.max = max;
		}

	};

	protected class Task implements Runnable, BackgroundTask {

		// keep the connection and thread alive for 2 minutes after
		//
		private static final long WAIT_TIME_MILLIS = 120 * 1000;

		private Thread thread = null;
		private InternalProgressListener progressListener;
		private ChannelSftp sftp = null;
		private final LinkedList<RemoteFile> queue;
		private final LinkedList<Runnable> callbacks;
		private int operation;

		protected RemoteFile currentFile;
		protected int totalFiles, finishedFiles; // in the "batch"
		protected float progress; // of current file

		private BackgroundTaskManager manager = null;

		public Task(int operation) {
			this.queue = new LinkedList<RemoteFile>();
			this.callbacks = new LinkedList<Runnable>();
			this.operation = operation;
			this.progressListener = new InternalProgressListener(this);
		}

		public RemoteFile getCurrentFile() {
			return currentFile;
		}

		public void run() {

			try {
				this.sftp = remote.createSFTP();
			} catch (NotConnectedException e) {
				Log.e("FileOperationsQueue.Task()", Log.getStackTraceString(e));
			}

			while (true) {

				progress = 0.0f;

				// N.B. we don't have to check if we have any files here,
				// because we only start the thread once we have one and
				// this is the only place where we remove files from the queue.
				Runnable callback;
				synchronized (queue) {
					currentFile = queue.removeFirst();
					callback = callbacks.removeFirst();
				}

				try {

					for (final ProgressListener l : progressListeners) {
						getActivity().runOnUiThread(new Runnable() {
							public void run() {
								l.onStartFile(operation, currentFile);
							}
						});
					}			
					
					if (operation == OP_SAVE) {
						// TODO; deal with deletes.
						sftp.put(currentFile.getLocalFile().getAbsolutePath().toString(),
								 currentFile.getPath().toString(),
								 progressListener);
					} else {
					    
					    // let's make sure the directories exist leading up to this file.
					    currentFile.getLocalFile().getParentFile().mkdirs();
					    
					    // get a copy of the mtime database
					    FileDatabase db = new FileDatabase(remote.getApp());
					    
						SftpATTRS stat = sftp.stat(currentFile.getPath().toString());
						long remoteLastModified = (long) stat.getMTime();
						long localLastModified = db.getLastModified(currentFile);
						if (!currentFile.getLocalFile().exists() || remoteLastModified != localLastModified) {
							sftp.get(currentFile.getPath().toString(),
							         currentFile.getLocalFile().getAbsolutePath().toString(),
							         progressListener);
							db.setLastModified(currentFile, remoteLastModified);
						}
					}

					if (callback != null) {
						getActivity().runOnUiThread(callback);
					}

					for (final ProgressListener l: progressListeners) {
						getActivity().runOnUiThread(new Runnable() {
							public void run() {
								l.onFinishFile(operation, currentFile);
							}
						});
					}

				} catch (Exception e) {
					Log.e("FileOperationsQueue.task",
							Log.getStackTraceString(e));
				}

				finishedFiles += 1;

				synchronized (queue) {
					if (!isActive()) {

						totalFiles = 0;
						finishedFiles = 0;
						currentFile = null;

						for (final ProgressListener l : progressListeners) {
							getActivity().runOnUiThread(new Runnable() {
								public void run() {
									l.finishedBatch(operation);
								}
							});
						}

						// this fun little block of code waits a while for new
						// files
						// to come in. If none do, the thread dies!
						long startWait = System.currentTimeMillis();
						while (!isActive() && (System.currentTimeMillis() - startWait) < WAIT_TIME_MILLIS) {
							try {
								queue.wait(WAIT_TIME_MILLIS);
							} catch (InterruptedException e) {
								// do nothing.
							}
						}
						if (!isActive()) {
						    break;
						}
					}
				}
			}

			this.sftp.disconnect();
			this.sftp = null;
			
			thread = null; // signal that we're done.
		}

		public void addFile(RemoteFile file, final Runnable callback, boolean highPriority) {
			// the complicated code has to do with re-setting a file with new priority
			// if called with highPriority. We want to call the new callback *and* the
			// original callback when the item is done, so we call them both with the
			// multiCallback.
			synchronized (queue) {

				int existingIndex = queue.indexOf(file);
				if (existingIndex != -1) {

					Runnable multiCallback = callback;
					final Runnable existingCallback = callbacks
							.get(existingIndex);
					if (existingCallback != null) {
						multiCallback = new Runnable() {
							public void run() {
								existingCallback.run();
								callback.run();
							}
						};
					}

					if (highPriority) {
						// bump up priority, using the composite callback.
						queue.remove(existingIndex);
						queue.addFirst(file);
						callbacks.remove(existingIndex);
						callbacks.addFirst(multiCallback);
					} else {
						callbacks.set(existingIndex, multiCallback);
					}

				} else {
					if (highPriority) {
						queue.addFirst(file);
						callbacks.addFirst(callback);
					} else {
						queue.addLast(file);
						callbacks.addLast(callback);
					}
				}

				totalFiles += 1;

				// FIXME high priority: ensure the Remote is connected before we begin
				
				if (isRunning()) {
					queue.notify();
				} else {
					finishedFiles = 0;
					thread = new Thread(this);
					thread.start();
					// TODO use single-thread thread pool (executor) instead, like Remote.
				}
				
				if (this.manager != null) {
				    this.manager.onProgress(this);
				}
			}
		}

		protected boolean isRunning() {
			return thread != null && thread.isAlive(); // XXX do we even need isAlive()?
		}

		public boolean isActive() {
			return finishedFiles < totalFiles;
		}

		public boolean isCancelable() {
			return false; // TODO cancelable.
		}

		public boolean isIndeterminate() {
			return false;
		}

		public int getProgress() {
			return (int) (progress * 10000);
		}

		public int getMaxProgress() {
			return 10000;
		}

		public int getSubtask() {
			return finishedFiles;
		}

		public int getMaxSubtask() {
			return totalFiles;
		}

		public String getMessage() {
			if (getCurrentFile() == null)
				return "Waiting for files";
			if (operation == OP_LOAD) {
				return "Downloading " + currentFile.getName()
				        + " (" + getSubtask() + "/" + (getMaxSubtask() + 1) + ")";
			} else {
				assert (operation == OP_SAVE);
				return "Uploading " + currentFile.getName()
				        + " (" + getSubtask() + "/" + (getMaxSubtask() + 1) + ")";
			}
		}

		public boolean showOnActionBar() {
			return true;
		}

		public String getActionBarMessage() {
			int n = getMaxSubtask() - getSubtask();
			if (operation == OP_LOAD) {
				return n + " down";
			} else {
				assert (operation == OP_SAVE);
				return n + " up";
			}
		}

		public void setManager(BackgroundTaskManager manager) {
			this.manager = manager;
		}

	};

	private static final int OP_LOAD = 0;
	private static final int OP_SAVE = 1;

	private final Remote remote;

	private List<ProgressListener> progressListeners = new Vector<ProgressListener>();

	private Task saveTask, loadTask;

	public FileOperationsQueue(Remote remote) {
		this.remote = remote;
		this.loadTask = new Task(OP_LOAD);
		this.saveTask = new Task(OP_SAVE);
	}

	public boolean isComplete() {
		return !loadTask.isActive() && !saveTask.isActive();
	}

	public void addProgressListener(ProgressListener listener) {
		if (progressListeners.contains(listener))
			return;
		progressListeners.add(listener);
	}

	public void removeProgressListener(ProgressListener listener) {
		progressListeners.remove(listener);
	}

	public BackgroundTask getUploadTask() {
		return saveTask;
	}

	public BackgroundTask getDownloadTask() {
		return loadTask;
	}

	public Activity getActivity() {
		return remote.getApp().getCurrentActivity();
	}

	public void disconnect() {
	    if (saveTask.sftp != null)
	        saveTask.sftp.disconnect();
        if (loadTask.sftp != null)
            loadTask.sftp.disconnect();        
	}
	
	/*******************************************************************/	

	/**
	 * Enqueues an action from the RemoteService.
	 * @param action
	 */
    public void queueAction(final RemoteFileAction action) {        
        Runnable callback = new Runnable() {            
            public void run() {
                action.finish(remote.getApp());
            }
        };        
        if (action instanceof SaveAction) {
            saveTask.addFile(action.getFile(remote.getApp()), callback, action.hasCallback());
        } else {
            assert(action instanceof LoadAction);
            loadTask.addFile(action.getFile(remote.getApp()), callback, action.hasCallback());
        }
    }

}
