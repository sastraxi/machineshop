package com.sastraxi.machineshop.adapters.code;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import android.content.Context;
import android.text.SpannableString;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.sastraxi.machineshop.R;
import com.sastraxi.machineshop.project.RemoteFile;

public abstract class CodeAdapter extends BaseAdapter {
    
    /**
     * Keys that we should keep the marked column.
     * Marked column: used so that keyboard navigation
     * doesn't suck in an IDE. Remember the column in which the
     * user started navigating up/down and stay on that column
     * to the best of our ability (due to ragged line lengths).
     */
    public static final int[] MARKED_COLUMN_KEYS = new int[]
        { KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_UP,
          KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_PAGE_UP };              
            
    protected final List<TextContext> contexts = new ArrayList<TextContext>();
    protected final List<Boolean> spannableIsCurrent = new ArrayList<Boolean>();
    protected final RemoteFile file;
    protected final List<Object> lines = new ArrayList<Object>();
    private final Context context;
    private Position cursor;
    private ListView listView;

    private int markedColumn = -1;
    
    public CodeAdapter(Context context, RemoteFile file) {
        this.context = context;
        this.file = file;
        loadFromFile();
    }
    
    /**
     * Return a list of TextContextIdentifiers that can be found in a file
     * of this type.
     * 
     * It is suggested to use a static list internally, and then return that
     * in this function.
     */
    protected abstract List<TextContextIdentifier> getContextidentifiers();
    
    /**
     * Return a mapping of context identifiers (from getContextIdentifiers())
     * to the TextItemIdentifiers they can contain. Also can map from null
     * to define TextItemIdentifiers available in the default context.
     * 
     * It is suggested to use a static map internally, and then return that
     * in this function. 
     */
    protected abstract Map<Object, TextItemIdentifier[]> getContextitems();

    /**
     * Use this instead of ListView.setAdapter.
     */
    public final void setAsAdapterOn(ListView listView) {
        listView.setChoiceMode(ListView.CHOICE_MODE_NONE);
        listView.setItemsCanFocus(true);
        listView.setAdapter(this);
        
        int backgroundColour = 0xFFFFFFFF; // XXX: don't define this here!!
        listView.setBackgroundColor(backgroundColour);
        listView.setCacheColorHint(backgroundColour);
        listView.setDivider(null);
        listView.setDividerHeight(0);        
        
        this.listView = listView;
    }    
    
    /**
     * Returns the {@code View} currently showing at the given position, or null
     * if that position isn't currently visible and the view has been recycled.
     */
    public final View getViewAtPosition(int position) {
        // from:
        // http://stackoverflow.com/questions/257514/android-access-child-views-from-a-listview
        int firstPosition = listView.getFirstVisiblePosition()
                - listView.getHeaderViewsCount();
        int wantedChild = position - firstPosition;
        if (wantedChild < 0 || wantedChild >= listView.getChildCount()) {
            return null;
        }
        return listView.getChildAt(wantedChild);
    }
    
    /**
     * Replaces the contents of the array with refreshed contents from the
     * backing file.
     */
    protected void loadFromFile() {
    
    	try {
    		BufferedReader reader = new BufferedReader(new FileReader(file.getLocalFile()));
    		while (true) {
    			String line = reader.readLine();
    			if (line == null)
    				break;
    			addLineAtEnd(line);
    		}
    	} catch (FileNotFoundException e) {
    	} catch (IOException e) {
    		Log.e("FileAsCodeAdapter", "Readline IOException", e);
    	}
    
        // add a new virtual line to the end of every file.
    	// this will be the only line in a new file.
    	// FIXME: this isn't the way to do this!
        addLineAtEnd("");
        
        markContexts();
        
    }

