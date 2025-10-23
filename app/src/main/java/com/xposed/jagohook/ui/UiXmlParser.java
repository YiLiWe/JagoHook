package com.xposed.jagohook.ui;

import android.graphics.Rect;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * UI XML解析器
 * 负责解析UI自动化dump生成的XML文件
 */
public class UiXmlParser {
    
    private static final String TAG = "UiXmlParser";
    
    private final String filePath;
    private final List<Node> nodes = new ArrayList<>();
    
    public UiXmlParser(String filePath) {
        this.filePath = filePath;
    }
    
    /**
     * 解析UI XML文件
     */
    public void parseUiXml() {
        File file = new File(filePath);
        if (!file.exists()) {
            Log.e(TAG, "File not found: " + filePath);
            return;
        }

        try (InputStream inputStream = new FileInputStream(file)) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);

            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        handleStartTag(parser);
                        break;
                    case XmlPullParser.END_TAG:
                        handleEndTag(parser);
                        break;
                }
                eventType = parser.next();
            }
        } catch (IOException | XmlPullParserException e) {
            Log.e(TAG, "Error parsing UI XML: " + e.getMessage());
        }
    }
    
    /**
     * 获取解析后的节点列表
     */
    public List<Node> getNodes() {
        return new ArrayList<>(nodes);
    }
    
    private void handleStartTag(XmlPullParser parser) {
        String tagName = parser.getName();
        if ("node".equals(tagName)) {
            // 提取节点属性
            String className = parser.getAttributeValue(null, "class");
            String text = parser.getAttributeValue(null, "text");
            String bounds = parser.getAttributeValue(null, "bounds");
            String packageName = parser.getAttributeValue(null, "package");
            String resourceId = parser.getAttributeValue(null, "resource-id");
            String contentDesc = parser.getAttributeValue(null, "content-desc");
            String naf = parser.getAttributeValue(null, "NAF");
            
            Node node = new Node();
            node.setNaf(naf);
            node.setClassName(className);
            node.setText(text);
            node.setBounds(bounds);
            node.setPackageName(packageName);
            node.setResourceId(resourceId);
            node.setContentDesc(contentDesc);
            nodes.add(node);
        }
    }
    
    private void handleEndTag(XmlPullParser parser) {
        // 可选的结束标签处理逻辑
    }
    
    @Setter
    @Getter
    @ToString
    public static class Node {
        private String className;
        private String text;
        private String bounds;
        private String packageName;
        private String resourceId;
        private String index;
        private String contentDesc;
        private String naf;

        public int getBoundsX() {
            Rect rect = getBounds();
            if (rect == null) return -1;
            return rect.centerX();
        }

        public int getBoundsY() {
            Rect rect = getBounds();
            if (rect == null) return -1;
            return rect.centerY();
        }

        public Rect getBounds() {
            if (bounds == null || bounds.isEmpty()) {
                return null;
            }
            try {
                // 移除所有非数字字符（保留逗号和方括号用于分割）
                String cleanedStr = bounds.replaceAll("[^\\d,\\[\\]]", "");
                String[] parts = cleanedStr.split("\\[|\\]|,");
                // 提取有效数字部分
                int x1 = Integer.parseInt(parts[1]);
                int y1 = Integer.parseInt(parts[2]);
                int x2 = Integer.parseInt(parts[4]);
                int y2 = Integer.parseInt(parts[5]);
                return new Rect(x1, y1, x2, y2);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}