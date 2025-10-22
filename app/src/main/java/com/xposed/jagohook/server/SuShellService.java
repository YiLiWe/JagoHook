package com.xposed.jagohook.server;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;


import com.xposed.jagohook.databinding.LayoutLogBinding;
import com.xposed.jagohook.runnable.CollectRunnable;
import com.xposed.jagohook.runnable.response.CollectBillResponse;
import com.xposed.jagohook.server.script.BaseScript;
import com.xposed.jagohook.server.script.MainActivityScript;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
public class SuShellService extends Service {
    private static final String TAG = "SuShellService";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Process suProcess;
    private DataOutputStream outputStream;
    private BufferedReader stdoutReader;
    private BufferedReader stderrReader;
    private LogWindow logWindow;
    private boolean isRunning = true;
    private String file;
    private String balance = "0";
    private CollectBillResponse collectBillResponse;
    private CollectRunnable collectRunnable;

    private final Map<String, BaseScript> activityScripts = new HashMap<>() {{
        put("com.jago.digitalBanking.MainActivity", new MainActivityScript());
    }};

    @Override
    public void onCreate() {
        super.onCreate();
        file = getCacheDir().getAbsolutePath() + "/ui.xml";
        logWindow = new LogWindow(this);
        collectRunnable = new CollectRunnable(this);
        new Thread(collectRunnable).start();
    }

