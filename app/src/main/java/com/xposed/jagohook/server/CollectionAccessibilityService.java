package com.xposed.jagohook.server;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.xposed.jagohook.utils.AccessibleUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        AccessibilityNodeInfo nodeInfo = accessibilityEvent.getSource();
        List<AccessibilityNodeInfo> accessibilityNodeInfos = new ArrayList<>();
        AccessibleUtil.getAccessibilityNodeInfoS(accessibilityNodeInfos, nodeInfo);
        Map<String, AccessibilityNodeInfo> nodeInfoMap = toContentDescMap(accessibilityNodeInfos);
        if (nodeInfoMap.containsKey("Search Text Field")) {
            AccessibilityNodeInfo accessibilityNodeInfo = nodeInfoMap.get("Search Text Field");
            if (accessibilityNodeInfo == null) return;
            AccessibilityNodeInfo accessibilityNodeInfo1 = accessibilityNodeInfo.getChild(0);
            accessibilityNodeInfo1.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            AccessibleUtil.inputTextByAccessibility(accessibilityNodeInfo1, "001901139428502");
        }
    }

    private Map<String, AccessibilityNodeInfo> toContentDescMap(List<AccessibilityNodeInfo> nodes) {
        Map<String, AccessibilityNodeInfo> map = new HashMap<>();
        for (AccessibilityNodeInfo node : nodes) {
            if (node.getContentDescription() != null) {
                map.put(node.getContentDescription().toString(), node);
            }
        }
        return map;
    }

    @Override
    public void onInterrupt() {

    }
}
