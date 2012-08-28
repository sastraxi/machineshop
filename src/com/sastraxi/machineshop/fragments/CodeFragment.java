package com.sastraxi.machineshop.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.sastraxi.machineshop.project.RemoteFile;
import com.sastraxi.machineshop.ui.CodeMirrorEditor;

public class CodeFragment extends Fragment {
    
	private static final String ARG_PREFIX = "CodeFragment";
	
    private CodeMirrorEditor editor = null;
    
	public CodeFragment() {

	}
	
	public CodeFragment(CodeMirrorEditor editor) {
	    this.editor = editor;
	}
	
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);       
        editor.getFile().toBundle(ARG_PREFIX, outState);
    }
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        if (editor == null) {
            RemoteFile file = RemoteFile.fromBundle(getActivity(), ARG_PREFIX, savedInstanceState);
            editor = new CodeMirrorEditor(getActivity(), file);
        }        
        return editor;
        
    }

	@Override
	public void onStart() {
		super.onStart();
	}

    public CodeMirrorEditor getEditor() {
        return editor;
    }

}
