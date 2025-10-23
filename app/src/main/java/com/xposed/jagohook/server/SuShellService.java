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
import com.xposed.jagohook.shell.ShellCommandExecutor;
import com.xposed.jagohook.thread.ThreadPoolManager;
import com.xposed.jagohook.ui.UiXmlParser;
import com.xposed.jagohook.utils.ActivityExtractor;

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
import java.util.concurrent.Future;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
public class SuShellService extends Service {
    
    // ========== 常量定义 ==========
    private static final String TAG = "SuShellService";
    private static final long MIN_PROCESS_INTERVAL = 2000; // 最小处理间隔2秒
    
    // ========== 组件管理 ==========
    private ThreadPoolManager threadPoolManager;
    private ShellCommandExecutor shellExecutor;
    
    // ========== UI处理相关 ==========
    private volatile boolean isProcessing = false;
    private final Object lock = new Object();
    private long lastProcessTime = 0;
    
    // ========== Shell进程相关 ==========
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Process suProcess;
    private DataOutputStream outputStream;
    private BufferedReader stdoutReader;
    private BufferedReader stderrReader;
    private boolean isRunning = true;
    
    // ========== 文件路径 ==========
    private String file;
    
    // ========== 业务逻辑相关 ==========
    private String balance = "0";
    private CollectBillResponse collectBillResponse;
    private CollectRunnable collectRunnable;
    private LogWindow logWindow;
    
    // ========== Activity脚本映射 ==========
    private final Map<String, BaseScript> activityScripts = new HashMap<>() {{
        put("com.jago.digitalBanking.MainActivity", new MainActivityScript());
    }};

    // ========== Service生命周期方法 ==========
    
    @Override
    public void onCreate() {
        super.onCreate();
        initializeComponents();
        startBackgroundTasks();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupResources();
        shutdownThreadPool();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new ScreenRecordBinder();
    }

    // ========== 初始化方法 ==========
    
    /**
     * 初始化组件
     */
    private void initializeComponents() {
        file = getCacheDir().getAbsolutePath() + "/ui.xml";
        logWindow = new LogWindow(this);
        collectRunnable = new CollectRunnable(this);
        threadPoolManager = new ThreadPoolManager();
        shellExecutor = new ShellCommandExecutor();
    }
    
    /**
     * 启动后台任务
     */
    private void startBackgroundTasks() {
        threadPoolManager.submit(collectRunnable);
    }

    // ========== 业务方法 ==========
    
    /**
     * 设置归集响应
     */
    public void setCollectBillResponse(CollectBillResponse collectBillResponse) {
        this.collectBillResponse = collectBillResponse;
        if (collectBillResponse != null) {
            logWindow.print("触发归集");
        }
    }
    
    /**
     * 开启服务
     */
    public void start() {
        startSuShell();
    }
    
    /**
     * 关闭服务
     */
    public void stop() {
        threadPoolManager.submit(this::stopSuShell);
    }

    // ========== 资源管理方法 ==========
    
    /**
     * 清理资源
     */
    private void cleanupResources() {
        logWindow.destroy();
        threadPoolManager.submit(this::stopSuShell);
    }
    
    /**
     * 优雅关闭线程池
     */
    private void shutdownThreadPool() {
        if (threadPoolManager != null) {
            threadPoolManager.shutdown();
        }
    }
    
    /**
     * 取消所有正在执行的任务
     */
    private void cancelAllTasks() {
        // 由于现在使用ThreadPoolManager，不需要单独取消任务
        // ThreadPoolManager会在关闭时自动取消所有任务
    }


    // ========== Binder类 ==========
    
    /**
     * Binder 用于与 Activity 通信
     */
    public class ScreenRecordBinder extends Binder {
        public SuShellService getService() {
            return SuShellService.this;
        }
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

