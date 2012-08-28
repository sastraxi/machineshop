package com.sastraxi.machineshop.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.sastraxi.machineshop.R;
import com.sastraxi.machineshop.project.Project;

public class ListLayouts {

	public static View textAndNumber(LayoutInflater inflater, View view,
			ViewGroup parent, String name, String extra) {
		if (view == null) {
			view = inflater.inflate(R.layout.listitem_textandnumber, parent,
					false);
		}

		((TextView) view.findViewById(R.id.key)).setText(name);
		((TextView) view.findViewById(R.id.value)).setText(extra);

		return view;
	}

	public static View categoryHeader(LayoutInflater inflater, View view,
			ViewGroup parent, String text) {
		if (view == null) {
			view = inflater.inflate(R.layout.listitem_sectionheader, parent,
					false);
			view.setTag(view.findViewById(R.id.category));
		}

		TextView textView = (TextView) view.getTag();
		textView.setText(text);
		return view;
	}

	public static View progress(LayoutInflater inflater, View view,
			ViewGroup parent, String message, boolean indeterminate,
			int progress, int max, boolean cancelable) {
		if (view == null) {
			view = inflater.inflate(R.layout.listitem_progress, parent, false);
		}

		int cancelVisibility = View.INVISIBLE;
		if (cancelable) {
			cancelVisibility = View.VISIBLE;
		}

		((TextView) view.findViewById(R.id.message)).setText(message);
		((ImageView) view.findViewById(R.id.cancel))
				.setVisibility(cancelVisibility);
		ProgressBar progressBar = (ProgressBar) view
				.findViewById(R.id.progress);
		progressBar.setIndeterminate(indeterminate);
		progressBar.setProgress(progress);
		progressBar.setMax(max);

		return view;
	}

	public static View project(LayoutInflater inflater, View view, final ViewGroup parent, final Project project) {
		if (view == null) {
			view = inflater.inflate(R.layout.listitem_project, parent, false);
			
		}

		((TextView) view.findViewById(R.id.name)).setText(project.getName());
		((TextView) view.findViewById(R.id.path)).setText(project.getRemotePath().toString());

		return view;
	}


    public static View textAndSummary(LayoutInflater inflater, View view,
            ViewGroup parent, String text, String summary) {
        if (view == null) {
            view = inflater.inflate(R.layout.listitem_textandsummary, parent,
                    false);
        }

        ((TextView) view.findViewById(R.id.text)).setText(text);
        ((TextView) view.findViewById(R.id.summary)).setText(summary);

        return view;
    }

    /**
     * Set closeButtonListener to null to hide the close button.
     * @param iconResource 
     */
    public static View textAndCloseButton(LayoutInflater inflater, View view, ViewGroup parent,
            String text, Integer iconResource, OnClickListener closeButtonListener) {
        
        if (view == null) {
            view = inflater.inflate(R.layout.listitem_textandclosebutton, parent, false);
        }
        
        ((TextView) view.findViewById(R.id.text)).setText(text);
       
        ImageView iconView = (ImageView) view.findViewById(R.id.icon);
        if (iconResource != null) {
            iconView.setVisibility(View.VISIBLE);
            iconView.setImageResource(iconResource);
        } else {
            iconView.setVisibility(View.INVISIBLE);
        }
        
        ImageButton button = (ImageButton) view.findViewById(R.id.closeButton);
        if (closeButtonListener == null) {
            button.setVisibility(View.INVISIBLE);
            button.setOnClickListener(null);
        } else {
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(closeButtonListener);
        }
        
        return view;
    }

    public static View iconText(LayoutInflater inflater, View view, ViewGroup parent, String text, Integer leftIcon) {
        return iconText(inflater, view, parent, text, leftIcon, null, null, null, null);
    }

    public static View iconText(LayoutInflater inflater, View view, ViewGroup parent, String text, Integer leftIcon,
            Integer rightIcon, OnClickListener rightIconOnClick,
            Integer rightIcon2, OnClickListener rightIcon2OnClick) {
        
        if (view == null) {
            view = inflater.inflate(R.layout.iconitem, parent, false);
        }
        
        ((TextView) view.findViewById(R.id.text)).setText(text);
       
        ImageView iconView = (ImageView) view.findViewById(R.id.leftIcon);
        if (leftIcon != null) {
            iconView.setVisibility(View.VISIBLE);
            iconView.setImageResource(leftIcon);
        } else {
            iconView.setVisibility(View.INVISIBLE);
        }

        iconView = (ImageView) view.findViewById(R.id.rightIcon);
        if (rightIcon != null) {
            iconView.setVisibility(View.VISIBLE);
            iconView.setImageResource(rightIcon);
            iconView.setOnClickListener(rightIconOnClick);
        } else {
            iconView.setVisibility(View.GONE);
        }
        
        iconView = (ImageView) view.findViewById(R.id.rightIcon2);
        if (rightIcon2 != null) {
            iconView.setVisibility(View.VISIBLE);
            iconView.setImageResource(rightIcon2);
            iconView.setOnClickListener(rightIcon2OnClick);
        } else {
            iconView.setVisibility(View.GONE);
        }
        
        return view;
    }

    public static View iconTextSwitch(LayoutInflater inflater, View view, ViewGroup parent, String text,
            Integer icon, OnClickListener iconClicked,
            boolean state, OnCheckedChangeListener checkedChanged) {

        if (view == null) {
            view = inflater.inflate(R.layout.icon_switch_item, parent, false);
        }
        
        ((TextView) view.findViewById(R.id.text)).setText(text);       

        ImageView iconView = (ImageView) view.findViewById(R.id.icon);
        if (icon != null) {
            iconView.setVisibility(View.VISIBLE);
            iconView.setImageResource(icon);
            iconView.setOnClickListener(iconClicked);
        } else {
            iconView.setVisibility(View.GONE);
        }
        
        Switch switcher = (Switch) view.findViewById(R.id.switcher);
        switcher.setChecked(state);
        switcher.setOnCheckedChangeListener(checkedChanged);

        return view;
    }    
    
}
