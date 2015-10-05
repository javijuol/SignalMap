package com.javijuol.signalmap.content;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.javijuol.signalmap.app.Application;

import java.util.Arrays;

/**
 * @author Javier Juan Oltra <javijuol@gmail.com>
 */
public class Provider extends ContentProvider {

    public static final int NETWORK_SIGNAL      = 10;
    public static final int NETWORK_SIGNAL_ID   = 11;

    private static final String TYPE_ROOT = "vnd.android.cursor.dir/vnd." + Contract.AUTHORITY + ".";

    private static UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(Contract.AUTHORITY, Contract.NetworkSignal.TABLE_NAME, NETWORK_SIGNAL);
        uriMatcher.addURI(Contract.AUTHORITY, Contract.NetworkSignal.TABLE_NAME + "/#", NETWORK_SIGNAL_ID);
    }

    private SQLiteDatabase database;

    @Override
    public boolean onCreate() {
        DatabaseHelper databaseHelper = new DatabaseHelper(getContext());

        try {
            database = databaseHelper.getWritableDatabase();
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case NETWORK_SIGNAL:
            case NETWORK_SIGNAL_ID:
                return TYPE_ROOT + Contract.NetworkSignal.TABLE_NAME;
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        switch (uriMatcher.match(uri)) {
            case NETWORK_SIGNAL:
            {
                if (sortOrder == null)
                    sortOrder = BaseColumns._ID + " ASC";
            }
            break;
            case NETWORK_SIGNAL_ID:
            {
                String selection2 = BaseColumns._ID + "=" + uri.getLastPathSegment();
                if (selection != null)
                    selection += " AND ( " + selection2 + " ) ";
                else
                    selection = selection2;

                if (sortOrder == null)
                    sortOrder = BaseColumns._ID + " ASC";

            }
            break;
        }

        return queryDefault(uri, projection, selection, selectionArgs, sortOrder);
    }

    private @Nullable Cursor queryDefault(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder){
        String table = getTableFromURI(uri);

        if(Application.DEVELOPER_MODE) Log.i(Provider.class.getSimpleName(), "SQL SELECT " + Arrays.toString(projection) + " FROM " + table + " WHERE " + selection + " " + Arrays.toString(selectionArgs) + " ORDER BY " + sortOrder);

        Cursor c = database.query(true, table, projection, selection, selectionArgs, null, null, sortOrder, null);
        if (c != null)
            c.setNotificationUri(getContext().getContentResolver(), uri);

        return c;
    }

    private String getTableFromURI(Uri uri){
        String type = getType(uri);
        return type.substring(type.lastIndexOf(".") +1);
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        long id = database.insert(getTableFromURI(uri), null, values);

        if (id != -1) {
            getContext().getContentResolver().notifyChange(uri, null);
            return ContentUris.withAppendedId(uri, id);
        } else {
            return null;
        }
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int count = updateById(uri.getLastPathSegment(), values, uri);
        if (count > 0)
            getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    private int updateById(String id, ContentValues values, Uri uri){
        String selection = BaseColumns._ID + " = ? ";
        String[] selectionArgs = new String[] { id };
        return database.update(getTableFromURI(uri), values, selection, selectionArgs);
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        int count = deleteById(uri.getLastPathSegment(), uri);
        if (count > 0)
            getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    private int deleteById(String id, Uri uri){
        String selection = BaseColumns._ID + " = ? ";
        String[] selectionArgs = new String[] { id };
        return database.delete(getTableFromURI(uri), selection, selectionArgs);
    }
}
