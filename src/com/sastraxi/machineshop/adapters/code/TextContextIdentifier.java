package com.sastraxi.machineshop.adapters.code;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public abstract class TextContextIdentifier implements StyleCreator {
    
    public static final int MULTI_LINE = 0;
    public static final int SINGLE_LINE = 1;
    public static final int SINGLE_LINE_AUTOCLOSE = 2;
    
    private final Pattern start;
    private final Pattern end;
    private final int lineType;

    public TextContextIdentifier(Pattern start, Pattern end, int lineType) {
        this.start = start;
        this.end = end;
        this.lineType = lineType;
    }

    public Pattern getStart() {
        return start;
    }

    public Pattern getEnd() {
        return end;
    }
    
    public int findStart(String lineString, int searchFrom) {
        Matcher matcher = start.matcher(lineString);
        if (matcher.find(searchFrom)) {
            return matcher.start();
        } else {
            return -1;
        }
    }

    public int findEnd(String lineString, int searchFrom) {
        Matcher matcher = end.matcher(lineString);
        if (matcher.find(searchFrom)) {
            return matcher.end();
        } else {
            return -1;
        }
    }

    public int getLineType() {
        return lineType;
    }
    
}
