package com.xposed.jagohook.hook.activity;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import com.xposed.jagohook.utils.Logs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivityHook extends BaseHook {
    private static final String TAG = "MainActivityHook";

    //Masukkan PIN kamu
    @Override
    public void onHook(Activity activity) {
        super.onHook(activity);
        Logs.d("开始执行");
        getHandler().postDelayed(this::button, 1000);
    }

    //定位按钮
    private void button() {
        if (getActivity() == null) {
            getHandler().postDelayed(this::button, 1000);
            return;
        }
        Window window = getActivity().getWindow();
        if (window == null) {
            Logs.d("获取不到window");
            getHandler().postDelayed(this::button, 1000);
            return;
        }
        View view = window.getDecorView();
        if (view instanceof ViewGroup viewGroup) {
            List<View> buttons = traverseViews(viewGroup);
            if (buttons.isEmpty()) {
                Logs.d("获取不到按钮a");
                getHandler().postDelayed(this::button, 1000);
                return;
            }
            Map<String, Button> buttonMap = new HashMap<>();
            for (View button : buttons) {
                if (button instanceof Button button1) {
                    buttonMap.put(button1.getText().toString(), button1);
                }
            }
            String pass = "115599";
            for (int i = 0; i < pass.length(); i++) {
                String key = String.valueOf(pass.charAt(i));
                if (buttonMap.containsKey(key)) {
                    Logs.d("找到按钮" + key);
                    buttonMap.get(key).performClick();
                }
            }
            Logs.d("执行完毕");
        } else {
            getHandler().postDelayed(this::button, 1000);
        }
    }

    @Override
    public void onDestroyed(Activity activity) {
        super.onDestroyed(activity);
    }
}
