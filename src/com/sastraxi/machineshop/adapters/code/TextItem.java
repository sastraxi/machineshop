package com.sastraxi.machineshop.adapters.code;


import android.content.Context;
import android.text.Spannable;


public class TextItem implements Comparable<TextItem> {
    
    public Position start;
    public Position end;

    public TextItemIdentifier identifier;
    
    public TextItem(Position start, Position end, TextItemIdentifier identifier) {
        this.start = start;
        this.end = end;
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        return identifier.toString() + "(" + start + " to " + end + ")";
    }
    
    public boolean contains(Position cursor) {
        return (start.compareTo(cursor) <= 0 && cursor.compareTo(end) < 0);
    }

    public int compareTo(TextItem another) {
        int positionCompare = start.compareTo(another.start);
        if (positionCompare != 0)
            return positionCompare;
        return identifier.getPriority() - another.identifier.getPriority();
    }
     
    public void mark(Context context, Spannable spannable) {
        identifier.mark(context, spannable, start.character, end.character);
    }

}
