package com.javijuol.signalmap.content.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.heatmaps.WeightedLatLng;
import com.javijuol.signalmap.content.Contract;
import com.javijuol.signalmap.content.bean.NetworkSignal;

import java.util.ArrayList;

/**
 * @author Javier Juan Oltra <javijuol@gmail.com>
 */
public class NetworkSignalDAO extends BaseDAO<NetworkSignal> {

    public static int MAX_STRENGTH = -50;
    public static int MIN_STRENGTH = -120;
    public static int[] GRADIENT_COLORS = {
            Color.rgb(255, 0, 0),   // red
            Color.rgb(255, 255, 0), // yellow
            Color.rgb(0, 225, 0)    // green
    };
    public static float[] GRADIENT_POINTS = {
            normalize(-105), normalize(-85), normalize(-50)
    };

    // Normalize strength value between 0 and 1
    private static float normalize(float strength) {
        return (strength - MIN_STRENGTH) / (MAX_STRENGTH - MIN_STRENGTH);
    }

    public static int SIGNAL_TYPE_2G = 2;
    public static int SIGNAL_TYPE_3G = 3;
    public static int SIGNAL_TYPE_4G = 4;

    private static String[] PROJECTION = {
            Contract.NetworkSignal._ID,
            Contract.NetworkSignal.FIELD_LATITUDE,
            Contract.NetworkSignal.FIELD_LONGITUDE,
            Contract.NetworkSignal.FIELD_STRENGTH,
            Contract.NetworkSignal.FIELD_TYPE,
            Contract.NetworkSignal.FIELD_CREATED_AT
    };

    public static final String DEFAULT_SORT_ORDER = Contract.NetworkSignal.FIELD_CREATED_AT + " DESC";

    public NetworkSignalDAO(@NonNull Context context) {
        this(context.getContentResolver());
    }

    public NetworkSignalDAO(@NonNull ContentResolver contentResolver) {
        super(contentResolver);
    }

    public @Nullable Cursor cursorFindByType(@NonNull Integer type){
        return contentResolver.query(
                getBaseUri(),
                PROJECTION,
                Contract.NetworkSignal.FIELD_TYPE+  " = ? ",
                new String[]{type.toString()},
                DEFAULT_SORT_ORDER
        );
    }

    @Override
    protected @NonNull Uri getBaseUri() {
        return Contract.NetworkSignal.URI;
    }

    @Override
    public NetworkSignal mapToBean(@NonNull Cursor cursor) {
        NetworkSignal networkSignal = new NetworkSignal();
        networkSignal.setId(cursor.getLong(cursor.getColumnIndex(Contract.NetworkSignal._ID)));
        networkSignal.setLat(cursor.getDouble(cursor.getColumnIndex(Contract.NetworkSignal.FIELD_LATITUDE)));
        networkSignal.setLng(cursor.getDouble(cursor.getColumnIndex(Contract.NetworkSignal.FIELD_LONGITUDE)));
        networkSignal.setStrength(cursor.getLong(cursor.getColumnIndex(Contract.NetworkSignal.FIELD_STRENGTH)));
        networkSignal.setType(cursor.getInt(cursor.getColumnIndex(Contract.NetworkSignal.FIELD_TYPE)));
        networkSignal.setCreatedAt(cursor.getLong(cursor.getColumnIndex(Contract.NetworkSignal.FIELD_CREATED_AT)));

        return networkSignal;
    }

    @Override
    protected @NonNull ContentValues mapToDB(@NonNull NetworkSignal networkSignal) {
        ContentValues cv = new ContentValues();
        cv.put(Contract.NetworkSignal.FIELD_LATITUDE, networkSignal.getLat());
        cv.put(Contract.NetworkSignal.FIELD_LONGITUDE, networkSignal.getLng());
        cv.put(Contract.NetworkSignal.FIELD_STRENGTH, networkSignal.getStrength());
        cv.put(Contract.NetworkSignal.FIELD_TYPE, networkSignal.getType());
        cv.put(Contract.NetworkSignal.FIELD_CREATED_AT, networkSignal.getCreatedAt());
        return cv;
    }

    public @NonNull ArrayList<WeightedLatLng> mapToHeatmap(@NonNull Cursor cursor) {
        ArrayList<WeightedLatLng> weightedLatLngList = new ArrayList<>();
        while (cursor.moveToNext()) {
            LatLng latLng = new LatLng(
                    cursor.getDouble(cursor.getColumnIndex(Contract.NetworkSignal.FIELD_LATITUDE)),
                    cursor.getDouble(cursor.getColumnIndex(Contract.NetworkSignal.FIELD_LONGITUDE))
            );
            int strength = cursor.getInt(cursor.getColumnIndex(Contract.NetworkSignal.FIELD_STRENGTH));
            if (strength >= MIN_STRENGTH && strength <= MAX_STRENGTH) {
                WeightedLatLng weightedLatLng = new WeightedLatLng(latLng, normalize(strength));
                weightedLatLngList.add(weightedLatLng);
            }
        }
        return weightedLatLngList;
    }
}
