package com.sastraxi.machineshop.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.Layout;
import android.widget.EditText;

import com.sastraxi.machineshop.adapters.code.Position;
import com.sastraxi.machineshop.project.RemoteFile;

/**
 * 
 * Methods that modify the cursor are clearly marked.
 */
public class CodeEditText extends EditText {

    /* TODO next steps:
       - make this extend a LinearLayout, throw a line # list in beside it.
         - when the text view scrolls, make it scroll
         - when the text view expands/contracts, change the items in the list
       - add a function to serialize the current file back to disk.
       - incorporate things from CodeAdapter into a CodeHighlighter class,
         change JavaCodeAdapter -> JavaCodeHighlighter
         - should we syntax highlight the whole file at the start?
           - in a background thread?
         
       - need to be able to store multiple open/edited files--
         when a CodeEditText.isModified(), simply store that one away and
         create a new one. Save it in a map? Memory requirements of this approach?
       - NOW you can start worrying about highlighting edited text 
       - code folding can be done by replacing that text with a span, and
         marking in the line # list that so many lines are skipped after this one
    */    
    
    protected static final Pattern INITIAL_WHITESPACE = Pattern.compile("^[ \t]*");
    
    public CodeEditText(Context context, RemoteFile file) {
        super(context);
        setFile(file);
    }

    private RemoteFile file;
    private boolean isModified;

    public void setFile(RemoteFile file) {
        this.file = file; 
        this.isModified = false;
    }
    
    public RemoteFile getFile() {
        return file;
    }
    
    public boolean isModified() {
        return isModified;
    }
    
    protected void markModified() {
        isModified = true;
    }
    
    protected void markClean() {
        isModified = false;
    }
    
    public Position getCursor() {
        return getSelectionStartPosition();
    }
    
    public void setCursor(Position cursor) {
        setSelection(positionToOffset(cursor));
    }
    
    protected int positionToOffset(Position p) {
        return getLayout().getLineStart(p.line) + p.character;
    }
    
    protected Position offsetToPosition(int offset) {
        Layout layout = getLayout();
        int line = layout.getLineForOffset(offset);
        int character = offset - layout.getLineStart(line);
        return new Position(line, character);
    }
        
    public void clearSelection() {
        setSelection(getSelectionStart());
    }
    
    public Position getSelectionStartPosition() {
        return offsetToPosition(getSelectionStart());
    }
    
    public Position getSelectionEndPosition() {
        return offsetToPosition(getSelectionEnd());
    }
        
    public CharSequence getText(Position start, Position end) {
        return getText().subSequence(
            positionToOffset(start),
            positionToOffset(end)
        );
    }
    
    public Position insertText(Position at, CharSequence text) {
        markModified();
        int offset = positionToOffset(at);
        getText().insert(offset, text);
        return offsetToPosition(offset + text.length());
    }
    
    public void removeText(Position start, Position end) {
        markModified();
        replaceText(start, end, "");
    }
        
    /**
     * Replaces the given selection with new text,
     * returns the Position at the end of the new 
     */
    private Position replaceText(Position start, Position end, CharSequence text) {
        markModified();
        int startOffset = positionToOffset(start);
        getText().replace(
            startOffset,
            positionToOffset(end),
            text
        );
        return offsetToPosition(startOffset + text.length());
    }

    private void split(Position cursor) {
        insertText(cursor, "\n");
        markModified();
    }
    
    /**
     * Merges line and the next one together.
     */
    private void merge(int line) {
        assert(line < getLineCount()-1);
        CharSequence line1 = getLine(line);
        CharSequence line2 = getLine(line+1);
        CharSequence merged = new StringBuilder(line1).append(line2).toString();
        replaceText(new Position(line, 0), new Position(line+1, line2.length()), merged);
        markModified();
    }

    public void save() {
        // get output stream from file
        // getText() -> output stream
        // save, mark not modified
    }
    
    /**
     * Returns a line (lineNumber in 0..getLineCount()), excluding the
     * newline character at the end.
     */
    public CharSequence getLine(int lineNumber) {
        Layout layout = getLayout();
        return getText().subSequence(
            layout.getLineStart(lineNumber),
            layout.getLineEnd(lineNumber)
        );
    }
    
    /**
     * Convenience function.
     * @return getLine(getCursor().line);
     */
    private CharSequence getCurrentLine() {
        return getLine(getCursor().line);
    }
    
    public void cursorBackspace() {
        if (hasSelection()) {
            removeText(getSelectionStartPosition(), getSelectionEndPosition());
        } else {
            Position cursor = getCursor();
            if (cursor.character > 0) {
                // deletes a single character
                Position previousChar = cursor.withCharacter(cursor.character-1);
                removeText(previousChar, cursor);
                
            } else if (cursor.line > 0) {
                // deletes the newline
                int lastLength = getLine(cursor.line-1).length();
                merge(cursor.line-1);
                setCursor(new Position(cursor.line-1, lastLength));
            }           
        }
    }
    

    public void cursorDelete() {
        if (hasSelection()) {
            cursorBackspace();
        } else {
            Position cursor = getCursor();
            if (cursor.line < getLineCount()-1 ||
                cursor.character < getCurrentLine().length()) {
                cursorRight();
                cursorBackspace();
            }            
        }
    }

    /**
     * On creation of new lines, we need to match the indentation level of the
     * previous line.
     */
    public void cursorEnter() {
        if (hasSelection()) {
            cursorDelete();
        }        
        CharSequence line = getCurrentLine();         
        Matcher matcher = INITIAL_WHITESPACE.matcher(line);
        String initialTab = "";
        if (matcher.find()) {
            initialTab = matcher.group();
        }
        
        split(getCursor());
        cursorRight();
        setCursor(insertText(getCursor(), initialTab));
        
    }

    public void cursorUp() {
        Position cursor = getCursor();
        if (cursor.line == 0) return;
        cursor.line -= 1;
        setCursor(cursor);
    }
    
    public void cursorDown() {
        Position cursor = getCursor();
        if (cursor.line == getLineCount()-1) return;
        cursor.line += 1;
        setCursor(cursor);    
    }
    
    public void cursorLeft() {
        Position cursor = getCursor();
        if (cursor.character > 0) {
            cursor.character -= 1;
        } else if (cursor.line > 0) {
            cursor.line -= 1;
            cursor.character = getLine(cursor.line).length();
        } else {
            return;
        }
        setCursor(cursor);        
    }
    
    public void cursorRight() {
        Position cursor = getCursor();
        int lastPosition = getLine(cursor.line).length();
        if (cursor.character < lastPosition) {
            cursor.character += 1;
        } else if (cursor.line < getLineCount()-1) {
            cursor.line += 1;
            cursor.character = 0;
        } else {
            return;
        }
        setCursor(cursor);
    }
    
    public void cursorLineStart() {
        // TODO go to end of tabs
        Position cursor = getCursor();
        if (cursor.character == 0) return;
        cursor.character = 0;
        setCursor(cursor);
    }
    
    public void cursorLineEnd() {
        Position cursor = getCursor();
        int lastPosition = getLine(cursor.line).length();
        if (cursor.character == lastPosition) return;
        cursor.character = lastPosition;
        setCursor(cursor);
    }

}