            // 使用线程池启动线程读取标准输出
            threadPoolManager.submit(() -> {
                try {
                    String line;
                    while ((line = stdoutReader.readLine()) != null) {
                        handlerMsg(line);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading stdout: " + e.getMessage());
                }
            });

            // 使用线程池启动线程读取标准错误
            threadPoolManager.submit(() -> {
                try {
                    String line;
                    while ((line = stderrReader.readLine()) != null) {
                        Log.e(TAG, "stderr: " + line);
                        // logWindow.print(line);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading stderr: " + e.getMessage());
                }
            });

            // 使用线程池持续执行命令
            threadPoolManager.submit(() -> {
                try {
                    while (isRunning) {
                        outputStream.writeBytes("uiautomator dump " + file + "\n");
                        outputStream.flush();
                        Thread.sleep(1000); // 1秒间隔
                    }
                } catch (IOException | InterruptedException e) {
                    Log.e(TAG, "Error executing command: " + e.getMessage());
                }
            });

        } catch (IOException e) {
            Log.e(TAG, "Failed to start su shell: " + e.getMessage());
        }
    }

    //点击控件
    public void click(Rect rect) {
        if (rect == null) return;
        int x = rect.centerX();
        int y = rect.centerY();
        String text = String.format("tap %s %s", x, y);
        inputText(text);
    }

    public String click(List<Rect> rects) {
        StringBuilder builder = new StringBuilder();
        for (Rect rect : rects) {
            int x = rect.centerX();
            int y = rect.centerY();
            builder.append(String.format("input tap %s %s\n", x, y));
        }
        return executeSuCommand(builder.toString());
    }

    // 或者使用更可靠的输入方式（推荐）
    public void inputStableX(String text) {
        StringBuilder stringBuilder = new StringBuilder();
        // 逐个字符输入（更稳定）
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)) {
                stringBuilder.append(String.format("input text \"%c\"\n", c));
            }
        }
        executeSuCommand(stringBuilder.toString());
    }


    public String inputString(String text) {
        StringBuilder stringBuilder = new StringBuilder();
        // 逐个字符输入（更稳定）
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)) {
                stringBuilder.append(String.format("input text \"%c\"\n", c));
            }
        }
        return stringBuilder.toString();
    }


    //输入文字
    public String input(String text) {
        String s = String.format("text \"%s\"", text);
        return inputText(s);
    }

    //返回上一页
    public String back() {
        return executeSuCommand("input keyevent KEYCODE_BACK");
    }

    private String inputText(String text) {
        return executeSuCommand(String.format("input %s", text));
    }

    //id.co.bri.brimo.ui.activities.FastMenuActivity
    private void handlerMsg(String line) {
        String ui = "UI hierchary dumped to: " + file;

        // 检查是否是需要处理的UI dump消息
        if (!line.equals(ui)) {
            return;
        }

        // 检查是否正在处理中，避免二次频繁执行
        synchronized (lock) {
            long currentTime = System.currentTimeMillis();
            if (isProcessing || (currentTime - lastProcessTime) < MIN_PROCESS_INTERVAL) {
                Log.d(TAG, "跳过处理，正在执行中或时间间隔太短");
                return;
            }

            isProcessing = true;
            lastProcessTime = currentTime;
        }

        // 使用线程池在后台线程中执行处理逻辑
        threadPoolManager.submit(() -> {
            try {
                Log.d(TAG, "开始处理UI dump");

                // 解析UI XML
                UiXmlParser uiXmlParser = new UiXmlParser(file);
                uiXmlParser.parseUiXml();
                List<UiXmlParser.Node> nodes = uiXmlParser.getNodes();

                // 获取当前Activity
                String activity = executeSuCommand("dumpsys activity activities | grep mFocusedWindow");
                if (activity != null) {
                    activity = extractActivityName(activity);
                    if (activity != null) {
                        Log.d(TAG, "当前Activity：" + activity);

                        // 执行对应的脚本
                        if (activityScripts.containsKey(activity)) {
                            activityScripts.get(activity).onCreate(this, nodes);
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "处理UI dump时发生错误: " + e.getMessage());
            } finally {
                // 处理完成，重置状态
                synchronized (lock) {
                    isProcessing = false;
                }
                Log.d(TAG, "UI dump处理完成");
            }
        });
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
