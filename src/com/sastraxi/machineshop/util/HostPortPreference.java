package com.sastraxi.machineshop.util;


import android.content.Context;
import android.preference.EditTextPreference;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.AttributeSet;
import android.widget.EditText;

public class HostPortPreference extends EditTextPreference {

    public HostPortPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        
        EditText hostport = getEditText();
        hostport.setHint("Host (:port)");
        hostport.setSingleLine(true);
        InputFilter addressFilter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) { 
                for (int i = start; i < end; i++) { 
                    char ch = source.charAt(i);
                    if (!Character.isLetterOrDigit(ch) && ch != ':' && ch != '.' && ch != '-') { 
                        return "";
                    } 
                } 
                return null; 
            } 
        }; 
        hostport.setFilters(new InputFilter[]{addressFilter});
        
    }

    public HostPortPreference(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.editTextPreferenceStyle);
    }

    public HostPortPreference(Context context) {
        this(context, null);
    }
    
    @Override
    public void setText(String text) {
        
        String[] parts = text.toString().split(":");
        int port = 22;
        if (parts.length == 2) {
            port = Integer.parseInt(parts[1]);
        }
        String host = parts[0];
        
        super.setText(new HostPort(host, port).toString());
        
    }
    
}
