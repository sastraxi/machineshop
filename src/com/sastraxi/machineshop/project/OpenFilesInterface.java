package com.sastraxi.machineshop.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.sastraxi.machineshop.fragments.CodeFragment;
import com.sastraxi.machineshop.fragments.EmptyFragment;
import com.sastraxi.machineshop.ui.CodeMirrorEditor;
import com.sastraxi.machineshop.ui.CodeMirrorEditor.ModifiedStateListener;

public abstract class OpenFilesInterface implements ModifiedStateListener {

    public interface Listener {
        
        /**
         * Called when the given entry is clicked on (as in, the user wants to
         * switch to it). The user wants this file open and visible.
         * 
         * Note that oldFile and newFile might be null!
         */
        void onSelect(RemoteFile oldFile, RemoteFile newFile);
        
        /**
         * Called when a given file has just been close.
         */
        void onClose(RemoteFile file);
        
        /**
         * The user wants this file closed.
         * Return true to let the close go through; false otherwise.
         * All listeners must be in agreement for the close to go through.
         */
        boolean allowClose(RemoteFile file);

        /**
         * Called when the given entry has been open. This has no impact on
         * the selection state. If the file was opened and selected, this
         * call will be followed by a call to onSelect.
         */
        void onOpen(RemoteFile file);
        
        /**
         * e.g. when the user makes modifications, or saves the file.
         */
        void onModifiedStateChanged(RemoteFile file);
        
    }

    private static final String OPEN_FILES_LIST = "open_files";
    private static final String SELECTED_FILE_INDEX = "selected_file_index";
    
    private Map<RemoteFile, CodeMirrorEditor> editors = new HashMap<RemoteFile, CodeMirrorEditor>();
    private List<RemoteFile> ordering = new ArrayList<RemoteFile>();
    private RemoteFile selectedFile = null;
    
    private List<Listener> listeners = new ArrayList<Listener>();    
    private final Activity activity;
    private final int fragmentResourceId;
    
    public OpenFilesInterface(Activity activity, int fragmentResourceId) {
        this.activity = activity;
        this.fragmentResourceId = fragmentResourceId;
        selectEmpty();
    }
    
    public void addListener(Listener l) {
        listeners.add(l);
    }
    
    public void removeListener(Listener l) {
        listeners.remove(l);
    }
    
    public boolean isOpen(RemoteFile item) {
        return ordering.contains(item); 
    }
    
    public boolean isModified(RemoteFile file) {
        if (!isOpen(file)) return false;
        
        CodeMirrorEditor currentEditor = editors.get(file);
        if (currentEditor == null) {
            return file.hasAutosaveModifications();
        } else {
            return editors.get(file).isModified();
        }
    }
    
    @Override
    public void onModifiedStateChanged(CodeMirrorEditor editor) {
        for (Listener l: listeners) {
            l.onModifiedStateChanged(editor.getFile());
        }
    }
    
    public void open(final RemoteFile file, final Runnable callback) {
        if (isOpen(file)) callback.run();
        if (!isOpen(file)) {
            
            fetchFile(file, new Runnable() {
                public void run() {
                    
                    ordering.add(file);
    
                    // LISTENER EVENT
                    for (Listener l: listeners) {
                        l.onOpen(file);
                    }
                    
                    if (callback != null) callback.run();
                }
            });            
            
        }
    }

    public void close(RemoteFile file) {
        if (isOpen(file)) {
            
            boolean canClose = true;
            for (Listener l: listeners) {
                if (!l.allowClose(file)) {
                    canClose = false;
                    break;
                }
            }                        
            if (!canClose) return;
            
            ordering.remove(file);
            if (selectedFile.equals(file)) {
                // TODO implement back stack to select the previous file.
                select(null);
            }
            editors.remove(file);            
                        
            // LISTENER EVENT
            for (Listener l: listeners) {
                l.onClose(file);
            }            

        }
    }
    
    public void select(final RemoteFile file) {
        if (selectedFile == null && file == null || selectedFile != null && selectedFile.equals(file)) return;        
        if (file == null) {
            selectEmpty();
            return;
        }
            
        open(file, new Runnable() {
            public void run() {
                continueSelect(file);
            }
        });
    }
    
    /**
     * Runs as the second part of select (as the file-fetching is asynchronous).
     */
    protected void continueSelect(RemoteFile file) {
        if (!editors.containsKey(file)) {
            CodeMirrorEditor editor = new CodeMirrorEditor(activity, file);
            editor.addListener(this);
            editors.put(file, editor);
        }
        
        // LISTENER EVENT
        RemoteFile oldFile = selectedFile;
        selectedFile = file;
        for (Listener l: listeners) {
            l.onSelect(oldFile, selectedFile);
        }

        FragmentManager manager = activity.getFragmentManager();   
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(fragmentResourceId, new CodeFragment(editors.get(file)));
        transaction.commit();
    }
    
    protected void selectEmpty() {        

        // LISTENER EVENT
        RemoteFile oldFile = selectedFile;
        selectedFile = null;
        for (Listener l: listeners) {
            l.onSelect(oldFile, selectedFile);
        }
        
        FragmentManager manager = activity.getFragmentManager();   
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(fragmentResourceId, new EmptyFragment());
        transaction.commit();                
    }

    public RemoteFile getSelected() {
        return selectedFile;
    }

    public CodeMirrorEditor getEditor(RemoteFile file) {
        return editors.get(selectedFile);
    }

    /**
     * Force autosaves & bundle the order/open state of the editor.
     */
    public void toBundle(String argPrefix, Bundle outState) {
        
        int selectedIndex = -1; 
        
        ArrayList<String> pathList = new ArrayList<String>();
        for (int i = 0; i < ordering.size(); ++i) {
            RemoteFile file = ordering.get(i);
            if (file.equals(selectedFile)) {
                selectedIndex = i;
            }
            pathList.add(file.getPath().toString());
        }        
        
        outState.putStringArrayList(argPrefix + OPEN_FILES_LIST, pathList);
        outState.putInt(argPrefix + SELECTED_FILE_INDEX, selectedIndex);
        
    }
    
    /**
     * Implement this method by handling both cases:
     *   1. If you deem this file to be loaded, simply run callback;
     *   2. If not, load the file, show a progress indicator, and then
     *      run the callback afterwards.
     */
    protected abstract void fetchFile(RemoteFile file, Runnable callback);
    
}
