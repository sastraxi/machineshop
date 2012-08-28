package com.sastraxi.machineshop.ui;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.sastraxi.lookmonster.InvalidPositionException;
import com.sastraxi.lookmonster.SimplerSmartListAdapter;
import com.sastraxi.lookmonster.SmartList;
import com.sastraxi.lookmonster.WrongClickModeException;
import com.sastraxi.machineshop.R;
import com.sastraxi.machineshop.adapters.ListLayouts;
import com.sastraxi.machineshop.project.OpenFilesInterface;
import com.sastraxi.machineshop.project.RemoteFile;
import com.sastraxi.machineshop.project.RemoteFileEntry;
import com.sastraxi.machineshop.project.RemoteFolder;
import com.sastraxi.machineshop.project.OpenFilesInterface.Listener;
import com.sastraxi.machineshop.remote.Remote;
import com.sastraxi.machineshop.remote.RemoteService;
import com.sastraxi.machineshop.remote.actions.ListFilesAction;
import com.sastraxi.machineshop.remote.actions.RemoteAction;
import com.sastraxi.machineshop.remote.actions.RemoteActionCallback;
import com.sastraxi.machineshop.util.PathUtils;

// TODO: icons.
// TODO: click on folder title to show list of parent folder,
//       grandparent, great-grandparent, etc.; clicking navigates
// TODO: instead of switch-then-load, load-then-switch.
//       show a progress indicator where the X would be on the folder.
public class RemoteFileExplorer extends LinearLayout implements OnPageChangeListener, Listener {

    public RemoteFileExplorer(Context context) {
        super(context);
        createLayout();
    }
    public RemoteFileExplorer(Context context, AttributeSet attrs) {
        super(context, attrs);
        createLayout();
    }

