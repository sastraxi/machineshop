package com.sastraxi.machineshop.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import com.sastraxi.machineshop.project.RemoteFile;
import com.sastraxi.machineshop.util.PathUtils;

public class CodeMirrorEditor extends RelativeLayout {

    public interface ModifiedStateListener {
        public void onModifiedStateChanged(CodeMirrorEditor editor);        
    }
    
    public static final HashMap<String, String> modeMap = new HashMap<String, String>();
    static {
        modeMap.put("js", "javascript");
        modeMap.put("j", "javascript");
        modeMap.put("c", "clike");
        modeMap.put("cc", "clike");
        modeMap.put("cxx", "clike");
        modeMap.put("cpp", "clike");
        modeMap.put("h", "clike");
        modeMap.put("hh", "clike");
        modeMap.put("hxx", "clike");
        modeMap.put("hpp", "clike");
        modeMap.put("java", "clike");
        modeMap.put("cs", "clike");
        modeMap.put("md", "markdown");
        modeMap.put("sql", "mysql");
        modeMap.put("htm", "htmlmixed");
        modeMap.put("html", "htmlmixed");
        modeMap.put("xhtml", "htmlmixed");
        modeMap.put("xml", "xml");
        modeMap.put("svg", "xml");
        modeMap.put("py", "python");
        modeMap.put("sh", "shell");
    }
    
    /**
     * Referred to as container in the javascript.
     * Mainly delegates methods.
     */
    public class JavascriptInterface {

        public void markModified() {
            CodeMirrorEditor.this.markModified();
        }
        
        public void alert(String what) {
            new AlertDialog.Builder(getContext())
                    .setTitle("WebView Alert")
                    .setMessage(what)
                    .show();
        }
        
        public String getMode() {
            String ext = MimeTypeMap.getFileExtensionFromUrl("http://localhost/" + file.getLocalFile().getName());
            return modeMap.get(ext);
        }
        
        public String getMimeType() {
            return PathUtils.getMimeType(file.getLocalFile());
        }
        
        public String getFileContents() {
            FileInputStream fis;
            try {
                fis = new FileInputStream(file.getLocalFile());
                String fileText = PathUtils.streamToString(fis);
                return fileText;
            } catch (FileNotFoundException e) {
                return "";
            }
        }
        
        public void continueSave(String newContents) {
            try {
                FileOutputStream fos = new FileOutputStream(saveFile);
                fos.write(newContents.getBytes());
                if (afterSave != null) {
                    afterSave.run();
                }
            } catch (FileNotFoundException e) {
                Log.e("Editor.continueSave", "Couldn't save", e);
            } catch (IOException e) {
                Log.e("Editor.continueSave", "Couldn't save", e);
            }
        }
        
    }

    private RemoteFile file;
    private WebView webView;   
    
    private Queue<String> queuedCalls = new LinkedList<String>();    
    private File saveFile;
    private Runnable afterSave;    

    private boolean isLoaded = false;
    private boolean isModified = false;
    private List<ModifiedStateListener> listeners = new ArrayList<ModifiedStateListener>();
    
    public CodeMirrorEditor(Context context, RemoteFile file) {
        super(context);
        setupWebView();
        setFile(file);
    }

    public void addListener(ModifiedStateListener l) {
        listeners.add(l);
    }
    
    public void removeListener(ModifiedStateListener l) {
        listeners.remove(l);
    }
    
    public void markModified() {
        if (isModified) return;
        isModified = true;
        for (ModifiedStateListener l: listeners) {
            l.onModifiedStateChanged(this);
        }
    }
    
    public void clearModified() {
        if (!isModified) return;
        isModified = false;
        for (ModifiedStateListener l: listeners) {
            l.onModifiedStateChanged(this);
        }
    }
    
    public boolean isModified() {
        return isModified;
    }

    private void setupWebView() {
        
        webView = new WebView(getContext()) {
            @Override
            public boolean onCheckIsTextEditor() {
                return true;
            }
        };
        
        WebViewClient client = new WebViewClient() {
            
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (url.startsWith("javascript:")) {
                    return super.shouldInterceptRequest(view, url);
                } else {
                    URI uri = URI.create(url);
                    String path = uri.getPath();
                    if (path.startsWith("/")) {
                        path = path.substring(1);
                    }
                    
                    try {
                        MimeTypeMap mime = MimeTypeMap.getSingleton();
                        InputStream fileStream = getResources().getAssets().open(path);
                        return new WebResourceResponse(
                                mime.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url)),
                                "UTF-8",
                                fileStream);
                        
                    } catch (IOException e) {
                        Log.w("WebView", "IOException on loading asset:" + path);
                        return super.shouldInterceptRequest(view, url);
                    }
                }
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.endsWith("editor.html")) {
                    synchronized(queuedCalls) {
                        isLoaded = true;
                        for (String jsURL: queuedCalls) {
                            webView.loadUrl(jsURL);
                        }
                        queuedCalls.clear();
                    }
                }
            }
            
        };
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setEnableSmoothTransition(false);
        webView.getSettings().setNeedInitialFocus(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.addJavascriptInterface(new JavascriptInterface(), "container");
        webView.setWebViewClient(client);        
        
        // allow keyboard focus
        webView.setOnTouchListener(new View.OnTouchListener() {   
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) { 
                    case MotionEvent.ACTION_DOWN: 
                    case MotionEvent.ACTION_UP: 
                        if (!v.hasFocus())
                            v.requestFocus(View.FOCUS_DOWN); 
                        break; 
                } 
                return false; 
            }        
        });
        
        addView(webView, new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        webView.loadUrl("http://baseURI/editor.html");
        webView.requestFocus(View.FOCUS_DOWN);
        
    }
    
    protected void callJS(String objectAndMethod, Object... params) {        
        
        String parameterString = "";
        if (params.length > 0) {            
            JSONArray jsonizer = new JSONArray(Arrays.asList(params));                        
            try {
                parameterString = jsonizer.join(", ");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        String jsURL = "javascript:" + objectAndMethod + "(" + parameterString + ");";
        synchronized (queuedCalls) {
            if (isLoaded) {
                webView.loadUrl(jsURL);
            } else {
                queuedCalls.add(jsURL);
            }
        }
        
    }

    public void setFile(RemoteFile file) {
        this.file = file;
        clearModified();
        callJS("fetchFile");
        webView.requestFocus();
    }

    public RemoteFile getFile() {
        return file;
    }
    
    public void save(Runnable callback) {
        if (!isModified()) return;
        this.saveFile = file.getLocalFile();
        this.afterSave = callback;
        callJS("save"); // control goes through the HTML and then passes on to continueSave()
    }
    
    public void autosave(Runnable callback) {
        if (!isModified()) return;
        this.saveFile = file.getAutosaveFile();
        this.afterSave = callback;
        callJS("save"); // control goes through the HTML and then passes on to continueSave()        
    }
    
}
