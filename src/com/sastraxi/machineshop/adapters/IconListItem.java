package com.sastraxi.machineshop.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

public class IconListItem {

    final String category;
    String text;
    Integer icon;
    Integer rightIcon;
    Integer rightIcon2;

    private OnClickListener rightIconListener = null;
    private OnClickListener rightIcon2Listener = null;
    
    public IconListItem(String category, String text, Integer icon) {
        this(category, text, icon, null);
    }
    public IconListItem(String category, String text, Integer icon, Integer rightIcon) {
        this(category, text, icon, null, null);
    }
    
    public IconListItem(String category, String text, Integer icon, Integer rightIcon, Integer rightIcon2) {
        this.category = category;
        this.text = text;
        this.icon = icon;
        
        this.rightIcon = rightIcon;        
        this.rightIcon2 = rightIcon2;
    }
    
    public View updateView(LayoutInflater inflater, View view, ViewGroup parent) {
        return ListLayouts.iconText(inflater, view, parent, text, icon,
                rightIcon, rightIconListener,
                rightIcon2, rightIcon2Listener);
    }
    
    public void setRightIconClick(OnClickListener onClickListener) {
        this.rightIconListener = onClickListener;
    }

    public void setRightIcon2Click(OnClickListener onClickListener) {
        this.rightIcon2Listener = onClickListener;
    }

    @Override
    public int hashCode() {
        int hash = this.text.hashCode();
        if (icon != null)
            hash += icon;
        if (rightIcon != null)
            hash += rightIcon;
        if (rightIcon2 != null)
            hash += rightIcon2;
        return hash;
    }
    
}
