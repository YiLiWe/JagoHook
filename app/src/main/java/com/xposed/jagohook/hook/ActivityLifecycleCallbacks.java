package com.xposed.jagohook.hook;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.xposed.jagohook.hook.activity.BaseHook;
import com.xposed.jagohook.hook.activity.MainActivityHook;

import java.util.HashMap;
import java.util.Map;

public class ActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    private final Map<String, BaseHook> hooks = new HashMap<>() {{
        put("com.jago.digitalBanking.MainActivity", new MainActivityHook());
    }};

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
        String className = activity.getClass().getName();
        if (hooks.containsKey(className)) {
            hooks.get(className).onHook(activity);
        }
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        String className = activity.getClass().getName();
        if (hooks.containsKey(className)) {
            hooks.get(className).onDestroyed(activity);
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }
}
