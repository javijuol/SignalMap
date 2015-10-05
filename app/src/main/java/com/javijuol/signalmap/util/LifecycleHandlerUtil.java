package com.javijuol.signalmap.util;

import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.os.Bundle;
import android.util.Log;

import com.javijuol.signalmap.app.Application;

/**
 * @author Javier Juan Oltra <javijuol@gmail.com>
 */
public class LifecycleHandlerUtil implements ActivityLifecycleCallbacks {

    private static int resumed;
    private static int paused;
    private static int started;
    private static int stopped;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        ++resumed;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        ++paused;
        if(Application.DEVELOPER_MODE) Log.i(LifecycleHandlerUtil.class.getSimpleName(), "application is in foreground: " + (resumed > paused));
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
        ++started;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        ++stopped;
        if(Application.DEVELOPER_MODE) Log.i(LifecycleHandlerUtil.class.getSimpleName(), "application is visible: " + (started > stopped));
    }

    public static boolean isApplicationVisible() {
        return started > stopped;
    }

    public static boolean isApplicationInForeground() {
        if(Application.DEVELOPER_MODE) Log.i(LifecycleHandlerUtil.class.getSimpleName(), "application is in foreground: " + (resumed > paused));
        return resumed > paused;
    }


}
