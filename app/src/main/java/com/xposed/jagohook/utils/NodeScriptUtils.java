package com.xposed.jagohook.utils;


import com.xposed.jagohook.server.SuShellService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeScriptUtils {
    public static Map<String, SuShellService.UiXmlParser.Node> toContentDescMap(List<SuShellService.UiXmlParser.Node> nodes) {
        Map<String, SuShellService.UiXmlParser.Node> nodeMap = new HashMap<>();
        for (SuShellService.UiXmlParser.Node node : nodes) {
            nodeMap.put(node.getContentDesc(), node);
        }
        return nodeMap;
    }

    public static Map<String, SuShellService.UiXmlParser.Node> toNAFMap(List<SuShellService.UiXmlParser.Node> nodes) {
        Map<String, SuShellService.UiXmlParser.Node> nodeMap = new HashMap<>();
        for (SuShellService.UiXmlParser.Node node : nodes) {
            if (node.getNaf() != null) {
                nodeMap.put(node.getContentDesc(), node);
            }
        }
        return nodeMap;
    }

    public static Map<String, SuShellService.UiXmlParser.Node> toResourceIdMap(List<SuShellService.UiXmlParser.Node> nodes) {
        Map<String, SuShellService.UiXmlParser.Node> nodeMap = new HashMap<>();
        for (SuShellService.UiXmlParser.Node node : nodes) {
            nodeMap.put(node.getResourceId(), node);
        }
        return nodeMap;
    }

    public static Map<String, SuShellService.UiXmlParser.Node> toTextMap(List<SuShellService.UiXmlParser.Node> nodes) {
        Map<String, SuShellService.UiXmlParser.Node> nodeMap = new HashMap<>();
        for (SuShellService.UiXmlParser.Node node : nodes) {
            nodeMap.put(node.getText(), node);
        }
        return nodeMap;
    }
}
