package com.sastraxi.machineshop;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.sastraxi.lookmonster.SmartList;
import com.sastraxi.machineshop.adapters.MultiDashboardAdapter;
import com.sastraxi.machineshop.adapters.RemoteConnectionsAdapter;
import com.sastraxi.machineshop.adapters.RemoteSpinnerAdapter;
import com.sastraxi.machineshop.project.Project;
import com.sastraxi.machineshop.project.RemoteFolder;
import com.sastraxi.machineshop.remote.Remote;
import com.sastraxi.machineshop.remote.Remote.StateChangedListener;
import com.sastraxi.machineshop.util.AbsolutePath;
import com.sastraxi.machineshop.util.HostPort;

public class MultiDashboardActivity extends BaseActivity implements StateChangedListener, Observer {

    private static String INTENT_EXTRA_REMOTE_NAME = "remote_name";
    
    private LinearLayout layout;
    private SmartList<Remote> remotes;
    
    // the corresponding list views:
    // adapters[i] --> layout.getChildAt(i);
    private List<MultiDashboardAdapter> adapters =
            new ArrayList<MultiDashboardAdapter>();

    private View actionBarView;
    
    /**
     * Shows a horizontally-scrolling view of all projects on a given Remote.
     */
    public static void launchIntent(Context context, Remote remote) {
        Intent intent = new Intent(context, MultiDashboardActivity.class);
        intent.putExtra(INTENT_EXTRA_REMOTE_NAME, remote.getName());
        context.startActivity(intent);
    }
    
