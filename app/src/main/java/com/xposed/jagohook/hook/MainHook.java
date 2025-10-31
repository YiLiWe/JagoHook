package com.xposed.jagohook.hook;

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
        XposedBridge.hookAllMethods(lpparam.classLoader.loadClass("com.android.server.am.ActivityManagerService"), "updateActivityUsageStats", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                Log.e("代码", "切换界面");
            }
        });


        XposedBridge.hookAllMethods(lpparam.classLoader.loadClass("com.android.server.accessibility.AccessibilityManagerService"), "sendAccessibilityEvent", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                Log.e("代码", "执行");
                // 打印调用类信息
                Class<?> callerClass = param.method.getDeclaringClass();
                Log.e("代码","调用类: " + callerClass.getName());

                    // 打印方法参数
                for (int i = 0; i < param.args.length; i++) {
                    Object arg = param.args[i];
                    Log.e("代码","参数 " + i + ": " +
                            (arg != null ? arg.getClass().getName() : "null") +
                            " = " + arg);
                }


                // 打印窗口属性（LayoutParams）
                if (param.args.length > 1 && param.args[1] != null) {
                    Object attrs = param.args[1];
                    String windowTitle = (String) XposedHelpers.getObjectField(attrs, "mTitle");
                    int width = XposedHelpers.getIntField(attrs, "width");
                    int height = XposedHelpers.getIntField(attrs, "height");
                    Log.e("代码","窗口标题: " + windowTitle + ", 尺寸: " + width + "x" + height);
                }

            }
        });


    }

    // 递归打印 View 层级
    private void dumpViewHierarchy(View view) {
        XposedBridge.log("View: " + view.getClass().getName());
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                dumpViewHierarchy(group.getChildAt(i));
            }
        }
    }
}
