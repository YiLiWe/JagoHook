package com.xposed.jagohook.hook;

import android.app.Application;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedHelpers.findAndHookMethod(
                "com.android.server.wm.WindowManagerService",
                lpparam.classLoader,
                "getWindowContentFrameStats",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            // 反射获取 ViewRootImpl 实例
                            Object windowState = XposedHelpers.getObjectField(param.thisObject, "mRoot");
                            Class<?> viewRootImplClass = Class.forName("android.view.ViewRootImpl");
                            Object root = XposedHelpers.getObjectField(windowState, "mAttachInfo");
                            View decorView = (View) XposedHelpers.getObjectField(root, "mView");

                            // 打印 View 树
                            dumpViewTree(decorView, 0);
                        } catch (Throwable e) {
                            XposedBridge.log("反射失败: " + e);
                        }
                    }
                }
        );

       // hookInstrumentation(lpparam);
    }

    void dumpViewTree(View view, int depth) {
        Log.d("ViewTree", String.format("%s%s@%x",
                " ".repeat(depth),
                view.getClass().getSimpleName(),
                System.identityHashCode(view)));
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup)view).getChildCount(); i++) {
                dumpViewTree(((ViewGroup)view).getChildAt(i), depth + 1);
            }
        }
    }

    private void hookInstrumentation(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod("android.app.Instrumentation", lpparam.classLoader, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                if (param.args[0] instanceof Application application) {
                    application.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks());
                }
            }
        });
    }
}
