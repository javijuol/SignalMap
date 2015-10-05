package com.javijuol.signalmap.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.javijuol.signalmap.app.Application;
import com.javijuol.signalmap.content.bean.NetworkSignal;
import com.javijuol.signalmap.content.dao.NetworkSignalDAO;

import java.util.Calendar;
import java.util.Random;

/**
 * This service collects data from mobile network and GPS and saves it in a persistent storage.
 *
 * @author Javier Juan Oltra <javijuol@gmail.com>
 */
public class NetworkSignalService extends Service {

    private static boolean mServiceRunning = false;
    private LocationManager mLocationManager;
    private NetworkListener mNetworkListener;
    private TelephonyManager mTelephonyManager;

    /**
     * This class is intended to keep the service always awake, so if it gets off by the system
     * the watchdog can wake it up again.
     * It checks every {@link NetworkSignalService.WatchdogBroadcastReceiver#MINUTES_INTERVAL} minutes
     * if the service is still running, and start it if needed.
     */
    public static class WatchdogBroadcastReceiver extends BroadcastReceiver {

        public static final int MINUTES_INTERVAL = 1;
        private static final int ALARM_ID = 0;
        private static AlarmManager alarmManager;

        public static boolean scheduleNextNotification(@NonNull Context context) {
            Intent intent = new Intent(context, NetworkSignalService.WatchdogBroadcastReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, intent, 0);

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(SystemClock.elapsedRealtime());
            calendar.add(Calendar.MINUTE, MINUTES_INTERVAL);
            alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, calendar.getTimeInMillis(), pendingIntent);

            return true;
        }

        public static void stopNextNotification(@NonNull Context context) {
            Intent intent = new Intent(context, NetworkSignalService.WatchdogBroadcastReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            alarmManager.cancel(pendingIntent);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mServiceRunning)
                context.startService(new Intent(context, NetworkSignalService.class));
            scheduleNextNotification(context);
        }
    }

    /**
     * This class basically listens to changes on network signal strength and gps locations.
     * When both, a new network signal and a new location, are met the process notifies a listener
     * for new data changes.
     */
    private static class NetworkListener extends PhoneStateListener implements LocationListener {

        private Context mContext;

        protected Location mLocation = null;
        protected Integer mStrength = null;
        protected int mType = 0;

        private static final double LOCATION_ACCURACY_THRESHOLD = 500;
        private static final double LOCATION_DISTANCE_THRESHOLD = 0; // meters

        protected OnNewDataListener mOnNewDataListener = null;

        public NetworkListener(Context context, OnNewDataListener callback) {
            mContext = context;
            this.setOnNewDataListener(callback);
        }

        public interface OnNewDataListener {
            void onNewData(Location location, int strength, int type);
        }

        public void setOnNewDataListener(OnNewDataListener l) {
            mOnNewDataListener = l;
        }

        @Override
        public void onLocationChanged(Location location) {
            if (location == null || (location.getAccuracy() > LOCATION_ACCURACY_THRESHOLD) ) { //|| location.distanceTo(mLocation) < LOCATION_DISTANCE_THRESHOLD) {
                return;
            }

            mLocation = location;
            if (mStrength != null) {
                if (mOnNewDataListener != null) {
                    mOnNewDataListener.onNewData(mLocation, mStrength, mType);
                }
                mLocation = null;
                mStrength = null;
            }

        }

        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);

            if (signalStrength.isGsm()) {
                mStrength = signalStrength.getGsmSignalStrength();
            } else {
                mStrength = signalStrength.getCdmaDbm();
            }
            mStrength = mStrength * 2 - 113;

            // This is only for emulator, so it can simulate different values for the signal strength
            if (Build.FINGERPRINT.contains("generic")) {
                Random r = new Random();
                mStrength = (r.nextInt(120 - 50) + 50) * -1;
            }

            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            mType = telephonyManager.getNetworkType();

            if (mLocation != null) {
                if (mOnNewDataListener != null) {
                    mOnNewDataListener.onNewData(mLocation, mStrength, mType);
                }
                mLocation = null;
                mStrength = null;
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }

    }

    @Override
    public void onCreate() {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mNetworkListener = new NetworkListener(getBaseContext(), new NetworkListener.OnNewDataListener() {
            @Override
            public void onNewData(Location location, int strength, int type) {
                saveData(location, strength, type);
            }
        });
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mServiceRunning = true;

        if (intent != null) {
            Criteria criteria = new Criteria();
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(false);
            criteria.setSpeedRequired(false);
            criteria.setPowerRequirement(Criteria.POWER_HIGH);
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            String providerFine = mLocationManager.getBestProvider(criteria, true);
            //  requestLocationUpdates(provider , minTime, minDistance, listener);
            if (providerFine != null) {
                mLocationManager.requestLocationUpdates(providerFine, 2000, (float) NetworkListener.LOCATION_DISTANCE_THRESHOLD, mNetworkListener);
            }

            try {
                mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                mTelephonyManager.listen(mNetworkListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        mServiceRunning = false;
        mLocationManager.removeUpdates(mNetworkListener);
        mTelephonyManager.listen(mNetworkListener, PhoneStateListener.LISTEN_NONE);

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }


    /**
     * Saves new data to the persistent storage.
     *
     * @param location a new location from it would extract latitude and longitude coordinates.
     * @param strength network signal strength measured in dBm.
     * @param networkType TYPE_2G = 2 | TYPE_3G = 3 | TYPE_4G = 4
     */
    private void saveData(@NonNull Location location, @NonNull int strength, @NonNull int networkType){
        int type;
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_LTE:
                type = NetworkSignalDAO.SIGNAL_TYPE_4G;
                break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                type = NetworkSignalDAO.SIGNAL_TYPE_3G;
                break;
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                type = NetworkSignalDAO.SIGNAL_TYPE_2G;
                break;
            default:
                type = 0;
                break;
        }

        if (Application.DEVELOPER_MODE) Log.d(Application.DEBUG_TAG, String.format("%1$ddBm (%2$dG) @ %3$.5f|%4$.5f", strength, type, location.getLatitude(), location.getLongitude()));

        NetworkSignalDAO networkSignalDAO = new NetworkSignalDAO(getBaseContext());
        NetworkSignal networkSignal = new NetworkSignal();
        networkSignal.setLat(location.getLatitude());
        networkSignal.setLng(location.getLongitude());
        networkSignal.setStrength(strength);
        networkSignal.setType(type);
        networkSignal.setCreatedAt(System.currentTimeMillis());
        networkSignalDAO.createNew(networkSignal);
    }

}