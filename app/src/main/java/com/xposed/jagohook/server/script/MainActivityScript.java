package com.xposed.jagohook.server.script;

import android.graphics.Rect;

import com.xposed.jagohook.server.SuShellService;
import com.xposed.jagohook.utils.Logs;
import com.xposed.jagohook.utils.NodeScriptUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivityScript extends BaseScript {
    @Override
    public void onCreate(SuShellService suShellService, List<SuShellService.UiXmlParser.Node> nodes) {
        Map<String, SuShellService.UiXmlParser.Node> map = NodeScriptUtils.toContentDescMap(nodes);
        inputPassword(suShellService, map);
    }

    //输入密码
    private void inputPassword(SuShellService suShellService, Map<String, SuShellService.UiXmlParser.Node> map) {
        if (map.containsKey("Masukkan PIN kamu")) {
            String pass = "115599";
            List<Rect> rects = new ArrayList<>();
            for (int i = 0; i < pass.length(); i++) {
                String key = String.valueOf(pass.charAt(i));
                if (map.containsKey(key)) {
                    Logs.d("找到按钮" + key);
                    SuShellService.UiXmlParser.Node node = map.get(key);
                    rects.add(node.getBounds());
                }
            }
            suShellService.click(rects);
            Logs.d("执行完毕");
        }
    }
}
