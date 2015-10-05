package com.javijuol.signalmap.app;

import android.content.SharedPreferences;
import android.os.StrictMode;
import android.preference.PreferenceManager;

import com.javijuol.signalmap.BuildConfig;
import com.javijuol.signalmap.util.LifecycleHandlerUtil;
import com.javijuol.signalmap.util.Preferences;

/**
 * @author Javier Juan Oltra <javijuol@gmail.com>
 */
public class Application extends android.app.Application {

    public static final String DEBUG_TAG = "debug.signalmap";
    public static final boolean DEVELOPER_MODE = BuildConfig.DEBUG;
    public static boolean FIRST_TIME = false;

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new LifecycleHandlerUtil());

        if (FIRST_TIME)
            onFirstTime();

        if (DEVELOPER_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .penaltyDialog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    //.detectActivityLeaks()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectLeakedRegistrationObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
    }

    private void onFirstTime() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean(Preferences.PREFERENCE_COLLECTING_DATA, true);
        editor.putInt(Preferences.PREFERENCE_COLLECTING_DATA, Preferences.FilterDataType.FILTER_DATA_ALL.getCode());
        editor.apply();
    }

}