    public RemoteFileExplorer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        createLayout();
    }

    private static final boolean SHOW_HIDDEN_FILES = false;
    
    public class FolderAdapter extends SimplerSmartListAdapter<RemoteFileEntry> {        
        
        private RemoteFolder folder;

        public FolderAdapter(Context context, RemoteFolder folder) {
            super(context, new SmartList<RemoteFileEntry>());
            this.folder = folder;
            folder.fetch(getContext(), new Runnable() {                
                public void run() {
                    
                    // FIXME high-priority: sometimes getChildren() returns null which causes addAll to fail??
                    getBackingList().addAll(FolderAdapter.this.folder.getChildren());
                    recreateItemList();
                    
                    // check the currently selected file, if it's in this directory.
                    int p = findItemPosition(openFiles.getSelected());
                    if (p != -1) {
                        try {
                            check(p);
                        } catch (WrongClickModeException e) {
                            // it's not a folder because it comes from a ProjectFile,
                            // and all non-folders are checkable
                            assert(false);
                        } catch (InvalidPositionException e) {
                            // we just checked that it's a valid position
                            assert(false);
                        }
                    }
                }
            });
            RemoteService.queue(getContext(),
                    new ListFilesAction(
                            folder,
                            Remote.MODE_LIST_ALL,
                            SHOW_HIDDEN_FILES,
                            new RemoteActionCallback() {                                        
                                public void run(RemoteAction action) {
                                    
                                    
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
        
        /**
         * Refresh the view of one item without recreating the whole list UI.
         */
        public void refreshEntryView(RemoteFileEntry file) {
            assert(!file.isFolder());
            
            int position = findItemPosition(file);
            if (position == -1) return;
            
            if (file.equals(openFiles.getSelected())) {
                try {
                    check(position);
                } catch (WrongClickModeException e) {
                    // we know it's not a folder
                    assert(false);
                } catch (InvalidPositionException e) {
                    // we already returned if the position was -1
                    assert(false);
                }
            }
            
            View view = getViewAtPosition(position);
            if (view != null) {
                getListItemView(position, file, view, (ViewGroup) view.getParent());
            }
        }
    
        @Override
        protected View getListItemView(int position, final RemoteFileEntry item, View view, ViewGroup parent) {
            String name = item.getName();
            OnClickListener closeButtonListener = null;
            
            Integer iconResource = null;
            if (!item.isFolder()) {
                final RemoteFile fileItem = (RemoteFile) item;
                if (openFiles.isOpen(fileItem)) {
                    // it's an open file--show a close button that'll close it.
                    closeButtonListener = new OnClickListener() {                        
                        public void onClick(View v) {
                            openFiles.close(fileItem);
                        }
                    };
                }
            } else {
                name += "/";
                iconResource = R.drawable.icon_view_as_grid;
            }
            
            return ListLayouts.textAndCloseButton(getLayoutInflater(), view, parent, name, iconResource, closeButtonListener);
        }
    
        @Override
        protected int getClickMode(int position, RemoteFileEntry item) {
            if (item.isFolder()) {
                return CLICK_MODE_INSTANTANEOUS;
            } else {
                return CLICK_MODE_CHECKABLE;
            }
        }
        
        @Override
        public void onInstantaneousClick(int position, RemoteFileEntry item, View view) {
            if (item.isFolder()) {
                navigateTo((RemoteFolder) item);
            } else {
                openFiles.select((RemoteFile) item);
            }
        }
    
        @Override
        protected int getMaxCheckedItems() {
            return 1;
        }
        
        @Override
        public boolean allowClickCheckChanges(int position, RemoteFileEntry item) {
            // we will check/uncheck items ourselves.
            return false;
        }

        public RemoteFolder getFolder() {
            return folder;
        }
    
    }
    
    public class PageAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return deepestNavigated.getPath().depth() - topLevel.getPath().depth() + 1;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
        
        @Override
        public Object instantiateItem(ViewGroup container, int newDepth) {
            
            int depth = topLevel.getPath().depth() + newDepth;
            RemoteFolder folder = deepestNavigated.partial(depth);
            
            ListView listView = pagerLists.get(newDepth);
            if (listView != null) {
                FolderAdapter adapter = (FolderAdapter) pagerLists.get(newDepth).getAdapter();
                if (adapter.getFolder().equals(folder)) {
                    container.addView(listView, 0);
                    return listView;
                }
            }
            
            if (listView == null) {
                listView = new ListView(container.getContext());
            }            
            new FolderAdapter(getContext(), folder).setAsAdapterOn(listView);                           
            pagerLists.put(newDepth, listView);
            
            container.addView(listView, 0);
            return listView;
        }
        
        @Override
        public void destroyItem(ViewGroup container, int depth, Object view) {
            pagerLists.remove(depth);
            container.removeView((View) view);
        }
        
    };
    
    private RemoteFolder topLevel;
    private OpenFilesInterface openFiles;
    
    // current exploration state
    private RemoteFolder deepestNavigated;
    private int relativeDepth;

    // UI items
    private View layout;
    private ImageButton backButton;
    private ImageButton forwardButton;
    private TextView folderTitle;
    private ViewPager pager;
    
    private Map<Integer, ListView> pagerLists = new HashMap<Integer, ListView>();
    
    public void createLayout() {

        // inflate the layout.
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layout = inflater.inflate(R.layout.remote_file_explorer, this, false);
        addView(layout, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

    }
    
    public void setRemoteAndPath(RemoteFolder topLevel, OpenFilesInterface openFiles) {
        this.topLevel = topLevel;
        this.deepestNavigated = topLevel;
        this.relativeDepth = 0;
        this.openFiles = openFiles;
        this.openFiles.addListener(this);

        // grab view pager.
        pager = (ViewPager) layout.findViewById(R.id.pager);
        pager.setOnPageChangeListener(this);
        pager.setAdapter(new PageAdapter());        
        
        // attach events.
        backButton = (ImageButton) layout.findViewById(R.id.back); 
        backButton.setOnClickListener(new OnClickListener() {            
            public void onClick(View v) {
                navigateBack();
            }
        });
        
        forwardButton = (ImageButton) layout.findViewById(R.id.forward);
        forwardButton.setOnClickListener(new OnClickListener() {            
            public void onClick(View v) {
                navigateForward();
            }
        });
        
        // grab the folder title label.
        folderTitle = (TextView) layout.findViewById(R.id.title);
        
        navigateTo(this.topLevel);
        
    }

    protected void navigateTo(RemoteFolder item) {
        
        // path must be a sub-path of the top-level path.
        assert(topLevel.getPath().isAncestorOf(item.getPath()));
        
        this.deepestNavigated = item;
        relativeDepth = this.deepestNavigated.getPath().depth() - topLevel.getPath().depth();
        
        // Fix-up the current list view.
        // ViewPager seems to keep the view to the left and to the right
        // intact, and won't call instantiateItem again on them even if
        // we call notifyDataSetChanged on its adapter...
        ListView currentList = pagerLists.get(relativeDepth);
        if (currentList != null) {
            if (currentList.getAdapter() != null) {
                FolderAdapter adapter = (FolderAdapter) currentList.getAdapter();
                if (!adapter.getFolder().equals(deepestNavigated)) {
                    new FolderAdapter(getContext(), deepestNavigated)
                        .setAsAdapterOn(currentList);
                }
            }
        }        

        pager.setCurrentItem(relativeDepth, true);
        pager.getAdapter().notifyDataSetChanged();
        
    }

    protected void navigateForward() {        
        if (relativeDepth == getMaximumRelativeDepth()) return;
        relativeDepth += 1;
        pager.setCurrentItem(relativeDepth, true);                
    }

    protected void navigateBack() {
        if (relativeDepth == 0) return;
        relativeDepth -= 1;
        pager.setCurrentItem(relativeDepth, true);
    }

    public String getCurrentPath() {
        return deepestNavigated.getPath().partial(topLevel.getPath().depth() + relativeDepth).toString();
    }
    
    public String getCurrentRelativePath() {
        // FIXME: doesn't work anymore
        return PathUtils.pathFrom(getCurrentPath(), topLevel.toString());
    }    

    @Override
    public void onPageScrollStateChanged(int position) { }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

    @Override
    public void onPageSelected(int position) {
        relativeDepth = position;        
        folderTitle.setText(getCurrentRelativePath());
        
        backButton.setEnabled(position > 0);
        forwardButton.setEnabled(position < getMaximumRelativeDepth());        
    }

    private int getMaximumRelativeDepth() {
        return pager.getAdapter().getCount()-1;
    }

    private void refreshFileUI(RemoteFileEntry file) {

        // figure out if we're showing the folder we want.
        RemoteFolder parent = file.getParent();               
        if (!parent.getPath().isAncestorOf(deepestNavigated.getPath())) {
            return;
        }
        
        // refresh this file's view in the correct list.
        int folderRelativeDepth = parent.getPath().depth() - topLevel.getPath().depth();        
        ListView folderList = pagerLists.get(folderRelativeDepth);       
        if (folderList != null) { // it's null if we haven't navigated there yet/recently 
            FolderAdapter adapter = (FolderAdapter) folderList.getAdapter();
            adapter.refreshEntryView(file);
        }
        
    }
    
    @Override
    public void onSelect(RemoteFile oldFile, RemoteFile newFile) {
        if (oldFile != null) refreshFileUI(oldFile);
        if (newFile != null) refreshFileUI(newFile);
    }
    
    @Override
    public void onClose(RemoteFile file) {
        refreshFileUI(file);
    }
    
    @Override
    public void onOpen(RemoteFile file) {
        refreshFileUI(file);
    }
    
    @Override
    public void onModifiedStateChanged(RemoteFile file) {
        refreshFileUI(file);
    }
    
    @Override
    public boolean allowClose(RemoteFile file) {
        return true;
    }
    
}
