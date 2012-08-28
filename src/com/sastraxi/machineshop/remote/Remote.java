package com.sastraxi.machineshop.remote;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.sastraxi.lookmonster.SmartList;
import com.sastraxi.lookmonster.tasks.BackgroundTask;
import com.sastraxi.lookmonster.tasks.BackgroundTaskManager;
import com.sastraxi.machineshop.MachineShopApplication;
import com.sastraxi.machineshop.MultiDashboardActivity;
import com.sastraxi.machineshop.R;
import com.sastraxi.machineshop.RemoteSettingsActivity;
import com.sastraxi.machineshop.project.Project;
import com.sastraxi.machineshop.project.RemoteFile;
import com.sastraxi.machineshop.project.RemoteFileEntry;
import com.sastraxi.machineshop.project.RemoteFolder;
import com.sastraxi.machineshop.remote.FileOperationsQueue.ProgressListener;
import com.sastraxi.machineshop.remote.actions.ConnectAction;
import com.sastraxi.machineshop.remote.actions.RemoteAction;
import com.sastraxi.machineshop.remote.actions.RemoteActionCallback;
import com.sastraxi.machineshop.remote.actions.RemoteFileAction;
import com.sastraxi.machineshop.util.AbsolutePath;
import com.sastraxi.machineshop.util.HostPort;
import com.sastraxi.machineshop.util.PathUtils;

public class Remote implements BackgroundTask, Observer {

    public static final int MODE_LIST_ALL = 0;
    public static final int MODE_LIST_FILES = 1;
    public static final int MODE_LIST_FOLDERS = 2;
    
    private static final boolean VERBOSE = true;
    static {
        // FIXME: remove and use jsch.setKnownHosts(inputStream) in connect()
        JSch.setConfig("StrictHostKeyChecking", "no");
        if (VERBOSE) {
            JSch.setLogger(new com.jcraft.jsch.Logger() {
                public boolean isEnabled(int level) {
                    return true;
                }

                public void log(int level, String message) {
                    Log.v("JSch[" + level + "]", message);
                }
            });
        }
    }
    
    private static final String PREFERENCE_PROJECT_NAMES = "project_names";
    private static final String PREFERENCE_ADDRESS = "address";
    private static final String PREFERENCE_USERNAME = "username";
    
    // TODO preference in main for this:
    private static final int CONNECT_TIMEOUT = 30 * 1000; // 30 seconds

    final String name;
    
    private Handler handler = new Handler();
    private BackgroundTaskManager manager = null;

    private JSch jsch;
    private Session session;
    private FileOperationsQueue fileOps;
    private boolean connected = false;
    private boolean inProgress = false;

    private SmartList<Project> projects;
    private File folder;

    private SharedPreferences settings;
    private final MachineShopApplication app;
    private ExecutorService executor;

    /**
     * The remote's folder is created in app.getRootFolder()
     * 
     * @param app
     * @param name
     */
    public Remote(MachineShopApplication app, String name) {
        this.app = app;
        this.name = name;
        this.jsch = new JSch();

        this.folder = new File(app.getRootFolder(), name);
        this.folder.mkdir();

        this.settings = app.getSharedPreferences(getPreferencesName(), 0);
        PreferenceManager.setDefaultValues(app, R.xml.remote, false);

        // load projects from preference string set.
        this.projects = new SmartList<Project>();
        for (String projectName: this.settings.getStringSet(PREFERENCE_PROJECT_NAMES, new HashSet<String>())) {
            this.projects.add(new Project(this, projectName));
        }
        this.projects.addObserver(this);

        this.fileOps = new FileOperationsQueue(this);
        this.executor = Executors.newFixedThreadPool(1);
    }

    public Project getProject(String name) {
        for (Project project: projects) {
            if (project.getName().equals(name)) {
                return project;
            }
        }
        return null;
    }
    
    public String getPreferencesName() {
        return "remotes." + this.name;
    }

    public String getName() {
        return name;
    }

    public SharedPreferences getSettings() {
        return settings;
    }

    public boolean isConnected() {
        return connected;
    }

    public SmartList<Project> getProjects() {
        return projects;
    }

    public File getFolder() {
        return folder;
    }

    public HostPort getAddress() {
        String addressString = settings.getString(PREFERENCE_ADDRESS, null);
        if (addressString != null) { return HostPort.fromString(addressString, 22); }
        return null;
    }

