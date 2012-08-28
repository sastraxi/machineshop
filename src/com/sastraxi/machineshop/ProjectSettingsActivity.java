package com.sastraxi.machineshop;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.sastraxi.machineshop.project.Project;
import com.sastraxi.machineshop.remote.Remote;

public class ProjectSettingsActivity extends PreferenceActivity {

    private static final String INTENT_EXTRA_REMOTE_NAME = "remote_name";
    private static final String INTENT_EXTRA_PROJECT_NAME = "project_name";
    
    private Project project;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.project_settings, menu);
        return true;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String remoteName = getIntent().getStringExtra(INTENT_EXTRA_REMOTE_NAME);
        MachineShopApplication app = (MachineShopApplication) getApplicationContext();
        Remote remote = app.getRemote(remoteName);

        String projectName = getIntent().getStringExtra(INTENT_EXTRA_PROJECT_NAME);
        project = remote.getProject(projectName);
        
        PreferenceManager prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName(project.getPreferencesName());
        prefMgr.setSharedPreferencesMode(MODE_PRIVATE);

        getActionBar().setTitle("Settings for " + projectName + " on " + remoteName);
        addPreferencesFromResource(R.xml.project);        
        
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public static void launchIntent(Context context, Project project) {
        
        Intent intent = new Intent(context, ProjectSettingsActivity.class);
        intent.putExtra(INTENT_EXTRA_REMOTE_NAME, project.getRemote().getName());
        intent.putExtra(INTENT_EXTRA_PROJECT_NAME, project.getName());
        context.startActivity(intent);
        
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, MultiDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;

            case R.id.delete:
                // TODO extra-special warning if this project has unmodified files
                // or is in some other way not synched with the server.
                new AlertDialog.Builder(this)
                        .setTitle("Delete " + project.getName())
                        .setMessage("All settings and unsynchronized/unsaved files will be lost.")
                        .setPositiveButton("Delete", new OnClickListener() {                            
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                project.getRemote().getProjects().remove(project);
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .setNegativeButton("Cancel", new OnClickListener() {                            
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();                
                break;
                
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

}
