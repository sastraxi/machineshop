package com.sastraxi.machineshop.adapters.code;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.Spannable;


public abstract class TextItemIdentifier implements StyleCreator {
    
    private final Pattern pattern;
    private final int spanFlags;
    private final int priority;

    public TextItemIdentifier(Pattern pattern, int spanFlags, int priority) {
        this.pattern = pattern;
        this.spanFlags = spanFlags;
        this.priority = priority;
    }
    
    public int getPriority() {
        return this.priority;
    }
    
    protected List<TextItem> findAll(PositionedString ps) {
        Matcher matcher = pattern.matcher(ps.string);        
        List<TextItem> items = new ArrayList<TextItem>();
        while (matcher.find()) {
            items.add(new TextItem(
                    new Position(ps.position.line, ps.position.character + matcher.start()),
                    new Position(ps.position.line, ps.position.character + matcher.end()),
                    this));
        }
        return items;
    }

    public int getSpanFlags() {
        return spanFlags;
    }

    public void mark(Context context, Spannable spannable, int start, int end) {
        spannable.setSpan(createStyle(context), start, end, getSpanFlags());
    }
    
}
