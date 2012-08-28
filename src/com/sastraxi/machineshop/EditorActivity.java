package com.sastraxi.machineshop;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.sastraxi.machineshop.adapters.OpenFilesAdapter;
import com.sastraxi.machineshop.project.OpenFilesInterface;
import com.sastraxi.machineshop.project.Project;
import com.sastraxi.machineshop.project.RemoteFile;
import com.sastraxi.machineshop.project.OpenFilesInterface.Listener;
import com.sastraxi.machineshop.remote.Remote;
import com.sastraxi.machineshop.ui.CodeMirrorEditor;
import com.sastraxi.machineshop.ui.EditorActionView;
import com.sastraxi.machineshop.ui.RemoteFileExplorer;
import com.sastraxi.machineshop.util.AbsolutePath;
import com.sastraxi.machineshop.util.PathUtils;

// TODO save instance state, re-load files we had from before
// if it's the same project.
public class EditorActivity extends BaseActivity implements Listener {

	public static final String INTENT_EXTRA_REMOTENAME = "remote";
	public static final String INTENT_EXTRA_PROJECTNAME = "project";
	public static final String INTENT_EXTRA_FILEPATH = "filepath";
	
    private static final int AUTOSAVE_DELAY = 60 * 1000; // every minute. TODO make this a pref

	protected Remote remote;
	protected Project project;    
    private RemoteFile contextMenuFile;
    private OpenFilesInterface openFiles;
    private Set<RemoteFile> autosaveSet = new HashSet<RemoteFile>();
    
