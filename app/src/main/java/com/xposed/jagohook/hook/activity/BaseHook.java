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

    public List<View> traverseViews(ViewGroup viewGroup) {
        List<View> list = new ArrayList<>();
        traverseViews(list, viewGroup);
        return list;
    }


    //获取所有的子view，我们这里只保存imageview及textview，用于测试
    private void getAllViews(List<View> viewList, ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View view = viewGroup.getChildAt(i);
            if (view instanceof ViewGroup viewGroup1) {
                getAllViews(viewList, viewGroup1);
            } else {
                viewList.add(view);
            }
        }
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