    public void setCollectBillResponse(CollectBillResponse collectBillResponse) {
        this.collectBillResponse = collectBillResponse;
        if (collectBillResponse != null) {
            logWindow.print("触发归集");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        logWindow.destroy();
        stopSuShell();
    }


    // Binder 用于与 Activity 通信
    public class ScreenRecordBinder extends Binder {
        public SuShellService getService() {
            return SuShellService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new ScreenRecordBinder();
    }

    //开启服务
    public void start() {
        startSuShell();
    }

    //关闭服务
    public void stop() {
        stopSuShell();
    }

    /**
     * 启动 su shell 并捕获输出
     */
    private void startSuShell() {
        logWindow.print("开始执行脚本");
        isRunning = true;
        try {
            suProcess = Runtime.getRuntime().exec("su");
            outputStream = new DataOutputStream(suProcess.getOutputStream());
            stdoutReader = new BufferedReader(new InputStreamReader(suProcess.getInputStream()));
            stderrReader = new BufferedReader(new InputStreamReader(suProcess.getErrorStream()));

            // 启动线程读取标准输出
            new Thread(() -> {
                try {
                    String line;
                    while ((line = stdoutReader.readLine()) != null) {
                        Log.d(TAG, "stdout: " + line);
                        handlerMsg(line);
                        //  logWindow.print(line);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading stdout: " + e.getMessage());
                }
            }).start();

            // 启动线程读取标准错误
            new Thread(() -> {
                try {
                    String line;
                    while ((line = stderrReader.readLine()) != null) {
                        Log.e(TAG, "stderr: " + line);
                        // logWindow.print(line);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading stderr: " + e.getMessage());
                }
            }).start();

            // 持续执行命令（示例：每隔5秒执行一次 `ls`）
            new Thread(() -> {
                try {
                    while (isRunning) {
                        outputStream.writeBytes("uiautomator dump " + file + "\n"); // 替换为你的命令
                        outputStream.flush();
                        Thread.sleep(2000); // 5秒间隔
                    }
                } catch (IOException | InterruptedException e) {
                    Log.e(TAG, "Error executing command: " + e.getMessage());
                }
            }).start();

        } catch (IOException e) {
            Log.e(TAG, "Failed to start su shell: " + e.getMessage());
        }
    }

    //重新获取一次ui数据
    public List<UiXmlParser.Node> getNodes() {
        executeSuCommand("uiautomator dump " + file + "\n");
        UiXmlParser uiXmlParser = new UiXmlParser(file);
        uiXmlParser.parseUiXml();
        return uiXmlParser.getNodes();
    }

    //点击控件
    public void click(Rect rect) {
        if (rect == null) return;
        int x = rect.centerX();
        int y = rect.centerY();
        String text = String.format("tap %s %s", x, y);
        inputText(text);
    }

    public void click(List<Rect> rects) {
        try {
            for (Rect rect : rects) {
                int x = rect.centerX();
                int y = rect.centerY();
                outputStream.writeBytes(String.format("input tap %s %s\n", x, y));
            }
            outputStream.flush();
        } catch (IOException e) {
            Log.i(TAG, "点击失败");
        }
    }

    //输入文字
    public void input(String text) {
        String s = String.format("text \"%s\"", text);
        inputText(s);
    }

    //返回上一页
    public void back() {
        try {
            outputStream.writeBytes("input keyevent KEYCODE_BACK\n");
            outputStream.flush();
        } catch (IOException e) {
            Log.i(TAG, "点击失败");
        }
    }

    private void inputText(String text) {
        try {
            outputStream.writeBytes(String.format("input %s\n", text));
            outputStream.flush();
        } catch (IOException e) {
            Log.i(TAG, "点击失败");
        }
    }

    private boolean isOk = true;

    //id.co.bri.brimo.ui.activities.FastMenuActivity
    private void handlerMsg(String line) {
        String ui = "UI hierchary dumped to: " + file;
        if (line.equals(ui)) {
            UiXmlParser uiXmlParser = new UiXmlParser(file);
            uiXmlParser.parseUiXml();
            List<UiXmlParser.Node> nodes = uiXmlParser.getNodes();
            String activity = executeSuCommand("dumpsys activity activities | grep  mFocusedWindow");
            if (activity != null) {
                activity = extractActivityName(activity);
                if (activity != null) {
                    Log.d(TAG, "当前Activity：" + activity);
                    if (activityScripts.containsKey(activity)) {
                        if (isOk) {
                            isOk = false;
                            activityScripts.get(activity).onCreate(this, nodes);
                            isOk = true;
                        }
                    }
                }
            }
        }
    }

    public static String extractActivityName(String input) {
        // 提取第一条日志
        String firstLine = input.split("\n")[0].trim();

        // 分割字符串获取Activity全路径
        String[] parts = firstLine.split(" ");
        String fullPath = parts[2]; // 得到 "bin.mt.plus/bin.mt.plus.MainLightIcon"

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("/([^/]+)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(fullPath);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 执行 su 命令并返回响应
     *
     * @param command 需要执行的命令（例如 "whoami"）
     * @return 命令的响应结果，如果失败则返回 null
     */
    public String executeSuCommand(String command) {
        Process process = null;
        BufferedReader reader = null;
        StringBuilder output = new StringBuilder();

        try {
            // 执行 su 命令
            process = Runtime.getRuntime().exec("su");

            // 向 su 进程写入命令
            process.getOutputStream().write((command + "\n").getBytes());
            process.getOutputStream().flush();
            process.getOutputStream().write("exit\n".getBytes());
            process.getOutputStream().flush();

            // 读取命令的输出
            InputStream inputStream = process.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // 等待命令执行完成
            process.waitFor();

            return output.toString().trim();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        } finally {
            // 关闭资源
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * 停止 su shell 并关闭资源
     */
    private void stopSuShell() {
        isRunning = false;
        try {
            if (outputStream != null) {
                outputStream.writeBytes("exit\n");
                outputStream.flush();
                outputStream.close();
            }
            if (stdoutReader != null) {
                stdoutReader.close();
            }
            if (stderrReader != null) {
                stderrReader.close();
            }
            if (suProcess != null) {
                suProcess.destroy();
            }
        } catch (IOException e) {
            Log.e(TAG, "Failed to stop su shell: " + e.getMessage());
        }
    }


    @Getter
    public static class UiXmlParser {
        private final String filePath;
        private final List<Node> nodes = new ArrayList<>();

        public UiXmlParser(String file) {
            this.filePath = file;
        }

        private static final String TAG = "UiXmlParser";

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

        //<node index="0" text="" resource-id="" class="android.widget.FrameLayout" package="com.xposed.briscriptx" content-desc="" checkable="false" checked="false" clickable="false" enabled="true" focusable="false" focused="false" scrollable="false" long-clickable="false" password="false" selected="false" bounds="[0,0][1080,2120]">


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

    public static class LogWindow {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final SuShellService service;
        private final LayoutLogBinding binding;
        private WindowManager windowManager;

        public LogWindow(SuShellService service) {
            this.service = service;
            this.binding = LayoutLogBinding.inflate(LayoutInflater.from(service));
            init();
        }

        public void print(String str) {
            handler.post(() -> printA(str));
        }

        private void printA(String str) {
            // 获取当前文本
            String currentText = binding.text.getText().toString();
            String[] lines = currentText.split("\n");

            // 检查最新内容是否已经存在
            boolean isDuplicate = false;
            for (String line : lines) {
                if (line.equals(str)) {
                    isDuplicate = true;
                    break;
                }
            }
            // 如果内容不存在，才追加
            if (!isDuplicate) {
                // 如果行数超过 20，移除最早的行
                if (lines.length >= 10) {
                    StringBuilder newText = new StringBuilder();
                    // 保留最后 19 行
                    for (int i = lines.length - 9; i < lines.length; i++) {
                        newText.append(lines[i]);
                    }
                    binding.text.setText(newText.toString());
                }
                // 追加新内容并滚动到底部
                binding.text.append("\n" + getCurrentDate() + ": " + str);
                binding.scroll.post(() -> binding.scroll.fullScroll(View.FOCUS_DOWN));
            }
        }

        public static String getCurrentDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-ss", Locale.getDefault());
            return sdf.format(new Date());
        }

        private void init() {
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    dpToPx(service, 300),
                    dpToPx(service, 100),
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |  // 关键：悬浮窗不获取焦点
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, // 关键：悬浮窗不可触摸（穿透点击）
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.END;
            windowManager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
            windowManager.addView(binding.getRoot(), params);
        }

        public void destroy() {
            windowManager.removeView(binding.getRoot());
        }

        public int dpToPx(Context context, float dp) {
            float density = context.getResources().getDisplayMetrics().density;
            return (int) (dp * density + 0.5f); // 四舍五入
        }
    }


}
