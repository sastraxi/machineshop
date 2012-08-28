package com.sastraxi.machineshop;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.sastraxi.machineshop.remote.Remote;

public class RemoteSettingsActivity extends PreferenceActivity {

    private static final String INTENT_EXTRA_REMOTE_NAME = "remote_name";
    
    private Remote remote;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.remote_settings, menu);
        return true;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String remoteName = getIntent().getStringExtra(INTENT_EXTRA_REMOTE_NAME);
        MachineShopApplication app = (MachineShopApplication) getApplicationContext();
        remote = app.getRemote(remoteName);
        
        PreferenceManager prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName(remote.getPreferencesName());
        prefMgr.setSharedPreferencesMode(MODE_PRIVATE);

        getActionBar().setTitle("Settings for " + remoteName);
        addPreferencesFromResource(R.xml.remote);        
        
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final MachineShopApplication app = (MachineShopApplication) getApplicationContext();
        
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, MultiDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                break;
            
            case R.id.delete:
                // TODO extra-special warning if any projects have unmodified files
                // or are in any other way not synched with the server.
                new AlertDialog.Builder(this)
                        .setTitle("Delete " + remote.getName())
                        .setMessage("All projects and settings will be lost.")
                        .setPositiveButton("Delete", new OnClickListener() {                            
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                app.getRemotes().remove(remote);
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

    public static void launchIntent(Context context, Remote remote) {
        
        Intent intent = new Intent(context, RemoteSettingsActivity.class);
        intent.putExtra(INTENT_EXTRA_REMOTE_NAME, remote.getName());
        context.startActivity(intent);
        
    }

}