	private TabHost sidebar;
    private TabSpec explorerTab;
    private TabSpec openFilesTab;    
    private OpenFilesAdapter openFilesAdapter;
    private RemoteFileExplorer fileExplorer;
    private ListView openFilesList;    
    private Handler handler;
    private Runnable autosaveRunnable;
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.editor, menu);
		
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.editor);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setDisplayShowCustomEnabled(true);
		actionBar.setCustomView(new EditorActionView(this));

	    sidebar = (TabHost) findViewById(R.id.sidebar); 
	    sidebar.setup();

	    openFiles = new OpenFilesInterface(this, R.id.codeContainer) {

            @Override
            protected void fetchFile(RemoteFile file, Runnable callback) {
                if (file.getState() == RemoteFile.STATE_UNKNOWN) {
                    file.fetch(EditorActivity.this, callback);
                } else {
                    callback.run();
                }                
            }
	        
	    };
	    openFiles.addListener(this);
	    
        fileExplorer = new RemoteFileExplorer(this);
		explorerTab = sidebar.newTabSpec("explorer")
		        .setIndicator("Browse")
		        .setContent(new TabHost.TabContentFactory() {    
                    public View createTabContent(String tag) {
                        return fileExplorer;
                    }
                });    
	    sidebar.addTab(explorerTab);
			    
        openFilesList = new ListView(this);
        openFilesAdapter = new OpenFilesAdapter(this, openFiles);
		openFilesTab = sidebar.newTabSpec("open")
		        .setIndicator("Open")
		        .setContent(new TabHost.TabContentFactory() {
                    public View createTabContent(String tag) {
                        openFilesAdapter.setAsAdapterOn(openFilesList);
                        return openFilesList;                        
                    }
                });
		sidebar.addTab(openFilesTab);
		
		handler = new Handler();
		autosaveRunnable = new Runnable() {            
            public void run() {
                autosave();
                postAutosaveLater();
            }
        };
				
	}
	
	@Override
	protected void onResume() {
	    super.onResume();
        //postAutosaveLater();
	}
	
	@Override
	protected void onPause() {
	    super.onPause();
	    // TODO force auto-save all open files in EditorActivity.onPause
        //handler.removeCallbacks(autosaveRunnable);
	}

	private void postAutosaveLater() {
	    handler.postDelayed(autosaveRunnable, AUTOSAVE_DELAY);
    }
	
	/**
	 * Saves currently-modified 
	 */
	private void autosave() {
	    for (RemoteFile file: autosaveSet) {
	        openFiles.getEditor(file).autosave(null);
	    }
	}

    private void intentFail(String message) {
		Log.w("EditorActivity.onStart", message);
		finish();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (v == sidebar) {
			// AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			//contextFile = (ProjectFile) fileAdapter.getItem(info.position);
			// XXX fixme this function
			
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.file_context_menu, menu);
			menu.setHeaderTitle(contextMenuFile.getName());

			String fileExtension = PathUtils.getExtension(contextMenuFile.getName());
			MenuItem hideItem = menu.findItem(R.id.hideType);
			if (fileExtension != null) {
				hideItem.setTitle(hideItem.getTitle() + " " + fileExtension);
			} else {
				hideItem.setTitle(R.string.hide_files_without_type);
			}
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.delete:
			// TODO add deletion to save()--check if it exists(), then do the
			// deed
			return true;
		case R.id.hide:
			// TODO add project hiding list
			return true;
		case R.id.hideType:
			// TODO add project hiding list
			// TODO add filter to SmartListAdapter
			return true;
		case R.id.refresh:
			// TODO todon't
			return true;
		}
		return false;
	}

	@Override
	protected void onStart() {
		super.onStart();

		MachineShopApplication app = (MachineShopApplication) getApplicationContext();

		String remoteName = getIntent().getStringExtra(INTENT_EXTRA_REMOTENAME);
		if (remoteName == null)
		    intentFail("Called with null remote");
		for (Remote r : app.getRemotes()) {
			if (r.getName().equals(remoteName)) {
				remote = r;
				break;
			}
		}
		if (remote == null) {
			intentFail("Called with invalid remote named '" + remoteName + "'");
			return;
		}

		String projectName = getIntent().getStringExtra(INTENT_EXTRA_PROJECTNAME);
		if (projectName == null)
			intentFail("Called with null project");
		for (Project p : remote.getProjects()) {
			if (p.getName().equals(projectName)) {
				project = p;
				break;
			}
		}
		if (project == null) {
			intentFail("Called with invalid project named '" + projectName
					+ "' (remote :" + remoteName + ")");
			return;
		}
		
		fileExplorer.setRemoteAndPath(project.getRemoteFolder(), openFiles);
		sidebar.setCurrentTabByTag("explorer");
		
        // does our intent want us to open a file?
	    String filePath = getIntent().getStringExtra(INTENT_EXTRA_FILEPATH);
        if (filePath != null) {
            openFiles.select(new RemoteFile(remote, new AbsolutePath(filePath)));
        }

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// app icon in action bar clicked; go home
			Intent intent = new Intent(this, MultiDashboardActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
			return true;
			
		case R.id.save:
		    save(null);
		    return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

    public static void launchIntent(Context context, Project project) {
        Intent openProjectInEditor = new Intent(context, EditorActivity.class);
        openProjectInEditor.putExtra(EditorActivity.INTENT_EXTRA_REMOTENAME, project.getRemote().getName());
        openProjectInEditor.putExtra(EditorActivity.INTENT_EXTRA_PROJECTNAME, project.getName());
        context.startActivity(openProjectInEditor);
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        if (event.isCtrlPressed() && keyCode == KeyEvent.KEYCODE_S) {            
            save(null);
            return true;
        }
        return super.onKeyShortcut(keyCode, event);
    }
    
    protected void save(final Runnable onSuccess) {
        // save the file locally, and then transfer the file back to the server.
        // show the tasks dialog while doing so.
        if (openFiles.getSelected() == null) {
            Log.wtf("EditorActivity.save", "Called when no file selected...");
        }
        
        CodeMirrorEditor editor = openFiles.getEditor(openFiles.getSelected());        
        editor.save(new Runnable() {
            public void run() {

                getApp().getBackgroundTaskManager().showDialog(true);
                openFiles.getSelected().save(EditorActivity.this, new Runnable() {
                    public void run() {
                        getApp().getBackgroundTaskManager().hideDialog();
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    }
                });
                
            }                
        });        
    }

    @Override
    public void onSelect(RemoteFile old, RemoteFile current) {
        boolean hasOld = (old != null);
        boolean hasCurrent = (current != null);
        if (hasOld != hasCurrent) {
            if (hasCurrent) {
                // TODO: show contextual action bar items
            } else {
                // TODO: hide contextual action bar items
            }
        }
    }

    @Override
    public boolean allowClose(final RemoteFile file) {
        
        // allow files to close immediately if they aren't modified.
        if (!openFiles.isModified(file)) return true;
        
        final Semaphore sem = new Semaphore(0);
        
        new AlertDialog.Builder(this)
            .setTitle("Save Modifications?")
            .setMessage(file.getName() + " has changes. ")
            .setCancelable(false)
            .setPositiveButton("Save", new OnClickListener() {                
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    save(new Runnable() {                        
                        public void run() {
                            sem.release(2);                     
                        }
                    });
                }
            })
            .setNeutralButton("Discard Changes", new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    file.discardAutosave();
                    dialog.dismiss();
                    sem.release(2);
                }
            })
            .setNegativeButton("Cancel", new OnClickListener() {                
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    sem.release(1);
                }
            }).show();
        
        while (true) {
            try {
                sem.acquire();
                boolean allowClose = sem.tryAcquire(); // above, we release 2 if we can close the file
                return allowClose;
            } catch (InterruptedException e) { }
        }
            
    }

    @Override
    public void onOpen(RemoteFile file) {
        
    }

    @Override
    public void onClose(RemoteFile file) {
        
    }

    @Override
    public void onModifiedStateChanged(RemoteFile file) {
        if (openFiles.isModified(file)) {
            autosaveSet.add(file);         
        } else {
            autosaveSet.remove(file);
        }
    }

}