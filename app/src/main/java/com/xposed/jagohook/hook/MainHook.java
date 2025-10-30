package com.xposed.jagohook.hook;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import java.util.Collections;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedBridge.hookAllMethods(lpparam.classLoader.loadClass("com.android.server.am.ActivityManagerService"), "updateActivityUsageStats", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Log.e("代码", "切换界面");
            }
        });
        XposedHelpers.findAndHookMethod(
                "android.view.accessibility.AccessibilityManager",
                lpparam.classLoader,
                "isEnabled",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        // 强制返回 false，隐藏无障碍服务状态
                        param.setResult(false);
                    }
                }
        );

        // 拦截获取已启用的无障碍服务列表
        XposedHelpers.findAndHookMethod(
                "android.view.accessibility.AccessibilityManager",
                lpparam.classLoader,
                "getEnabledAccessibilityServiceList",
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        // 返回空列表
                        param.setResult(Collections.emptyList());
                    }
                }
        );

    }
}
