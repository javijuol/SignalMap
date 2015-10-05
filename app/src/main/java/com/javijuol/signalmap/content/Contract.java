package com.javijuol.signalmap.content;

import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Javier Juan Oltra <javijuol@gmail.com>
 */
public class Contract {
    public static final String AUTHORITY = "com.javijuol.signalmap.content";

    public static final class NetworkSignal implements BaseColumns {
        public static final String TABLE_NAME = "network_signal";
        public static final Uri URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

        public static final String FIELD_LATITUDE 	    = "lat";
        public static final String FIELD_LONGITUDE      = "lng";
        public static final String FIELD_STRENGTH	    = "str";
        public static final String FIELD_TYPE	        = "type";
        public static final String FIELD_CREATED_AT 	= "created_at";

        public static final String SQL_CREATE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
                        + _ID 					    + " INTEGER PRIMARY KEY" + ","
                        + FIELD_LATITUDE            + " NUMERIC" + ","
                        + FIELD_LONGITUDE           + " NUMERIC" + ","
                        + FIELD_STRENGTH 			+ " INTEGER" + ","
                        + FIELD_TYPE                + " INTEGER" + ","
                        + FIELD_CREATED_AT 		    + " INTEGER " + ");";

        public static final String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    public static List<String> getDDL(boolean withDrop){
        List<String> result = new ArrayList<>();
        try {
            for (Class clazz : Contract.class.getDeclaredClasses()) {
                if (BaseColumns.class.isAssignableFrom(clazz)) {
                    if(withDrop){
                        result.add((String)clazz.getField("SQL_DROP").get(null));
                    }
                    result.add((String)clazz.getField("SQL_CREATE").get(null));
                }
            }
        }catch (Exception e){
            Log.i(Contract.class.getSimpleName(), e.getMessage());
        }
        return result;
    }
}
