package com.javijuol.signalmap.content;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.javijuol.signalmap.app.Application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Javier Juan Oltra <javijuol@gmail.com>
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "db.sqlite";
    public static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Application.FIRST_TIME = true;
        for(String sql: Contract.getDDL(false)) {
            db.execSQL(sql);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(Contract.NetworkSignal.SQL_CREATE);
    }

    private void dropColumn(@NonNull SQLiteDatabase db,
                            @NonNull String createTableCmd,
                            @NonNull String tableName,
                            @NonNull String[] colsToRemove) {

        List<String> updatedTableCols = getTableColumns(db, tableName);
        // Remove the columns we don't want anymore from the table's list of columns
        updatedTableCols.removeAll(Arrays.asList(colsToRemove));
        // Prepare the query
        String colsSeparated = TextUtils.join(",", updatedTableCols);
        // Rename actual table to "_old"
        db.execSQL("ALTER TABLE " + tableName + " RENAME TO " + tableName + "_old;");
        // Creating the table on its new format (no redundant columns)
        db.execSQL(createTableCmd);
        // Populating the table with the data
        db.execSQL("INSERT INTO " + tableName + "(" + colsSeparated + ") SELECT " + colsSeparated + " FROM " + tableName + "_old;");
        // Remove the "_old" table
        db.execSQL("DROP TABLE " + tableName + "_old;");

    }

    private List<String> getTableColumns(@NonNull SQLiteDatabase db, @NonNull String tableName) {

        Cursor cur = db.rawQuery("PRAGMA table_info(" + tableName + ");", null);
        List<String> columns = new ArrayList<>(cur.getCount());
        while (cur.moveToNext())
            columns.add(cur.getString(cur.getColumnIndex("name")));

        cur.close();
        return columns;

    }
}