    /**
     * Shows every Project on every Remote.
     */
    public static void launchIntent(Context context) {
        Intent intent = new Intent(context, MultiDashboardActivity.class);
        context.startActivity(intent);
    }
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.multi_dashboard, menu);
        return true;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayShowCustomEnabled(true);
        getActionBar().setCustomView(R.layout.multi_dashboard_actionbar);
        // TODO hook up spinner
        
        setContentView(R.layout.multi_dashboard);
        layout = (LinearLayout) findViewById(R.id.lists);       
        
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LinearLayout dialogLayout;
        switch (item.getItemId()) {
            case R.id.add_remote:

                dialogLayout = new LinearLayout(this);
                dialogLayout.setOrientation(LinearLayout.VERTICAL);                       
                
                final EditText remoteName = new EditText(this);
                remoteName.setHint("Computer name");
                remoteName.setSingleLine(true);
                
                final EditText hostport = new EditText(this);
                hostport.setHint("Host (:port)");
                hostport.setSingleLine(true);
                InputFilter addressFilter = new InputFilter() {
                    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) { 
                        for (int i = start; i < end; i++) { 
                            char ch = source.charAt(i);
                            if (!Character.isLetterOrDigit(ch) && ch != ':' && ch != '.' && ch != '-') { 
                                return "";
                            } 
                        } 
                        return null; 
                    } 
                }; 
                hostport.setFilters(new InputFilter[]{addressFilter});
                
                final EditText username = new EditText(this);
                username.setHint("Username");
                username.setSingleLine(true);

                final AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle("Add Remote Connection")
                        .setMessage("Machine Shop connects to your computers using SSH.")
                        .setView(dialogLayout)
                        .setPositiveButton("Add", new OnClickListener() {                        
                            public void onClick(DialogInterface dialog, int which) {
                                
                                String[] parts = hostport.getText().toString().split(":");
                                int port = 22;
                                if (parts.length == 2) {
                                    port = Integer.parseInt(parts[1]);
                                }
                                String host = parts[0];
                                
                                Remote remote = Remote.create(getApp(),
                                        remoteName.getText().toString(),
                                        new HostPort(host, port),
                                        username.getText().toString());
                                
                                getApp().addRemote(remote);
                                
                            }
                        })
                        .setNegativeButton("Cancel", new OnClickListener() {                        
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create();
                
                remoteName.setOnEditorActionListener(new OnEditorActionListener() {                
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        hostport.requestFocus(View.FOCUS_DOWN);
                        return true;
                    }
                });
                hostport.setOnEditorActionListener(new OnEditorActionListener() {                
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        username.requestFocus(View.FOCUS_DOWN);
                        return true;
                    }
                });           
                username.setOnEditorActionListener(new OnEditorActionListener() {                
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        Button addButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                        addButton.performClick();
                        return true;
                    }
                });
                
                TextWatcher enableAdd = new TextWatcher() {                
                    public void onTextChanged(CharSequence s, int start, int before, int count) { }                
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }                
                    public void afterTextChanged(Editable s) {
                        Button addButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                        if (remoteName.getText().length() != 0 &&
                            username.getText().length() != 0 &&
                            hostport.getText().length() != 0) {
                            addButton.setEnabled(true);
                        } else {
                            addButton.setEnabled(false);
                        }                    
                    }
                };
                remoteName.addTextChangedListener(enableAdd);
                hostport.addTextChangedListener(enableAdd);
                username.addTextChangedListener(enableAdd);
                
                dialogLayout.addView(remoteName);
                dialogLayout.addView(hostport);            
                dialogLayout.addView(username);           
                
                dialog.show();

                Button addButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                addButton.setEnabled(false);            
                
                break;

            case R.id.add_project:
                
                dialogLayout = new LinearLayout(this);
                dialogLayout.setOrientation(LinearLayout.VERTICAL);  
                
                final Spinner remoteSpinner = new Spinner(this);
                remoteSpinner.setAdapter(new RemoteSpinnerAdapter(this));
                dialogLayout.addView(remoteSpinner);
                if (remotes != getApp().getRemotes()) {
                    // this dashboard is only showing projects from a given Remote,
                    // select this Remote by default.
                    Remote selectedRemote = remotes.get(0);
                    remoteSpinner.setSelection(getApp().getRemotes().indexOf(selectedRemote));
                }
                
                final EditText projectNameEdit = new EditText(this);
                projectNameEdit.setSingleLine(true);
                dialogLayout.addView(projectNameEdit);                
                
                new AlertDialog.Builder(this)
                        .setTitle("Add Project")
                        .setView(dialogLayout)
                        .setPositiveButton("Add",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        
                                        String projectName = projectNameEdit.getText().toString();
                                        Remote selectedRemote = (Remote) remoteSpinner.getSelectedItem();
                                        
                                        // TODO actually let them configure the remote folder HERE.
                                        RemoteFolder remotePath = new RemoteFolder(selectedRemote, new AbsolutePath("/"));                                        
                                        
                                        Project newProject = Project.create(selectedRemote, projectName, remotePath);
                                        selectedRemote.getProjects().add(newProject);
                                        
                                        dialog.dismiss();
                                        
                                        ProjectSettingsActivity.launchIntent(MultiDashboardActivity.this, newProject);
                                        
                                    }
                                })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                    }
                                }).show();
                
                break;
                
            case R.id.manage_connections:
                
                RemoteConnectionsAdapter adapter = new RemoteConnectionsAdapter(this);
                AlertDialog connectionsDialog = new AlertDialog.Builder(this)
                    .setTitle("Connections Manager")
                    .setCancelable(true)
                    .setAdapter(adapter, null)
                    .setNeutralButton("Close", new OnClickListener() {                        
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create();

                connectionsDialog.show();
                adapter.setAsAdapterOn(connectionsDialog.getListView());
                
                break;
        
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        // attach the background task manager's action bar item.
        if (actionBarView == null) {
            actionBarView = getApp().getBackgroundTaskManager().getActionBarView(true);
        }
        LinearLayout customView = (LinearLayout) getActionBar().getCustomView();
        customView.addView(actionBarView, 0);
        
        String remoteName = getIntent().getStringExtra(INTENT_EXTRA_REMOTE_NAME);
        
        if (remoteName == null) {
            remotes = getApp().getRemotes();
        } else {
            remotes = new SmartList<Remote>();
            Remote remote = getApp().getRemote(remoteName);            
            remotes.add(remote);
        }
        
        recreateLists();
        
        // register for (dis)connect notices and new/deleted remotes/projects
        remotes.addObserver(this);        
        for (Remote remote: remotes) {  
            remote.getProjects().addObserver(this);   
            remote.addListener(this);
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();

        // detach the background task manager's action bar item.
        LinearLayout customView = (LinearLayout) getActionBar().getCustomView();
        customView.removeView(actionBarView);
     
        // de-register [(dis)connect notices and new/deleted remotes/projects]
        remotes.deleteObserver(this);        
        for (Remote remote: remotes) {        
            remote.getProjects().deleteObserver(this);       
            remote.removeListener(this);
        }        
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStateChanged(Remote remote) {
        // XXX: loop through each, probably not a performance concern as we won't have thousands of projects...
        for (MultiDashboardAdapter adapter: adapters) {
            if (adapter.getProject().getRemote().equals(remote)) {
                adapter.notifyConnectionChange();
            }
        }
    }

    /**
     * Updates come from Remotes or their project lists.
     */
    @Override
    public void update(Observable observable, Object data) {
        recreateLists();
    }

    private void recreateLists() {
        adapters.clear();
        layout.removeAllViews();        
        
        for (Remote remote: remotes) {
            for (final Project project: remote.getProjects()) {

                View projectView = getLayoutInflater().inflate(R.layout.multi_dashboard_project, layout, false);
                
                // TODO create + subscribe to project settings changes  
                
                ((TextView) projectView.findViewById(R.id.projectName)).setText(project.getName());
                ((TextView) projectView.findViewById(R.id.remoteName)).setText("on " + remote.getName());
                
                MultiDashboardAdapter adapter = new MultiDashboardAdapter(this, project);
                adapters.add(adapter);
                adapter.setAsAdapterOn((ListView) projectView.findViewById(R.id.list)); 
                
                layout.addView(projectView);
                layout.addView(getLayoutInflater().inflate(R.layout.vertical_divider, layout, false));
                
            }
        }
        
        // remove last border
        if (layout.getChildCount() != 0) {
            layout.removeViewAt(layout.getChildCount()-1);
        }
        
    }
    
}
