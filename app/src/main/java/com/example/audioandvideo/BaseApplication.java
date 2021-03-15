package com.example.audioandvideo;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.hjq.permissions.XXPermissions;
import com.squareup.leakcanary.LeakCanary;

public class BaseApplication extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    @Override
    public void onCreate() {
        super.onCreate();
        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);
        sContext = getApplicationContext();
        XXPermissions.setScopedStorage(true);
    }
    public static Context getContext() {
        return sContext;
    }
}