    public void setAddress(HostPort address) {
        String addressString = null;
        if (address != null) {
            addressString = address.toString();
        }
        settings.edit().putString(PREFERENCE_ADDRESS, addressString).commit();
    }

    public String getUsername() {
        return settings.getString(PREFERENCE_USERNAME, null);
    }

    public void setUsername(String username) {
        settings.edit().putString(PREFERENCE_USERNAME, username).commit();
    }

    // run on background thread
    public synchronized void connect() {
        if (isConnected()) return;

        try {
            
            setProgress("Connecting to " + getName());

            getApp().registerKeyPair(jsch);
            //jsch.setKnownHosts(inputStream);
            HostPort hp = getAddress();
            session = jsch.getSession(getUsername(), hp.getHost(), hp.getPort());
            session.setUserInfo(new UserInfo(this));
            session.connect(CONNECT_TIMEOUT);
            connected = true;
            promptAddKey();

            finishProgress();            

        } catch (final JSchException e) {

            // correctly show as disconnected.
            connected = false;
            finishProgress(); 
            
            // show a message in certain circumstances.
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    
                    new AlertDialog.Builder(getActivity()).setTitle(name).setMessage(e.getMessage())
                    .setPositiveButton("Home", new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            MultiDashboardActivity.launchIntent(getActivity());
                        }
                    })
                    .setNeutralButton("Settings", new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            RemoteSettingsActivity.launchIntent(getActivity(), Remote.this);
                        }
                    })
                    .show();
                    
                }
            });
            Log.e("Remote.connect(" + name + ")", Log.getStackTraceString(e));
            // TODO specially handle "Auth cancel"?
            
        } catch (NotConnectedException e) {
            // from promptAddKey. Can't get here!
        }
    }

    private Activity getActivity() {
        return getApp().getCurrentActivity();
    }

    // run only from DisconnectAction
    public synchronized void disconnect()  {
        if (!isConnected()) return;

        setProgress("Disconnecting from " + getName());
        
        // file operations might get mangled if we don't wait.
        if (!fileOps.isComplete()) {
            final Semaphore semaphore = new Semaphore(0);

            ProgressListener listener = new ProgressListener() {
                public void onUpdateBatch(int operaton, int numFiles) {
                }

                public void onStartFile(int operation, RemoteFile file) {
                }

                public void onProgress(int operation, long bytesTransferred, long bytesTotal) {
                }

                public void onFinishFile(int operation, RemoteFile file) {
                }

                public void finishedBatch(int operation) {
                    if (fileOps.isComplete()) {
                        semaphore.release(1);
                    }
                }
            };

            fileOps.addProgressListener(listener);
            while (true) {
                try {
                    semaphore.acquire(1);
                    break;
                } catch (InterruptedException e) {
                }
            }
            fileOps.removeProgressListener(listener);
        }

        // now disconnect on this thread, and return.
        fileOps.disconnect();
        session.disconnect();
        
        connected = false;        
        finishProgress();

    }

    // run on background thread
    public synchronized ChannelSftp createSFTP() throws NotConnectedException {
        if (!isConnected()) throw new NotConnectedException();
        try {
            ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            return sftp;
        } catch (JSchException e) {
            Log.e("Remote.openSFTP(" + name + ")", Log.getStackTraceString(e));
            return null;
        }
    }
    
    // run on background thread
    protected synchronized void promptAddKey() throws NotConnectedException {
        if (!isConnected()) throw new NotConnectedException();

        // get the authorized_keys file.
        // XXX route file access through fileOps?
        String authorizedKeyString;
        try {
            ChannelSftp sftp = createSFTP();
            InputStream inputStream = sftp.get(sftp.getHome() + "/.ssh/authorized_keys");
            authorizedKeyString = PathUtils.streamToString(inputStream);
            sftp.disconnect();
        } catch (SftpException e) {
            // can't get the file... assume it's empty.
            Log.w("Remote.promptAddKey(" + name + ")", "--", e);
            authorizedKeyString = "";
        }

        if (!authorizedKeyString.endsWith("\n")) {
            addNewlineOnInstall = true;
        }

        InputStream publicKeyStream = getApp().getPublicKeyStream();
        String publicKeyString = PathUtils.streamToString(publicKeyStream);

        if (!authorizedKeyString.contains(publicKeyString)) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Install Public Key?")
                            .setMessage(
                                    "Machine Shop can authorize its public key on " + getName()
                                            + ". This will allow you to connect as " + getUsername()
                                            + " without typing a password.")
                            .setPositiveButton("Authorize", new OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    handler.post(new Runnable() {
                                        public void run() {
                                            try {
                                                Remote.this.installKey();
                                            } catch (NotConnectedException e) {
                                                Log.wtf("Somehow...", Log.getStackTraceString(e));
                                            }
                                        }
                                    });
                                }
                            }).setNeutralButton("Not Now", new OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).setNegativeButton("Never", new OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // settings.put(SETTINGS.NEVER_INSTALL_PUBKEY,
                                    // true);
                                    dialog.dismiss();
                                }
                            }).show();
                }
            });
        } else {
            Log.i("Remote.promptAddKey(" + name + ")", "Key already in ~/.ssh/authorized_keys");
            // settings.put(SETTINGS.NEVER_INSTALL_PUBKEY, true);
        }
    }

    private boolean addNewlineOnInstall = false;
    private String message;
    private int progress;
    private int maxProgress;    
    
    // run on background thread
    protected synchronized void installKey() throws NotConnectedException {
        InputStream publicKeyStream = getApp().getPublicKeyStream();
        // XXX route file access through fileOps?
        // Also ensures ~, ~/.ssh, and ~/.ssh/authorized_keys are not
        // group-writable,
        // see http://linux.die.net/man/1/ssh-copy-id
        try {

            ChannelSftp sftp = createSFTP();
            String homePath = sftp.getHome();
            String path = homePath + "/.ssh/authorized_keys";
            try { 
                sftp.mkdir(homePath + "/.ssh");
            } catch (SftpException e) {
                // just continue: we don't care if the directory already exists
            }

            if (addNewlineOnInstall) {
                sftp.put(new ByteArrayInputStream("\n".getBytes()), path, null, ChannelSftp.APPEND);
            }
            // FIXME: doesn't work on cuba.technifar.com
            sftp.put(publicKeyStream, path, null, ChannelSftp.APPEND);
            sftp.put(new ByteArrayInputStream("\n".getBytes()), path, null, ChannelSftp.APPEND);
            // XXX: ChannelSftp seems to not write the last newline even if it
            // exists, so add one in.

            // remove group-writable from ~, set recommended permissions
            // for ~/.ssh and ~/.ssh/authorized_keys
            Integer homePermissions = sftp.stat(homePath).getPermissions();
            if ((homePermissions & Integer.parseInt("020", 8)) != 0) {
                sftp.chmod(homePermissions & Integer.parseInt("757", 8), homePath);
            }
            sftp.chmod(Integer.parseInt("700", 8), homePath + "/.ssh");
            sftp.chmod(Integer.parseInt("600", 8), path);

            sftp.disconnect();
        
        } catch (SftpException e) {
            Log.e("Remote.installKey(" + name + ")", Log.getStackTraceString(e));
        }
    }

    // run on background thread.
    public synchronized List<RemoteFileEntry> listFolder(String remoteFolder, int mode, boolean showHiddenFiles) throws NotConnectedException {

        setProgress("Listing " + remoteFolder);
        
        List<RemoteFileEntry> results = new ArrayList<RemoteFileEntry>();
        ChannelSftp sftp = createSFTP();
        try {
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> entries = sftp.ls(remoteFolder);
            setProgress("Listing " + remoteFolder);
            for (int i = 0; i < entries.size(); ++i) {                
                ChannelSftp.LsEntry remoteEntry = entries.get(i);
                setProgress("Listing " + remoteFolder, i, entries.size());
                
                // exclude the virtual directories added by the channel
                if (remoteEntry.getFilename().equals(".") || remoteEntry.getFilename().equals(".."))
                    continue;
                
                if (!showHiddenFiles && remoteEntry.getFilename().startsWith("."))
                    continue;
                
                boolean isFolder = remoteEntry.getAttrs().isDir(); 
                if (isFolder && mode == MODE_LIST_FILES) continue;
                if (!isFolder && mode == MODE_LIST_FOLDERS) continue;
                
                AbsolutePath remotePath = new AbsolutePath(PathUtils.joinPath(remoteFolder, remoteEntry.getFilename()));
                if (isFolder) {
                    results.add(new RemoteFolder(this, remotePath));
                } else {
                    results.add(new RemoteFile(this, remotePath));
                }
                
            }
        } catch (SftpException e) {
            Log.e("Remote.listFolder", remoteFolder, e);
        }
        sftp.disconnect();
        
        Collections.sort(results);
        finishProgress();
        
        return results;
    }

    public boolean isActive() {
        return inProgress;
    }

    public boolean isCancelable() {
        return false;
    }

    public boolean isIndeterminate() {
        return progress == -1;
    }

    public int getProgress() {
        return progress;
    }

    public int getMaxProgress() {
        return maxProgress;
    }

    public int getSubtask() {
        return 0;
    }

    public int getMaxSubtask() {
        return 0;
    }

    public String getMessage() {
        return message;
    }

    public boolean showOnActionBar() {
        return true;
    }

    public String getActionBarMessage() {
        return "Active";
    }
    
    /**
     * Indeterminate version.
     */
    public void setProgress(String message) {
        this.message = message;
        this.inProgress = true;
        this.progress = -1;
        if (this.manager != null) {
            this.manager.onProgress(this);
        }
    }
    
    public void setProgress(String message, int progress, int max) {
        this.progress = progress;
        this.maxProgress = max;
        this.inProgress = true;
        this.message = message;
        if (this.manager != null) {
            this.manager.onProgress(this);
        }
    }
    
    public void finishProgress() {
        this.inProgress = false;
        if (this.manager != null) {
            this.manager.onProgress(this);
        }
    }

    /**
     * Removes all tasks from the old manager, puts them on the new one.
     */
    public void setManager(BackgroundTaskManager manager) {
        if (manager == this.manager) return;
        if (this.manager != null) {
            manager.removeTask(this);
            manager.removeTask(fileOps.getDownloadTask());
            manager.removeTask(fileOps.getUploadTask());
        }
        this.manager = manager;
        manager.addTask(this);
        manager.addTask(fileOps.getDownloadTask());
        manager.addTask(fileOps.getUploadTask());
    }

    public static Remote create(MachineShopApplication app, String name, HostPort address, String username) {
        Remote remote = new Remote(app, name);
        remote.setAddress(address);
        remote.setUsername(username);
        return remote;
    }

    public MachineShopApplication getApp() {
        return app;
    }

    /**
     * Updates the preferences file when we mess with the project list.
     */
    public void update(Observable observable, Object data) {
        
        if (observable != this.projects) {
            Log.w("Remote.update", "got unknown observable" + observable.toString());
            return;
        }
        
        Set<String> projectNameSet = new HashSet<String>();
        for (Project project: this.projects) {
            projectNameSet.add(project.getName());
        }
        settings.edit().putStringSet(PREFERENCE_PROJECT_NAMES, projectNameSet).commit();
        
    }

    public void queueAction(final RemoteAction action) {

        // continue this function once we're connected. 
        if (!isConnected() && !(action instanceof ConnectAction)) {
            RemoteService.queue(
                    getApp().getCurrentActivity(),
                    new ConnectAction(this, new RemoteActionCallback() {                
                        @Override
                        public void run(RemoteAction connectAction) {
                            if (!isConnected()) {
                                // TODO show "could not connect" error message.
                            } else {
                                // if we did connect, continue with the original action.
                                queueAction(action);
                            }
                        }
                    }));
            return;
        }
        
        if (action instanceof RemoteFileAction) {
            fileOps.queueAction((RemoteFileAction) action);            
        } else {
            executor.execute(new Runnable() {                
                @Override
                public void run() {
                    action.execute(Remote.this);                    
                }
            });
        }        
        
    }

    public interface StateChangedListener {
        /**
         * Called when a remote connects or disconnects.
         */
        public void onStateChanged(Remote remote);
    }   
   
    
    private List<StateChangedListener> listeners = new ArrayList<StateChangedListener>();
    
    public void addListener(StateChangedListener l) {
        listeners.add(l);
    }
    
    public void removeListener(StateChangedListener l) {
        listeners.add(l);
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (o.getClass() != this.getClass()) return false;
        
        Remote other = (Remote) o;
        return other.getName().equals(getName());        
    }

    Session getSession() {
        return session;
    }
}
