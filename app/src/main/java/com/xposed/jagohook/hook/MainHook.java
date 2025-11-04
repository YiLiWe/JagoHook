package com.xposed.jagohook.hook;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedHelpers.findAndHookMethod("android.app.Instrumentation", lpparam.classLoader, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[0] instanceof Application application) {
                    application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                        @Override
                        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
                            Log.d("代码", activity.getClass().getName());
                        }

                        @Override
                        public void onActivityDestroyed(@NonNull Activity activity) {

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
                    });
                }
            }
        });
        XposedBridge.hookAllMethods(System.class, "exit", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                param.setResult(null); // 阻止退出
            }
        });
    }

}
