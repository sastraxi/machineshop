package com.sastraxi.machineshop.adapters.code;

import java.util.regex.Pattern;


import android.content.Context;
import android.text.style.TextAppearanceSpan;

public class TextContextIdentifierResource extends TextContextIdentifier {

    private final int styleResourceId;
    private final String name;

    public TextContextIdentifierResource(String name, Pattern start, Pattern end, int lineStyle, int styleResourceId) {
        super(start, end, lineStyle);
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
