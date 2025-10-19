package com.xposed.jagohook.server.script;

import com.xposed.jagohook.server.SuShellService;
import com.xposed.jagohook.utils.Logs;

import java.util.List;

public class MainActivityScript extends BaseScript {
    @Override
    public void onCreate(SuShellService suShellService, List<SuShellService.UiXmlParser.Node> nodes) {
        for (SuShellService.UiXmlParser.Node node : nodes) {
            Logs.d("节点名称：" + node.toString());
        }
    }
}
