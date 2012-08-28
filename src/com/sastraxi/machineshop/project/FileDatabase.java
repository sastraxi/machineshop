package com.sastraxi.machineshop.project;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class FileDatabase extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;
    
    private static final String KEY_REMOTE = "remote";
    private static final String KEY_PATH = "path";
    private static final String KEY_MTIME = "mtime";    
    
    private static final String FILE_TABLE_NAME = "file_mtimes";        
    
    private static final String FILE_TABLE_CREATE =
                "CREATE TABLE " + FILE_TABLE_NAME + " (" +
                        KEY_REMOTE  + " TEXT, " +
                        KEY_PATH    + " TEXT, " +
                        KEY_MTIME   + " INTEGER);";    
    private static final String FILE_TABLE_DESTROY = "DROP TABLE "
                 + FILE_TABLE_NAME;
    
    private static final String FILE_WHERE =
            KEY_REMOTE + " = ? AND " +
            KEY_PATH + " = ?";

    public FileDatabase(Context context) {
        super(context, FILE_TABLE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(FILE_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(FILE_TABLE_DESTROY);
        db.execSQL(FILE_TABLE_CREATE);
        // XXX: Ricky Gervais: "I think there's been a rape up there!"
    }
    
    private String[] getFileSelectionArgs(RemoteFile file) {
        String remoteName = file.getRemote().getName();
        String path = file.getPath().toString();
        return new String[]{remoteName, path};
    }
    
    public long getLastModified(RemoteFile file) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor query = db.query(FILE_TABLE_NAME,
                                new String[]{KEY_MTIME},
                                FILE_WHERE,
                                getFileSelectionArgs(file),
                                null, null, null);
        long answer;
        if (query.getCount() == 1) {
            answer = query.getLong(0);    
        } else {
            answer = -1;
        }
        db.close();
        return answer;
    }
    
    public void setLastModified(RemoteFile file, long mtime) {
        SQLiteDatabase db = getWritableDatabase();        
        ContentValues values = new ContentValues();
        values.put(KEY_MTIME, mtime);        
        db.update(FILE_TABLE_NAME, values, FILE_WHERE, getFileSelectionArgs(file));    
        db.close();
    }
    
}
