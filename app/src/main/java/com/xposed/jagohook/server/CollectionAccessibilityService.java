package com.xposed.jagohook.server;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.xposed.jagohook.utils.AccessibleUtil;
import com.xposed.jagohook.utils.Logs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionAccessibilityService extends AccessibilityService {
    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        AccessibilityNodeInfo nodeInfo = accessibilityEvent.getSource();
        if (nodeInfo == null) return;
        Map<String, AccessibilityNodeInfo> nodeInfoMap = AccessibleUtil.toContentDescMap(nodeInfo);
        ScreenLockPassword(nodeInfoMap);
    }

    //屏幕输入密码
    private void ScreenLockPassword(Map<String, AccessibilityNodeInfo> nodeInfoMap) {
        if (!nodeInfoMap.containsKey("GUNAKAN PASSWORD")) return;
        String pass = "159951";
        for (int i = 0; i < pass.length(); i++) {
            String key = String.valueOf(pass.charAt(i));
            if (nodeInfoMap.containsKey(key)) {
                AccessibilityNodeInfo accessibilityNodeInfo = nodeInfoMap.get(key);
                if (accessibilityNodeInfo == null) return;
                accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    private void initCard(List<AccessibilityNodeInfo> accessibilityNodeInfos) {
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
                Logs.d(node.getContentDescription().toString());
                map.put(node.getContentDescription().toString(), node);
            }
        }
        return map;
    }

    @Override
    public void onInterrupt() {

    }
}
