package com.sastraxi.machineshop.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.sastraxi.machineshop.MachineShopApplication;
import com.sastraxi.machineshop.project.RemoteFolder;
import com.sastraxi.machineshop.remote.Remote;
import com.sastraxi.machineshop.ui.RemoteFileSelector;

/**
 * <p>
 * A {@link Preference} that allows for selecting a remote path.
 * </p>
 * <p>
 * This preference will store a string into the SharedPreferences.
 * </p>
 */
public class RemoteFolderPreference extends DialogPreference {
    
    private String path;
    private RemoteFileSelector selector;
    
    public RemoteFolderPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);               
    }

    public RemoteFolderPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextPreferenceStyle);
    }

    public RemoteFolderPreference(Context context) {
        this(context, null);
    }
    
    @Override
    protected void onAttachedToHierarchy(PreferenceManager preferenceManager) {
        super.onAttachedToHierarchy(preferenceManager);

        MachineShopApplication app = (MachineShopApplication) getContext().getApplicationContext();
        
        // extract the remote name from the preference name:
        // remotes.REMOTE_NAME.PROJECT_NAME
        String name = preferenceManager.getSharedPreferencesName();
        String remoteName = name.split("[.]")[1];
        Remote remote = app.getRemote(remoteName);                
        selector = new RemoteFileSelector(getContext(), new RemoteFolder(remote, AbsolutePath.ROOT), true);                

        setDialogTitle("Select Folder on " + remoteName);       
    }
    
    /**
     * Saves the text to the {@link SharedPreferences}.
     * 
     * @param text The text to save
     */
    public void setPath(String path) {
        final boolean wasBlocking = shouldDisableDependents();
        
        this.path = path;        
        persistString(path);
        
        final boolean isBlocking = shouldDisableDependents(); 
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }
    
    /**
     * Gets the text from the {@link SharedPreferences}.
     * 
     * @return The current preference value.
     */
    public String getPath() {
        return this.path;
    }

    @Override
    protected View onCreateDialogView() {
        selector.navigateTo(new AbsolutePath(this.path));
        return selector;
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        
        if (positiveResult) {
            String newPath = selector.getSelectedEntry().getPath().toString();
            if (callChangeListener(newPath)) {
                setPath(newPath);
            }
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        setPath(restoreValue ? getPersistedString(path) : (String) defaultValue);
    }

    @Override
    public boolean shouldDisableDependents() {
        return TextUtils.isEmpty(path) || super.shouldDisableDependents();
    }

    /** @hide */
    protected boolean needInputMethod() {
        // We want the input method to show, if possible, when dialog is displayed
        return true;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            // No need to save instance state since it's persistent
            return superState;
        }
        
        final SavedState myState = new SavedState(superState);
        myState.path = getPath();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }
         
        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setPath(myState.path);
    }
    
    private static class SavedState extends BaseSavedState {
        String path;
        
        public SavedState(Parcel source) {
            super(source);
            path = source.readString();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(path);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
    
}