    private void markContexts() {
        contexts.clear();	   
    
        Position endCursor = new Position(getCount()-1, getString(getCount()-1).length());	    
        Position cursor = new Position(0, 0);
        String lineString = null;	    
        while (cursor.compareTo(endCursor) < 0) {
    
            TextContext blankContext = new TextContext(cursor.clone(), null, null);
            TextContext nextContext = new TextContext(null, null, null);
            
            while (nextContext.identifier == null && cursor.compareTo(endCursor) <= 0) {
            
                lineString = getString(cursor.line);
                nextContext.start = blankContext.end = cursor.withCharacter(lineString.length());
                
        	    for (TextContextIdentifier tci: getContextidentifiers()) {
        	        int start = tci.findStart(lineString, cursor.character);
        	        if (start != -1 && start < nextContext.start.character) {
        	            nextContext.start.character = start;
        	            blankContext.end = nextContext.start; // XXX: I don't think we need this because nextContext.start == blankContext.end (an object)
        	            nextContext.identifier = tci;
        	        }
        	    }
        	    
        	    if (nextContext.identifier == null) {
        	        cursor.line += 1;
        	        cursor.character = 0;
        	    }
        	    
            }
            
            // create the blank context
            if (!blankContext.isEmpty()) {
                addContext(blankContext);
            }
            
            // now advance & create the next context
            cursor = nextContext.start.clone();
            while (nextContext != null && nextContext.identifier != null && cursor.compareTo(endCursor) < 0) {	           
    
                lineString = getString(cursor.line);
                
                int foundEnd = -1;
                int lineType = nextContext.identifier.getLineType(); 
                if (lineType == TextContextIdentifier.SINGLE_LINE_AUTOCLOSE) {
                    // go to the end of the line.
                    foundEnd = lineString.length();
                } else {
                    // +1 so that symmetric contexts (e.g. strings) don't match at the start again
    	            foundEnd = nextContext.identifier.findEnd(lineString, cursor.character + 1);
    	            if (foundEnd == -1 && lineType == TextContextIdentifier.SINGLE_LINE) {
    	                // TODO add in errors for disallowed multiline contexts
    	                // nextContext.identifier = new ErrorTextContext(); 
    	                foundEnd = lineString.length();
    	            }
                }
                
                if (foundEnd != -1) {
                    nextContext.end = cursor.withCharacter(foundEnd);
                    addContext(nextContext);
                    cursor = nextContext.end.clone();
                    nextContext = null; // mark that we're done with this iteration
                }                
                
                if (nextContext != null) {
                    cursor.line += 1;     
                    cursor.character = 0;
                }
                
            }
            
            if (cursor.compareTo(endCursor) >= 0 && nextContext != null && nextContext.identifier != null) {
                if (!nextContext.isEmpty()) {
    	            // TODO mark as error
                    // nextContext.identifier = new ErrorTextContext();	            
    	            addContext(nextContext);
                }
            }
            
        }
    }

    protected void addContext(TextContext context) {
        
        // get the list of items this context can contain. 
        TextItemIdentifier[] itemIdentifiers = getContextitems().get(context.identifier);       
        if (itemIdentifiers != null) {
            
            SortedSet<TextItem> candidates = new TreeSet<TextItem>();
            for (Iterator<PositionedString> iter = getContextStrings(context); iter.hasNext(); ) {
                PositionedString ps = iter.next();
                for (TextItemIdentifier identifier: itemIdentifiers) {
                    candidates.addAll(identifier.findAll(ps));
                }
            }
            
            Position cursor = context.start;
            for (TextItem item: candidates) {            
                // throws out any candidates we've already passed
                if (item.start.compareTo(cursor) >= 0) { 
                    context.addItem(item);
                    cursor = item.end;
                }            
            }
        }
        
        contexts.add(context);
            
    }

    private Iterator<PositionedString> getContextStrings(final TextContext context) {
        return new Iterator<PositionedString>() {            
            
            int line = context.start.line;
            
            public void remove() {
                return; // no-op
            }
            
            public PositionedString next() {
                PositionedString ps = new PositionedString(getString(line), new Position(line, 0));                
                if (line == context.start.line) {
                    int ch = context.start.character;
                    ps.position.character = ch;
                    ps.string = ps.string.substring(ch);
                }
                if (line == context.end.line) {
                    int ch = context.end.character - ps.position.character;
                    ps.string = ps.string.substring(0, ch);
                }
                line += 1;
                return ps;
            }
            
            public boolean hasNext() {
                return line <= context.end.line;
            }
            
        };
    }

    protected TextContext getContext(Position cursor) {
        int position = Collections.binarySearch(contexts, new TextContext(cursor, cursor, null), new Comparator<TextContext>() {
            public int compare(TextContext lhs, TextContext rhs) {
                
                // find which one of the TextContexts is the cursor.
                TextContext context;
                Position cursor;
                if (lhs.isEmpty()) { // start == end
                    cursor = lhs.start;
                    context = rhs;
                } else {
                    cursor = rhs.start;
                    context = lhs;
                }
                
                // if the context contains the cursor, this is what we're looking for.
                // otherwise, just compare the starts of these contexts
                if (context.contains(cursor)) return 0;
                return lhs.start.compareTo(rhs.start);
                
            }            
        });
        if (position < 0) {
            Log.wtf("findContext", "couldn't find context for " + cursor.toString());
            return null;
        } else {
            return contexts.get(position);
        }
    }

    protected void addLineAtEnd(String line) {
    	lines.add(line);
    	spannableIsCurrent.add(false);
    }

