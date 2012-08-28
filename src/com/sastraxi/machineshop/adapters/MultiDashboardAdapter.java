package com.sastraxi.machineshop.adapters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;

import com.sastraxi.lookmonster.SimplerSmartListAdapter;
import com.sastraxi.lookmonster.SmartList;
import com.sastraxi.machineshop.EditorActivity;
import com.sastraxi.machineshop.ProjectSettingsActivity;
import com.sastraxi.machineshop.R;
import com.sastraxi.machineshop.project.Project;

public class MultiDashboardAdapter extends SimplerSmartListAdapter<IconListItem> {

    private final Project project;
    private Set<IconListItem> needsConnection = new HashSet<IconListItem>();    

    private boolean hasConstructorFinished = false;

    private Map<IconListItem, OnClickListener> clickMap
            = new HashMap<IconListItem, OnClickListener>();
    
    private final IconListItem onlineSwitchItem;
    private final IconListItem settingsItem;
    private IconListItem editorItem;
    private IconListItem collaborateItem;
    private IconListItem pullItem;
    
    public MultiDashboardAdapter(final Context context, final Project project) {
        super(context, new SmartList<IconListItem>());
        this.project = project;
        
        SmartList<IconListItem> list = getBackingList();
        
        onlineSwitchItem = new IconListItem(null, "", null, R.drawable.icon_help);
        onlineSwitchItem.setRightIconClick(new OnClickListener() {            
            public void onClick(View v) {
                // TODO show help
            }
        });
        clickMap.put(onlineSwitchItem, new OnClickListener() {
            public void onClick(View v) {
                // TODO start downloading stuff if the project is going offline 
                project.setOnline(!project.isOnline());                
            }
        });        
        updateOnlineSwitch();
        list.add(onlineSwitchItem);
        
        settingsItem = new IconListItem(null, "Settings", R.drawable.icon_settings); 
        clickMap.put(settingsItem, new OnClickListener() {
            public void onClick(View v) {
                ProjectSettingsActivity.launchIntent(context, project);
            }
        });
        list.add(settingsItem);
        
        String CATEGORY = "Actions";
        
        editorItem = new IconListItem(CATEGORY, "Open in Editor", R.drawable.icon_directions);
        clickMap.put(editorItem, new OnClickListener() {
            public void onClick(View v) {
                EditorActivity.launchIntent(context, project);
            }
        });
        list.add(editorItem);
        
        collaborateItem = new IconListItem(CATEGORY, "Collaborate", R.drawable.icon_group);
        clickMap.put(collaborateItem, new OnClickListener() {
            public void onClick(View v) {
                // TODO collaborate activity
            }
        });
        list.add(collaborateItem);
        
        pullItem = new IconListItem(CATEGORY, "Pull", R.drawable.icon_download);
        clickMap.put(pullItem, new OnClickListener() {
            public void onClick(View v) {
                // TODO collaborate activity
            }
        });
        list.add(pullItem);
        
        //addViewAndHandler(ListLayouts.iconTextSpinner(inflater, null, null, "Branch", R.drawable.icon_branch));

        // TODO status adapter + items + everything
        //addView(ListLayouts.categoryHeader(inflater, null, null, "Status"));
        //addAdapter(new StatusItemsAdapter(project));
        
        // TODO run configurations adapter + items + everything
        //addView(ListLayouts.categoryHeader(inflater, null, null, "Run Configurations"));
        //addAdapter(new RunConfigurationsAdapter(project));
        
        // TODO commands/services adapter + items + everything
        //addView(ListLayouts.categoryHeader(inflater, null, null, "Commands and Services"));
        //addAdapter(new ServicesAdapter(project));
        //addAdapter(new CommandsAdapter(project));
        
        // TODO currently-modified files at the end
        //addView(ListLayouts.categoryHeader(inflater, null, null, "Unsaved Files"));
        //addAdapter(new ModifiedFilesJumpAdapter(project));
        
        recreateItemList();
        hasConstructorFinished = true;
        
    }
    
    private void updateOnlineSwitch() {        
        if (project.isOnline()) {
            onlineSwitchItem.text = "Work Offline";
            onlineSwitchItem.icon = R.drawable.icon_storage;
        } else {
            onlineSwitchItem.text = "Go Online";
            onlineSwitchItem.icon = R.drawable.icon_earth;            
        }
        
        if (getListView() != null) {
            updateItem(onlineSwitchItem);
        }
    }

    private void updateItem(IconListItem item) {
        int position = findItemPosition(item);        
        if (position != -1) {
            View currentView = getViewAtPosition(position);
            if (currentView != null) {                
                getListItemView(position, item, currentView, (ViewGroup) currentView.getParent());
            }
        }
    }

    /**
     * Called by the parent Activity when we connect/disconnect from the
     * Project's Remote.
     */
    public void notifyConnectionChange() {
        for (IconListItem item: needsConnection) {
            updateItem(item);
        }
    }
    
    public ListView createListView() {
        ListView listView = new ListView(getContext());
        setAsAdapterOn(listView);
        return listView;
    }

    public Project getProject() {
        return project;
    }

    @Override
    protected String getCategory(IconListItem item) {
        return item.category;
    }

    @Override
    protected View getListItemView(int position, IconListItem item, View convertView, ViewGroup parent) {
        View view = item.updateView(getLayoutInflater(), convertView, parent);
        if (needsConnection.contains(item)) {
            if (project.getRemote().isConnected()) {
                view.setAlpha(1.0f);
            } else {
                view.setAlpha(0.3f);
            }
        }
        return view;
    }

    @Override
    protected int getClickMode(int position, IconListItem item) {
        if (needsConnection.contains(item) && !project.getRemote().isConnected()) {
            return CLICK_MODE_NOT_CLICKABLE;
        } else {
            return CLICK_MODE_INSTANTANEOUS;
        }
    }

    @Override
    protected int getMaxCheckedItems() {
        return NO_LIMIT;
    }
    
    /**
     * Don't update the backing list until we've finished the constructor.
     */
    @Override
    protected boolean autoUpdate() {
        return hasConstructorFinished;
    }
    
    @Override
    public void onInstantaneousClick(int position, IconListItem item, View view) {
        OnClickListener listener = clickMap.get(item);
        if (listener != null) {
            listener.onClick(view);
        }
    }
    
}
