package com.xposed.jagohook.utils;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.pm.ArchivedActivityInfo;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccessibleUtil {

    //获取开头
    public static AccessibilityNodeInfo toStateContentDescMap(Map<String, AccessibilityNodeInfo> nodeInfoMap, String text) {
        for (String key : nodeInfoMap.keySet()) {
            if (key.startsWith(text)) {
                return nodeInfoMap.get(key);
            }
        }
        return null;
    }

    public static Map<String, AccessibilityNodeInfo> toStateContentDescMapX(Map<String, AccessibilityNodeInfo> nodeInfoMap, String text) {
        Map<String, AccessibilityNodeInfo> map = new HashMap<>();
        for (String key : nodeInfoMap.keySet()) {
            if (key.startsWith(text)) {
                map.put(key, nodeInfoMap.get(key));
            }
        }
        return map;
    }


    //获取备注的文字
    public static Map<String, AccessibilityNodeInfo> toContentDescMap(AccessibilityNodeInfo accessibilityNodeInfo) {
        List<AccessibilityNodeInfo> accessibilityNodeInfos = new ArrayList<>();
        AccessibleUtil.getAccessibilityNodeInfoS(accessibilityNodeInfos, accessibilityNodeInfo);
        return toContentDescMap(accessibilityNodeInfos);
    }

    public static Map<String, AccessibilityNodeInfo> toContentDescMap(List<AccessibilityNodeInfo> accessibilityNodeInfos) {
        Map<String, AccessibilityNodeInfo> map = new HashMap<>();
        for (AccessibilityNodeInfo node : accessibilityNodeInfos) {
            if (node.getContentDescription() != null) {
                map.put(node.getContentDescription().toString(), node);
            }
        }
        return map;
    }


    /**
     * 使用无障碍服务进行文本输入（最稳定）
     */
    public static boolean inputTextByAccessibility(AccessibilityNodeInfo targetNode, String text) {
        // 设置新文本
        Bundle setTextArgs = new Bundle();
        setTextArgs.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        return targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, setTextArgs);
    }

    public static void getAccessibilityNodeInfoS(List<AccessibilityNodeInfo> nodeInfos, AccessibilityNodeInfo node) {
        if (node == null) {
            return;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo accessibilityNodeInfo = node.getChild(i);
            if (accessibilityNodeInfo != null) {
                nodeInfos.add(accessibilityNodeInfo);
                getAccessibilityNodeInfoS(nodeInfos, accessibilityNodeInfo);
            }
        }
    }

    public static AccessibilityNodeInfo findAccessibilityNodeInfosByViewId(AccessibilityNodeInfo nodeInfo, String text) {
        List<AccessibilityNodeInfo> switch_widget = nodeInfo.findAccessibilityNodeInfosByViewId(text);
        if (switch_widget.size() == 0) {
            return null;
        }
        for (AccessibilityNodeInfo nodeInfo1 : switch_widget) {
            if (nodeInfo1.getViewIdResourceName().equals(text)) {
                return nodeInfo1;
            }
        }
        return null;
    }

    public static AccessibilityNodeInfo findAccessibilityNodeInfosByViewIdEnd(AccessibilityNodeInfo nodeInfo, String text) {
        List<AccessibilityNodeInfo> switch_widget = nodeInfo.findAccessibilityNodeInfosByViewId(text);
        if (switch_widget.size() == 0) {
            return null;
        }
        for (AccessibilityNodeInfo nodeInfo1 : switch_widget) {
            if (nodeInfo1.getViewIdResourceName().endsWith(text)) {
                return nodeInfo1;
            }
        }
        return null;
    }

    public static AccessibilityNodeInfo findAccessibilityNodeInfosByText(AccessibilityNodeInfo nodeInfo, String text) {
        List<AccessibilityNodeInfo> switch_widget = nodeInfo.findAccessibilityNodeInfosByViewId(text);
        if (switch_widget.size() == 0) {
            return null;
        }
        for (AccessibilityNodeInfo nodeInfo1 : switch_widget) {
            if (nodeInfo1.getText().equals(text)) {
                return nodeInfo1;
            }
        }
        return null;
    }

    public static AccessibilityNodeInfo findAccessibilityNodeInfosByTextS(AccessibilityNodeInfo nodeInfo, String text) {
        List<AccessibilityNodeInfo> accessibilityNodeInfos = new ArrayList<>();
        AccessibleUtil.getAccessibilityNodeInfoS(accessibilityNodeInfos, nodeInfo);
        for (AccessibilityNodeInfo nodeInfo1 : accessibilityNodeInfos) {
            CharSequence sequence = nodeInfo1.getText();
            if (sequence != null && sequence.toString().contains(text)) {
                return nodeInfo1;
            }
        }
        return null;
    }

    public static AccessibilityNodeInfo getScrollableNode(AccessibilityNodeInfo node) {
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
        AccessibleUtil.getAccessibilityNodeInfoS(nodeInfos, node);
        for (AccessibilityNodeInfo childNode : nodeInfos) {
            if (childNode.isScrollable()) {
                return childNode;
            }
        }
        return null;
    }

    public static AccessibilityNodeInfo getClassName(AccessibilityNodeInfo nodeInfo, String className) {
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
        AccessibleUtil.getAccessibilityNodeInfoS(nodeInfos, nodeInfo);
        for (AccessibilityNodeInfo nodeInfo1 : nodeInfos) {
            if (nodeInfo1.getClassName().equals(className)) {
                return nodeInfo1;
            }
        }
        return null;
    }


    public static void Scrollable(AccessibilityNodeInfo nodeInfo) {
        AccessibilityNodeInfo nodeInfo1 = AccessibleUtil.getScrollableNode(nodeInfo);
        if (nodeInfo1 != null) {
            nodeInfo1.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        }
    }

    public static void ClickText(AccessibilityService accessibilityService, AccessibilityNodeInfo targetNode, String text) {
        AccessibilityNodeInfo allow = AccessibleUtil.findAccessibilityNodeInfosByText(targetNode, text);//同意授权
        if (allow != null) {
            AccessibleUtil.Click(accessibilityService, allow);
        }
    }

    public static void ClickId(AccessibilityService accessibilityService, AccessibilityNodeInfo targetNode, String id) {
        AccessibilityNodeInfo allow = AccessibleUtil.findAccessibilityNodeInfosByViewId(targetNode, id);//同意授权
        if (allow != null) {
            AccessibleUtil.Click(accessibilityService, allow);
        }
    }

    public static AccessibilityNodeInfo getSwitchWidget(AccessibilityNodeInfo nodeInfo) {
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
        getAccessibilityNodeInfoS(nodeInfos, nodeInfo);
        for (AccessibilityNodeInfo nodeInfo1 : nodeInfos) {
            CharSequence ResourceName = nodeInfo1.getViewIdResourceName();
            if (ResourceName != null && ResourceName.toString().equals("android:id/switch_widget")) {
                return nodeInfo1;
            }
        }
        return null;
    }

    public static List<AccessibilityNodeInfo> getWidget(AccessibilityNodeInfo nodeInfo, String className) {
        List<AccessibilityNodeInfo> widget = new ArrayList<>();
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
        getAccessibilityNodeInfoS(nodeInfos, nodeInfo);
        for (AccessibilityNodeInfo nodeInfo1 : nodeInfos) {
            CharSequence charSequence = nodeInfo1.getClassName();
            if (charSequence != null && charSequence.equals(className)) {
                widget.add(nodeInfo1);
            }
        }
        return widget;
    }

    public static void Click(AccessibilityService accessibilityService, AccessibilityNodeInfo targetNode) {
        if (targetNode == null) {
            return;
        }
        if (!targetNode.isVisibleToUser()) {
            return;
        }
        if (click(targetNode)) {
            return;
        }
        Rect bounds = new Rect();
        targetNode.getBoundsInScreen(bounds);
        int x = bounds.centerX();
        int y = bounds.centerY();

        GestureDescription.Builder builder = new GestureDescription.Builder();
        Path path = new Path();
        path.moveTo(x, y);
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
        accessibilityService.dispatchGesture(builder.build(), new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
            }
        }, null);
    }


    public static void simulateClick(AccessibilityService service, float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        GestureDescription gestureDescription = builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100)).build();
        service.dispatchGesture(gestureDescription, null, null);
    }


    private static boolean click(AccessibilityNodeInfo it) {
        return it.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

}
