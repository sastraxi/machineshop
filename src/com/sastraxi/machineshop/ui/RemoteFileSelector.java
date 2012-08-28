package com.sastraxi.machineshop.ui;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.sastraxi.lookmonster.InvalidPositionException;
import com.sastraxi.lookmonster.SimplerSmartListAdapter;
import com.sastraxi.lookmonster.SmartList;
import com.sastraxi.lookmonster.WrongClickModeException;
import com.sastraxi.machineshop.adapters.ListLayouts;
import com.sastraxi.machineshop.project.RemoteFile;
import com.sastraxi.machineshop.project.RemoteFileEntry;
import com.sastraxi.machineshop.project.RemoteFolder;
import com.sastraxi.machineshop.remote.Remote;
import com.sastraxi.machineshop.remote.RemoteService;
import com.sastraxi.machineshop.remote.actions.ListFilesAction;
import com.sastraxi.machineshop.remote.actions.RemoteAction;
import com.sastraxi.machineshop.remote.actions.RemoteActionCallback;
import com.sastraxi.machineshop.util.AbsolutePath;

// TODO: also create RemoteFileExplorer for a single-pane approach.
// TODO: 2 constructors. one for folders, other one for files.
//       file c'tor takes regex for filename and
//       an icon resource to show as the icon for matching files.
// TODO: the 3 line icon for folders.
// TODO: min-width 200, expand from there based on items
// TODO: dividers between items
// TODO: don't add last listview if it has no items.
// TODO: scroll checked list items into view after the listview loses focus
// TODO: animate
// TODO: keep dialog a certain size, no smaller + no bigger
public class RemoteFileSelector extends HorizontalScrollView {

    public class RemoteFolderAdapter extends SimplerSmartListAdapter<RemoteFileEntry> {
        
        private final RemoteFolder folder;
        private RemoteFileEntry selectedEntry;
    
        private static final boolean SHOW_HIDDEN_FILES = false;
        
        public RemoteFolderAdapter(Context context, final RemoteFolder folder) {
            super(context, new SmartList<RemoteFileEntry>());
            this.folder = folder;
            
            final int listMode;
            if (isFolder()) {
                listMode = Remote.MODE_LIST_FOLDERS;
            } else {
                listMode = Remote.MODE_LIST_ALL;
            }
            
            RemoteService.queue(getContext(),
                    new ListFilesAction(
                            folder,
                            listMode,
                            SHOW_HIDDEN_FILES,
                            new RemoteActionCallback() {                                        
                                public void run(RemoteAction action) {
                                    
                                    ListFilesAction lfa = (ListFilesAction) action;
                                    getBackingList().addAll(lfa.getFolderEntries());
                                    recreateItemList();                             
                                    
                                    if (selectedEntry != null) {
                                        try {
                                            check(findItemPosition(selectedEntry));
                                        } catch (WrongClickModeException e) {
                                            // folders are always checkable
                                            assert(false);
                                        } catch (InvalidPositionException e) {
                                            Log.e("RemoteFileSelector", "Folder Adapter given bogus selected entry: "
                                                    + selectedEntry.toString() + " not in " + folder.toString());
                                        }
                                    }
                                    
                                }
                            }));
            
        }
        
        @Override
        protected boolean autoUpdate() {
            return false;
        }    
        
        @Override
        protected String getCategory(RemoteFileEntry item) {
            return null;
        }
    
        @Override
        protected View getListItemView(int position, RemoteFileEntry item, View view, ViewGroup parent) {
            return ListLayouts.textAndNumber(getLayoutInflater(), view, parent, item.getName(), "");
        }
    
        @Override
        protected int getClickMode(int position, RemoteFileEntry item) {
            if (!isFolder() || item.isFolder()) {
                return CLICK_MODE_CHECKABLE;
            } else {
                return CLICK_MODE_NOT_CLICKABLE;
            }
        }
    
        @Override
        protected int getMaxCheckedItems() {
            return 1;
        }
        
        @Override
        public void onToggleOn(int position, RemoteFileEntry item, View view) {
            selectedEntry = item;
            if (item.isFolder()) {
                navigateTo((RemoteFolder) item);
            } else {
                selectedFile = (RemoteFile) item;
            }
        }
        