    protected SpannableString getOrCreateSpannable(int line) {
        
        // easy-out: there is a current spannable
        Object lineItem = getItem(line);
        String lineString;
        if (lineItem instanceof SpannableString) {
            if (spannableIsCurrent.get(line)) {
                return (SpannableString) lineItem;
            } else {
                lineString = ((SpannableString) lineItem).toString();
            }
        } else {
            lineString = (String) lineItem;
        }

        SpannableString spannable = new SpannableString(lineString);
        
        Position cursor = new Position(line, 0);
        Position endCursor = new Position(line, lineString.length()); 
        
        while (cursor.compareTo(endCursor) < 0) {
            
            TextContext textContext = getContext(cursor);
                        
            // set this span to the end of the row. 
            if (textContext.end.compareTo(endCursor) > 0) {
                if (textContext.identifier != null) {
                    spannable.setSpan(textContext.identifier.createStyle(getContext()), cursor.character, endCursor.character, 0);
                }
                cursor = endCursor;
            } else {
                if (textContext.identifier != null) {
                    spannable.setSpan(textContext.identifier.createStyle(getContext()), cursor.character, textContext.end.character, 0);
                }
                cursor = textContext.end;
            }
            
            // highlight all the items in the context.
            textContext.markItems(getContext(), line, spannable);            
            
        }        
        
        // cache so we don't have to do this again until the code changes.
        lines.set(line, spannable);
        spannableIsCurrent.set(line, true);
        return spannable;
        
    }

    public Context getContext() {
        return context;
    }

    protected String getString(int line) {
        Object lineItem = getItem(line);
        if (lineItem instanceof String) {
            return (String) lineItem;
        }
        SpannableString lineSpannable = (SpannableString) lineItem;
        return lineSpannable.toString();
    }

    public View getView(int line, View view, ViewGroup parent) {
    
    	if (view == null) {
    	    // TODO: move to CodeRow
    		LayoutInflater layoutInflater = (LayoutInflater) getContext()
    				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    		view = layoutInflater.inflate(R.layout.code_row, parent, false);
    	}
    
    	CodeRow codeRow = (CodeRow) view;
    	
    	if (line == getCount() - 1) {
    		// last row: it's virtual, accounting for the fact that
    		// a text file is defined as a series of lines of text that
    		// end with newline characters. So, do not give it a line number.
    		codeRow.setLineInformation(line, getCount()-1, new SpannableString(""), this);    	    
    	} else {
    	    codeRow.setLineInformation(line, getCount()-1, getOrCreateSpannable(line), this);
    	}
    
    	return view;
    }

    public int getCount() {
        return lines.size();
    }

    public Object getItem(int position) {
        return lines.get(position);
    }

    public long getItemId(int position) {
        return (long) position;
    }
    
    @Override
    public boolean hasStableIds() {
        return false;
    }
    
    protected void up(int n) {
        Position position = getCursor();
        setMarkedColumn(position.character);        
        int newLine = Math.max(0, position.line - n);
        if (position.line != newLine) {
            setCursor(position.withLine(newLine));        
        }
    }
    
    protected void down(int n) {
        Position position = getCursor();
        setMarkedColumn(position.character);
        int newLine = Math.min(getCount() - 1, position.line + n);
        if (position.line != newLine) {
            setCursor(position.withLine(newLine));        
        }
    }
    
    public void up() { up(1); }
    public void down() { down(1); }
    
    /**
     * Returns the current number of code lines in view.
     */
    public int getPageSize() {
        return listView.getLastVisiblePosition() - listView.getFirstVisiblePosition() + 1;
    }
    
    public void pageUp() {
        up(getPageSize());
    }
    
    public void pageDown() {
        down(getPageSize());
    }
    
    private void setCursor(Position cursor) {
        /*                
        Position previous = getCursor();
        if (previous != null) {
            View previousView = getViewAtPosition(previous.line);
            if (previousView != null) {
                CodeRow codeRow = (CodeRow) previousView;
                
            }
        }
        */

        View newView = getViewAtPosition(cursor.line);
        listView.smoothScrollToPosition(cursor.line);        
        if (newView != null) {
            CodeRow codeRow = (CodeRow) newView;
            
            int lineLength = codeRow.editText.getText().length();
            cursor.character = Math.min(cursor.character, lineLength);
            
            codeRow.editText.requestFocus();            
            codeRow.editText.setSelection(cursor.character);
        }
        
        this.cursor = cursor;
    }

    private Position getCursor() {
        return cursor;
    }

    private void setMarkedColumn(int col) {
        if (this.markedColumn == -1 || col > this.markedColumn) {
            this.markedColumn = col;
        }
    }

    public void clearMarkedColumn() {
        this.markedColumn = -1;
    }


}