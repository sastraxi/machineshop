package com.sastraxi.machineshop.adapters.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import android.content.Context;
import android.text.Spannable;

import com.sastraxi.machineshop.R;
import com.sastraxi.machineshop.project.RemoteFile;

public class JavaCodeAdapter extends CodeAdapter {

	protected static final Pattern BRACES = Pattern.compile("[\\{\\}]");
	protected static final Pattern KEYWORDS = Pattern.compile("\\b(package|import|try|null|float|int|String|catch|this|class|interface|extends|implements|throws|private|new|protected|public|final|static|void|new|return|for|while|break|switch|if|do)\\b");
	protected static final Pattern NUMBERS = Pattern.compile("\\b((\\-)?\\d+|0x[0-9a-fA-F]+)\\b");
	protected static final Pattern ANNOTATIONS = Pattern.compile("\\b(@\\w+)\\b");
	protected static final Pattern TASKS = Pattern.compile("\\b(TODO|FIXME|XXX|NOTE)\\b");
	
	protected static final Pattern JAVADOC_COMMENT_START = Pattern.compile("/\\*\\*");
	protected static final Pattern SINGLELINE_COMMENT_START = Pattern.compile("//");
	protected static final Pattern MULTILINE_COMMENT_START = Pattern.compile("/\\*[^\\*]");
	protected static final Pattern MULTILINE_COMMENT_END = Pattern.compile("\\*/");
	protected static final Pattern STRING_START = Pattern.compile("\""); // double-quote.
	protected static final Pattern STRING_END = Pattern.compile("(^|[^\\\\])\""); // double-quote at the start of the line OR not directly preceded by a slash

    // TODO: new feature, user-assignable styles.
    
	public static final TextContextIdentifier strings =
	    new TextContextIdentifierResource("string",
	            STRING_START, STRING_END,
	            TextContextIdentifier.SINGLE_LINE,
	            R.style.code_strings);
	
    public static final TextContextIdentifier multiline =
        new TextContextIdentifierResource("multiline comment", 
                MULTILINE_COMMENT_START, MULTILINE_COMMENT_END,
                TextContextIdentifier.MULTI_LINE,
                R.style.code_multiline);

    public static final TextContextIdentifier javadoc =
        new TextContextIdentifierResource("javadoc comment",
                JAVADOC_COMMENT_START, MULTILINE_COMMENT_END,
                TextContextIdentifier.MULTI_LINE,
                R.style.code_javadoc);
   
    public static final TextContextIdentifier singleline =
        new TextContextIdentifierResource("javadoc comment",
                SINGLELINE_COMMENT_START, null,
                TextContextIdentifier.SINGLE_LINE_AUTOCLOSE,
                R.style.code_javadoc);

    public static final TextItemIdentifier keywords =
        new TextItemIdentifierResource("keyword",
                KEYWORDS, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE, 0,
                R.style.code_keywords);        

    public static final TextItemIdentifier numbers =
        new TextItemIdentifierResource("number",
                BRACES, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE, 0,
                R.style.code_section_delimiters);        

    public static final TextItemIdentifier braces =
        new TextItemIdentifierResource("brace",
                NUMBERS, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE, 0,
                R.style.code_numbers);      
    
    public static final TextItemIdentifier annotations =
        new TextItemIdentifierResource("annotation",
                ANNOTATIONS, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE, 0,
                R.style.code_annotations);      

    public static final TextItemIdentifier javadoc_tags =
        new TextItemIdentifierResource("javadoc tag",
                ANNOTATIONS, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE, 0,
                R.style.code_javadoc_tags);      

    public static final TextItemIdentifier tasks =
        new TextItemIdentifierResource("task identifier",
                TASKS, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE, 0,
                R.style.code_tasks);
    
	private static final List<TextContextIdentifier> contextIdentifiers = new ArrayList<TextContextIdentifier>();
	private static final Map<Object, TextItemIdentifier[]> contextItems = new HashMap<Object, TextItemIdentifier[]>();
		
	static {
	    
	    contextIdentifiers.add(javadoc);
	    contextIdentifiers.add(multiline);
	    contextIdentifiers.add(strings);
	    contextIdentifiers.add(singleline);
        
	    contextItems.put(null, new TextItemIdentifier[] {keywords, numbers, braces, annotations});        
	    contextItems.put(multiline, new TextItemIdentifier[] {tasks});        
	    contextItems.put(singleline, new TextItemIdentifier[] {tasks});
	    contextItems.put(javadoc, new TextItemIdentifier[] {javadoc_tags});
        
	}
	
	public JavaCodeAdapter(Context context, RemoteFile file) {
		super(context, file);
	}

	@Override
    protected List<TextContextIdentifier> getContextidentifiers() {
        return contextIdentifiers;
    }

    @Override
    public Map<Object, TextItemIdentifier[]> getContextitems() {
        return contextItems;
    }

}
