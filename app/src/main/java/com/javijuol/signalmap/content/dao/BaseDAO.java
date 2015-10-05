package com.javijuol.signalmap.content.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.javijuol.signalmap.app.Application;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Javier Juan Oltra <javijuol@gmail.com>
 */
public abstract class BaseDAO<T> {

    protected ContentResolver contentResolver;

    public BaseDAO(@NonNull Context context) {
        this.contentResolver = context.getContentResolver();
    }

    public BaseDAO(@NonNull ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public Uri createNew(@NonNull T bean){
        ContentValues cv = mapToDB(bean);

        try {
            Uri returnedUri = contentResolver.insert(getCreateUri(), cv);
            if(Application.DEVELOPER_MODE) Log.i(Application.DEBUG_TAG, "InsertUri: " + returnedUri.toString());
            return returnedUri;
        } catch (IllegalArgumentException ex) {
            if(Application.DEVELOPER_MODE) Log.e(Application.DEBUG_TAG, ex.getMessage());
        }
        return null;
    }

    public @Nullable T findById(long id) {
        List<T> list = find(getFindByIdUri(id), null, null, null);

        if(list.size() > 0)
            return list.get(0);

        return null;
    }

    public boolean deleteById(long id) {
        int count = contentResolver.delete(getDeleteByIdUri(id), null, null);
        if(count == 1)
            if(Application.DEVELOPER_MODE) Log.i(Application.DEBUG_TAG, "Deleted id: " + id);

        return (count == 1);
    }

    public boolean deleteAll() {
        int count = contentResolver.delete(getBaseUri(), null, null);
        if(Application.DEVELOPER_MODE) Log.i(Application.DEBUG_TAG, "Deleted all: " + count);
        return count > 0;
    }

    public boolean update(@NonNull T bean, long id) {
        int count = contentResolver.update(
                Uri.withAppendedPath(getBaseUri(), Long.toString(id)),
                mapToDB(bean),
                null,
                null
        );
        if (count == 1)
            if (Application.DEVELOPER_MODE) Log.i(Application.DEBUG_TAG, "Updated id: " + id);

        return (count == 1);
    }

    protected @NonNull Uri getDeleteByIdUri(long id){
        return Uri.parse(getBaseUri() + "/" + id);
    }

    protected @NonNull Uri getFindByIdUri(long id){
        return Uri.parse(getBaseUri() + "/" + id);
    }

    protected @NonNull Uri getCreateUri(){
        return getBaseUri();
    }

    protected @NonNull Uri getUpdateUri(long id){
        return Uri.parse(getBaseUri() + "/" + id);
    }

    protected @NonNull List<T> findWithCursor(@Nullable Cursor cursor) {
        List<T> beans = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext())
                beans.add(mapToBean(cursor));

            cursor.close();
        }
        return beans;
    }

    protected @NonNull List<T> find(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArguments, String sortOrder) {
        Cursor cursor = contentResolver.query(uri, null, selection, selectionArguments, sortOrder);
        return findWithCursor(cursor);
    }

    public @Nullable Cursor cursorAll(){
        return contentResolver.query(
                getBaseUri(),
                null,
                null,
                null,
                null
        );
    }

    public @NonNull List<T> findAll() {
        return findWithCursor(cursorAll());
    }

    protected abstract @NonNull Uri getBaseUri();

    public abstract T mapToBean(@NonNull Cursor cursor);

    protected abstract @NonNull ContentValues mapToDB(@NonNull T bean);
}
