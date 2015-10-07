package com.javijuol.signalmap.app;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.WeightedLatLng;
import com.javijuol.signalmap.R;
import com.javijuol.signalmap.composer.Gradient;
import com.javijuol.signalmap.composer.WeightedHeatmapTileProvider;
import com.javijuol.signalmap.content.dao.NetworkSignalDAO;
import com.javijuol.signalmap.service.NetworkSignalService;
import com.javijuol.signalmap.util.Preferences;

import java.util.ArrayList;


/**
 * Entry point of the app where all the UI processes happens.
 * First check if GooglePlayServices are available, and then load the heat map and start the
 * network service.
 *
 * @author Javier Juan Oltra <javijuol@gmail.com>
 */
public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, OnMapReadyCallback {

    private GoogleMap mGoogleMap;
    private WeightedHeatmapTileProvider mProvider;
    private TileOverlay mOverlay;
    private RadioGroup mFilterSignalType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.appbar));

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());
        if(status != ConnectionResult.SUCCESS){
            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();
        }else {
            mFilterSignalType = (RadioGroup) findViewById(R.id.radio_group_list_selector);
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
            startService();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem toggle_service = menu.findItem(R.id.menu_toggle_service);

        boolean collectingData = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Preferences.PREFERENCE_COLLECTING_DATA, true);
        toggle_service.setTitle(collectingData ? R.string.menu_disable_service : R.string.menu_enable_service);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_toggle_service:
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                boolean collectingData = sharedPreferences.getBoolean(Preferences.PREFERENCE_COLLECTING_DATA, true);
                if(collectingData)
                    stopService();
                else
                    startService();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(Preferences.PREFERENCE_COLLECTING_DATA, !collectingData);
                editor.apply();
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, null, null, null, null, null)
        {
            final ForceLoadContentObserver mObserver = new ForceLoadContentObserver();

            @Override
            public Cursor loadInBackground() {
                Cursor cursor;
                int filter_type = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(Preferences.PREFERENCE_FILTER_DATA, Preferences.FilterDataType.FILTER_DATA_ALL.getCode());
                if (filter_type == Preferences.FilterDataType.FILTER_DATA_ALL.getCode()) {
                    cursor = new NetworkSignalDAO(getBaseContext()).cursorAll();
                }else{
                    cursor = new NetworkSignalDAO(getBaseContext()).cursorFindByType(filter_type);
                }

                if (cursor != null)
                    cursor.registerContentObserver(mObserver);

                return cursor;
            }

        };
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        refreshHeatMap(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        refreshHeatMap(null);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        getSupportLoaderManager().initLoader(0, null, this);
        mGoogleMap = googleMap;
        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location arg0) {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(arg0.getLatitude(), arg0.getLongitude()), 14));
                mGoogleMap.setOnMyLocationChangeListener(null);
            }
        });

        mFilterSignalType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
                switch (checkedId) {
                    case R.id.filter_map_2G:
                        editor.putInt(Preferences.PREFERENCE_FILTER_DATA, Preferences.FilterDataType.FILTER_DATA_2G.getCode());
                        break;
                    case R.id.filter_map_3G:
                        editor.putInt(Preferences.PREFERENCE_FILTER_DATA, Preferences.FilterDataType.FILTER_DATA_3G.getCode());
                        break;
                    case R.id.filter_map_4G:
                        editor.putInt(Preferences.PREFERENCE_FILTER_DATA, Preferences.FilterDataType.FILTER_DATA_4G.getCode());
                        break;
                    case R.id.filter_map_all:
                    default:
                        editor.putInt(Preferences.PREFERENCE_FILTER_DATA, Preferences.FilterDataType.FILTER_DATA_ALL.getCode());
                        break;
                }
                editor.apply();
                refreshData();
            }
        });

        int filter_type = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getInt(Preferences.PREFERENCE_FILTER_DATA, Preferences.FilterDataType.FILTER_DATA_ALL.getCode());
        int filter_option_selected = R.id.filter_map_all;
        if (filter_type == Preferences.FilterDataType.FILTER_DATA_2G.getCode())
            filter_option_selected = R.id.filter_map_2G;
        if (filter_type == Preferences.FilterDataType.FILTER_DATA_3G.getCode())
            filter_option_selected = R.id.filter_map_3G;
        if (filter_type == Preferences.FilterDataType.FILTER_DATA_4G.getCode())
            filter_option_selected = R.id.filter_map_4G;
        mFilterSignalType.check(filter_option_selected);
    }

    private void refreshData() {
        mProvider = null;
        mGoogleMap.clear();
        getSupportLoaderManager().restartLoader(0, null, this);
    }

    private void refreshHeatMap(@Nullable Cursor data) {
        ArrayList<WeightedLatLng> list = null;
        if (data != null) {
            NetworkSignalDAO dao = new NetworkSignalDAO(getBaseContext());
            list = dao.mapToHeatmap(data);
        }
        if (list != null && !list.isEmpty()) {
            if (mProvider == null) {
                mProvider = new WeightedHeatmapTileProvider.Builder()
                        .gradient(new Gradient(NetworkSignalDAO.GRADIENT_COLORS, NetworkSignalDAO.GRADIENT_POINTS))
                        .weightedData(list)
                        .build();
                mOverlay = mGoogleMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
            } else {
                mOverlay.clearTileCache();
                mProvider.setWeightedData(list);
            }
        }
    }

    private void startService() {
        Toast.makeText(this, R.string.toast_enable_service, Toast.LENGTH_LONG).show();
        NetworkSignalService.WatchdogBroadcastReceiver.startServiceNow(this);
    }

    private void stopService() {
        Toast.makeText(this, R.string.toast_disable_service, Toast.LENGTH_LONG).show();
        NetworkSignalService.WatchdogBroadcastReceiver.stopServiceNow(this);
    }
}
