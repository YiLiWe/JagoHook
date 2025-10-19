package com.xposed.jagohook.hook.activity;

import android.app.Activity;
import android.util.Log;
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
            getHandler().postDelayed(this::button, 1000);
            return;
        }
        List<Button> buttons = traverseViews(Button.class, window.getDecorView().findViewById(android.R.id.content));
        if (buttons.isEmpty()) {
            getHandler().postDelayed(this::button, 1000);
            return;
        }
        Map<String, Button> buttonMap = new HashMap<>();
        for (Button button : buttons) {
            buttonMap.put(button.getText().toString(), button);
        }
        String pass = "115599";
        for (int i = 0; i < pass.length(); i++) {
            String key = String.valueOf(pass.charAt(i));
            if (buttonMap.containsKey(key)) {
                buttonMap.get(key).performClick();
            }
        }
        Logs.d("执行完毕");
    }

    @Override
    public void onDestroyed(Activity activity) {
        super.onDestroyed(activity);
    }
}
