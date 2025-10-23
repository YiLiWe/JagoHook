package com.xposed.jagohook.shell;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Shell命令执行器
 * 负责执行Shell命令并返回结果
 */
public class ShellCommandExecutor {
    
    private static final String TAG = "ShellCommandExecutor";
    
    /**
     * 执行Shell命令并返回结果
     */
    public String executeCommand(String command) {
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
            Log.e(TAG, "执行命令失败: " + e.getMessage());
            return null;
        } finally {
            // 关闭资源
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭读取器失败: " + e.getMessage());
                }
            }
            if (process != null) {
                process.destroy();
            }
        }
    }
    
    /**
     * 点击屏幕指定坐标
     */
    public String tap(int x, int y) {
        String command = String.format("input tap %d %d", x, y);
        return executeCommand(command);
    }
    
    /**
     * 输入文本
     */
    public String inputText(String text) {
        String command = String.format("input text \"%s\"", text);
        return executeCommand(command);
    }
    
    /**
     * 返回上一页
     */
    public String back() {
        return executeCommand("input keyevent KEYCODE_BACK");
    }
    
    /**
     * 执行UI自动化dump
     */
    public String dumpUI(String filePath) {
        String command = String.format("uiautomator dump %s", filePath);
        return executeCommand(command);
    }
    
    /**
     * 获取当前Activity信息
     */
    public String getCurrentActivity() {
        return executeCommand("dumpsys activity activities | grep mFocusedWindow");
    }
}