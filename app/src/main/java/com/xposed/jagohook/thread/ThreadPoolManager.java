package com.xposed.jagohook.thread;

import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 线程池管理器
 * 统一管理应用中的所有线程池和任务
 */
public class ThreadPoolManager {
    
    private static final String TAG = "ThreadPoolManager";
    private static final int MAX_THREADS = 5;
    
    private ExecutorService threadPool;
    
    public ThreadPoolManager() {
        threadPool = Executors.newFixedThreadPool(MAX_THREADS);
    }
    
    /**
     * 提交任务到线程池
     */
    public Future<?> submit(Runnable task) {
        if (threadPool == null || threadPool.isShutdown()) {
            Log.w(TAG, "线程池已关闭，无法提交新任务");
            return null;
        }
        return threadPool.submit(task);
    }
    
    /**
     * 优雅关闭线程池
     */
    public void shutdown() {
        if (threadPool != null && !threadPool.isShutdown()) {
            try {
                // 停止接受新任务并等待现有任务完成
                threadPool.shutdown();
                
                // 等待所有任务完成，最多等待30秒
                if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    // 如果超时，强制关闭
                    threadPool.shutdownNow();
                    Log.w(TAG, "线程池强制关闭");
                }
                Log.d(TAG, "线程池已优雅关闭");
            } catch (InterruptedException e) {
                Log.e(TAG, "线程池关闭时被中断: " + e.getMessage());
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 强制关闭线程池
     */
    public void shutdownNow() {
        if (threadPool != null && !threadPool.isShutdown()) {
            threadPool.shutdownNow();
            Log.d(TAG, "线程池强制关闭");
        }
    }
    
    /**
     * 检查线程池是否已关闭
     */
    public boolean isShutdown() {
        return threadPool == null || threadPool.isShutdown();
    }
    
    /**
     * 取消任务
     */
    public void cancelTask(Future<?> task) {
        if (task != null && !task.isDone()) {
            task.cancel(true);
        }
    }
}