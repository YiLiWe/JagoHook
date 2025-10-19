package com.xposed.jagohook.server.script;

import com.xposed.jagohook.server.SuShellService;

import java.util.List;

public abstract class BaseScript {
    public abstract void onCreate(SuShellService suShellService, List<SuShellService.UiXmlParser.Node> nodes);

}
