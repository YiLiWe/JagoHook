package com.xposed.jagohook.hook.activity;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public abstract class BaseHook {
    public final Handler handler = new Handler(Looper.getMainLooper());
    private Activity activity;

    public void onHook(Activity activity) {
        this.activity = activity;
    }

    public void onDestroyed(Activity activity) {
        handler.removeCallbacksAndMessages(null);
    }

    public <T extends View> List<T> traverseViews(Class<T> clazz, ViewGroup viewGroup) {
        List<View> list = new ArrayList<>();
        traverseViews(list, viewGroup);
        List<T> result = new ArrayList<>();
        for (View view : list) {
            if (clazz.isInstance(view)) {
                result.add((T) view);
            }
        }
        return result;
    }

    public void traverseViews(List<View> list, ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View childView = viewGroup.getChildAt(i);
            if (childView instanceof ViewGroup) {
                traverseViews(list, (ViewGroup) childView); // 递归调用
            } else {
                list.add(childView);
            }
        }
    }
}
