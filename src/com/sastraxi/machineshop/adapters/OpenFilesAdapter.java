package com.sastraxi.machineshop.adapters;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.sastraxi.lookmonster.InvalidPositionException;
import com.sastraxi.lookmonster.SimplerSmartListAdapter;
import com.sastraxi.lookmonster.SmartList;
import com.sastraxi.lookmonster.WrongClickModeException;
import com.sastraxi.machineshop.R;
import com.sastraxi.machineshop.project.OpenFilesInterface;
import com.sastraxi.machineshop.project.RemoteFile;
import com.sastraxi.machineshop.project.OpenFilesInterface.Listener;

public class OpenFilesAdapter extends SimplerSmartListAdapter<RemoteFile> implements Listener {

    private final OpenFilesInterface openFiles;

    public OpenFilesAdapter(Context context, OpenFilesInterface openFiles) {
        super(context, new SmartList<RemoteFile>());
        this.openFiles = openFiles;
        this.openFiles.addListener(this);
    }

    @Override
    protected String getCategory(RemoteFile item) {
        return null;
    }

    @Override
    protected View getListItemView(final int position, final RemoteFile file, View convertView, ViewGroup parent) {                
        Integer iconResource = null;
        if (openFiles.isModified(file)) {
            iconResource = R.drawable.modified_bullet; 
        }        
        return ListLayouts.textAndCloseButton(getLayoutInflater(), convertView, parent,
                file.getName(),
                iconResource,
                new View.OnClickListener() {                    
                    public void onClick(View v) {
                        openFiles.close(file);
                    }
                });
    }

    @Override
    protected int getClickMode(int position, RemoteFile item) {
        return CLICK_MODE_CHECKABLE;
    }

    @Override
    protected int getMaxCheckedItems() {
        return 1;
    }
    
    @Override
    public boolean allowClickCheckChanges(int position, RemoteFile item) {
        // we'll handle checking/unchecking ourselves.
        return false;
    }
    
    @Override
    public void onInstantaneousClick(int position, RemoteFile file, View view) {
        openFiles.select(file);
    }

    @Override
    public void onClose(RemoteFile file) {
        getBackingList().remove(file);
    }

    @Override
    public void onOpen(RemoteFile file) {
        getBackingList().add(file);
    }

    @Override
    public void onModifiedStateChanged(RemoteFile file) {
        int position = findItemPosition(file);
        View view = getViewAtPosition(position);
        if (view != null) {
            // that view is visible, update it.
            getListItemView(position, file, view, (ViewGroup) view.getParent());
        }
    }

    @Override
    public boolean allowClose(RemoteFile file) {
        return true;
    }

    @Override
    public void onSelect(RemoteFile oldFile, RemoteFile newFile) {
        try {
            if (newFile != null) {
                check(findItemPosition(newFile));
            }
        } catch (WrongClickModeException e) {
            // they're all checkable.
            assert(false);
        } catch (InvalidPositionException e) {
            // we know onOpen will be called before this.
            // that being said, let's log it anyway.
            Log.wtf("onSelect", "Called before onOpen (selected file not in open list!)");
        }
    }
    
}