        @Override
        public void onToggleOff(int position, RemoteFileEntry item, View view) {
            selectedEntry = null;
            navigateTo(folder);
        }
    
        public RemoteFolder getFolder() {
            return folder;
        }

        public void setSelectedEntry(RemoteFileEntry selectedEntry) {            
            this.selectedEntry = selectedEntry;
        }
    
    }
    
        
    private final Remote remote;
    private final boolean isFolder;
    private RemoteFolder selectedFolder;
    private RemoteFile selectedFile;
    
    private final LinearLayout container;
    
    public RemoteFileSelector(Context context, RemoteFolder selectedFolder, boolean isFolder) {
        super(context);
        this.isFolder = isFolder;
        this.remote = selectedFolder.getRemote();
        
        this.container = new LinearLayout(context);
        this.container.setOrientation(LinearLayout.HORIZONTAL);
        this.container.setLayoutParams(new LayoutParams(800, 500));
        addView(this.container);
        
        navigateTo(selectedFolder);
    }       

    public RemoteFileEntry getSelectedEntry() {
        if (isFolder) {
            return selectedFolder;
        } else {
            return selectedFile;
        }
    }
    
    private ListView addFolderView(RemoteFolder folder) {
        
        ListView listView = new ListView(getContext());
        listView.setLayoutParams(new LinearLayout.LayoutParams(300, LayoutParams.MATCH_PARENT));
        RemoteFolderAdapter adapter = new RemoteFolderAdapter(getContext(), folder); 
        adapter.setAsAdapterOn(listView);        
        container.addView(listView);
        return listView;        
        
    }

    public Remote getRemote() {
        return remote;
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void navigateTo(RemoteFolder folder) {
        if (folder != null && folder.equals(selectedFolder)) return;
        this.selectedFolder = folder;
        
        // so that we can start listing files.
        if (folder == null) {
            folder = new RemoteFolder(remote, AbsolutePath.ROOT);
        }
        
        AbsolutePath folderPath = folder.getPath();
        List<ListView> listViews = getListViews();

        int createFrom = 0;
        for (ListView listView: listViews) {
            RemoteFolderAdapter adapter = (RemoteFolderAdapter) listView.getAdapter();
            if (adapter.getFolder().getPath().isAncestorOf(folderPath)) {
                createFrom += 1;
            } else {
                break;
            }            
        }
        
        for (int i = createFrom; i < listViews.size(); ++i) {
            container.removeView(listViews.get(i));
        }
        
        for (; createFrom < folderPath.depth()+1; ++createFrom) {                        
            ListView newList = addFolderView(new RemoteFolder(remote, folderPath.partial(createFrom)));
            
            if (createFrom < folderPath.depth()) {
                // this isn't the rightmost folder, so select whichever folder is next.
                RemoteFileEntry selectedEntry = new RemoteFolder(remote, folderPath.partial(createFrom+1));
                RemoteFolderAdapter adapter = ((RemoteFolderAdapter) newList.getAdapter());
                adapter.setSelectedEntry(selectedEntry);
            }
            
        }
        
    }
    
    public List<ListView> getListViews() {
        List<ListView> listViews = new ArrayList<ListView>();
        for (int i = 0; i < container.getChildCount(); ++i) {
            View candidate = container.getChildAt(i);
            if (candidate instanceof ListView) {
                listViews.add((ListView) candidate);
            }
        }
        return listViews;
    }

    public void showDialog(Context context, final Runnable runIfSuccess) {
        
        String title;
        if (isFolder) {
            title = "Select Folder";
        } else {
            title = "Select File";
        }
        
        new AlertDialog.Builder(context)
                .setTitle(title + " on " + remote.getName())
                .setView(this)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {                                
                            public void onClick(DialogInterface dialog, int which) {
                                runIfSuccess.run();
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {                                
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                .show();
    }

    public void navigateTo(AbsolutePath absolutePath) {
        navigateTo(new RemoteFolder(remote, absolutePath));
    }    
    
}
