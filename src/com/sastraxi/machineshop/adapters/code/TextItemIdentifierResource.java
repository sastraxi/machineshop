package com.sastraxi.machineshop.adapters.code;

import java.util.regex.Pattern;


import android.content.Context;
import android.text.style.TextAppearanceSpan;

public class TextItemIdentifierResource extends TextItemIdentifier {

    private final int styleResourceId;
    private final String name;

    public TextItemIdentifierResource(String name, Pattern pattern, int spanFlags, int priority, int styleResourceId) {
        super(pattern, spanFlags, priority);
        this.name = name;
        this.styleResourceId = styleResourceId;
    }
    
    @Override
    public String toString() {
        return name;
    }

    public Object createStyle(Context context) {
        return new TextAppearanceSpan(context, styleResourceId);
    }

}
