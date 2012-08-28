package com.sastraxi.machineshop.adapters.code;

import android.content.Context;
import android.text.Spannable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sastraxi.machineshop.R;

public class CodeRow extends RelativeLayout implements OnLongClickListener {

    protected EditText editText;
    protected TextView lineNumber;
    protected CodeAdapter codeAdapter;
    protected int line;
    
    public CodeRow(Context context) {
        super(context);
    }

    public CodeRow(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CodeRow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup();
    }

    public void setAsLine(int line, int maxLine, Spannable content, CodeAdapter codeAdapter) {
        this.line = line;
        this.codeAdapter = codeAdapter;        
    }
    
    public static boolean isKeyEventPrintable(KeyEvent event) {
        return event.getDisplayLabel() != 0;
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        ListView listView = (ListView) getParent();
        listView.smoothScrollToPosition(line);
        
        boolean clearMarkedColumn = true;
        for (int thisKeyCode: CodeAdapter.MARKED_COLUMN_KEYS) {
            if (keyCode == thisKeyCode) {
                clearMarkedColumn = false;
                break;
            }
        }
        if (clearMarkedColumn) {
            codeAdapter.clearMarkedColumn();
        }
        
        switch (keyCode) {
            
            case KeyEvent.KEYCODE_DPAD_UP:
                //codeAdapter.trackPosition(getCurrentPosition(), event.isShiftPressed());
                codeAdapter.up();
                return true;
            
            case KeyEvent.KEYCODE_DPAD_DOWN:
                //codeAdapter.trackPosition(getCurrentPosition(), event.isShiftPressed());
                codeAdapter.down();
                return true;
                        
            case KeyEvent.KEYCODE_PAGE_UP:
                //codeAdapter.up(getCurrentPosition(), codeAdapter.getPageSize(), event.isShiftPressed());
                codeAdapter.pageUp();
                return true;
                
            case KeyEvent.KEYCODE_PAGE_DOWN:
                codeAdapter.pageDown();
                //codeAdapter.down(getCurrentPosition(), codeAdapter.getPageSize(), event.isShiftPressed());
                return true;
                
            case KeyEvent.KEYCODE_ENTER:                
                //codeAdapter.splitLine(getCurrentPosition());
                //return true;
                
             // NOTE: this is actually backspace!
            case KeyEvent.KEYCODE_DEL:
                //Position currentPosition = getCurrentPosition();
                //if (currentPosition.character == 0 && currentPosition.line != 0) {
                //    codeAdapter.mergeLines(currentPosition.line, currentPosition.line-1);
                //    return true;
               // }
                
            default:
                if (isKeyEventPrintable(event)) { // printable?
                    if (event.isShiftPressed()) {
                        //codeAdapter.deleteSelection();
                        //codeAdapter.insertText()
                        // TODO: replace the selection with this character.                        
                    }
                }
                
        }
        return false;
    }
    
    public Position getCurrentPosition() {
        return new Position(line, editText.getSelectionStart());
    }

    private void setup() {
        this.editText = (EditText) findViewById(R.id.editText);
        this.lineNumber = (TextView) findViewById(R.id.lineNumber);
        setOnLongClickListener(this);
    }
    
    public void setLineInformation(int line, int maxLine, Spannable spannable, CodeAdapter codeAdapter) {        
        if (this.editText == null)
            setup();
        
        this.codeAdapter = codeAdapter;
        this.line = line;
        
        editText.setText(spannable);
        lineNumber.setText(String.valueOf(line + 1));
        
        // set the width of the line numbers to be enough for the highest line
        // number. as we're using a monospace font, should work out nicely.
        ensureWidth(lineNumber, String.valueOf(maxLine + 1));
    }

    private void ensureWidth(TextView textView, String ensureText) {
        TextPaint paint = textView.getPaint();
        float w = paint.measureText(ensureText);
        textView.setWidth((int) Math.ceil(w));
    }

    public boolean onLongClick(View v) {
        return true; // don't allow the selection bar to come up.
    }
    
}
