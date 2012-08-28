package com.sastraxi.machineshop.adapters.code;

import java.util.ArrayList;
import java.util.List;


import android.content.Context;
import android.text.Spannable;


public class TextContext {
    
    public Position start;
    public Position end;
    
    public TextContextIdentifier identifier;
    
    private List<TextItem> items = new ArrayList<TextItem>(); 
    
    public TextContext(Position start, Position end, TextContextIdentifier identifier) {
        this.start = start;
        this.end = end;
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        if (identifier != null) {
            return identifier.toString() + "(" + start + " to " + end + ")";
        } else {
            return "---- (" + start + " to " + end + ")";
        }
    }
    
    public boolean contains(Position cursor) {
        return (start.compareTo(cursor) <= 0 && cursor.compareTo(end) < 0);
    }
    
    public void markItems(Context context, int line, Spannable spannable) {
        // TODO: do some pre-processing in addItem to make this faster
        for (TextItem item: items) {            
            if (item.start.line <= line && item.end.line >= line) {                
                
                int markStart = item.start.character;
                if (item.start.line < line)
                    markStart = 0;
                
                int markEnd = item.end.character;
                if (item.end.line > line)
                    markEnd = spannable.length();
                
                item.identifier.mark(context, spannable, markStart, markEnd);
                
            }            
        }
    }
    
    public void addItem(TextItem item) {
        items.add(item);
    }

    public boolean isEmpty() {
        return this.start.equals(this.end);
    }

    public List<TextItem> getItems() {
        return items;
    }
    
}