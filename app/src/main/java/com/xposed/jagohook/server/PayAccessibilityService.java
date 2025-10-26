package com.xposed.jagohook.server;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import com.xposed.jagohook.config.AppConfig;
import com.xposed.jagohook.databinding.LayoutLogBinding;
import com.xposed.jagohook.room.AppDatabase;
import com.xposed.jagohook.room.dao.PostPayErrorDao;
import com.xposed.jagohook.room.entity.PostPayErrorEntity;
import com.xposed.jagohook.runnable.PayRunnable;
import com.xposed.jagohook.runnable.PostPayErrorRunnable;
import com.xposed.jagohook.runnable.response.TakeLatestOrderBean;
import com.xposed.jagohook.utils.AccessibleUtil;
import com.xposed.jagohook.utils.Logs;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Getter
@Setter
public class PayAccessibilityService extends AccessibilityService {
    // ========== 代付相关 ==========
    private boolean isRunning = false;
    private TakeLatestOrderBean takeLatestOrderBean;
    private PostPayErrorRunnable postPayErrorRunnable;
    private PayRunnable payRunnable;
    private String balance = "0";

    // ========== ui操作 ==========
    private LogWindow logWindow;
    private boolean isTransfer = false;

    // ========== 配置 ==========
    private AppConfig appConfig;

    @Override
    public void onCreate() {
        super.onCreate();
        appConfig = new AppConfig(this);
        logWindow = new LogWindow(this);

        postPayErrorRunnable = new PostPayErrorRunnable(this);
        payRunnable = new PayRunnable(this);
        new Thread(postPayErrorRunnable).start();
        new Thread(payRunnable).start();

        isRunning = true;

        logWindow.printA("代付运行中");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        try {
            AccessibilityNodeInfo nodeInfo = accessibilityEvent.getSource();
            Map<String, AccessibilityNodeInfo> nodeInfoMap = AccessibleUtil.toContentDescMap(nodeInfo);
            ScreenLockPassword(nodeInfoMap);
            getBalance(nodeInfoMap);
            BottomNavigationBar(nodeInfoMap);
            Transfer(nodeInfoMap, nodeInfo);
            Dialogs(nodeInfoMap);
            Thread.sleep(1000);
        } catch (Throwable e) {
            Logs.d("异常:" + e.getMessage());
        }
    }

    //弹窗直接点击确认
    private void Dialogs(Map<String, AccessibilityNodeInfo> nodeInfoMap) {
        if (nodeInfoMap.containsKey("Sesi berakhir")) {//登录失效
            clickButton(nodeInfoMap.get("Oke "));
        }

        if (nodeInfoMap.containsKey("Bank tujuan tidak merespon")) {//卡号错误
            TakeLatestOrderBean id = takeLatestOrderBean;
            postCollectStatus(0, "卡号错误", id);
            takeLatestOrderBean = null;
            isTransfer = false;
            balance = "0";
            Logs.d("转账失败");
            logWindow.printA("归集失败");
            clickButton(nodeInfoMap.get("Oke "));
        }

        if (takeLatestOrderBean == null) {
            if (nodeInfoMap.containsKey("Search Text Field")) {
                AccessibilityNodeInfo accessibilityNodeInfo = nodeInfoMap.get("Search Text Field");
                if (accessibilityNodeInfo != null) {
                    AccessibilityNodeInfo nodeInfo = accessibilityNodeInfo.getParent();
                    if (nodeInfo != null) {
                        AccessibilityNodeInfo nodeInfo1 = nodeInfo.getChild(3);
                        clickButton(nodeInfo1);
                    }
                }
            }
        }
    }

    private void postCollectStatus(int state, String error, TakeLatestOrderBean takeLatestOrderBean) {
        PullPost(state, error, takeLatestOrderBean);
    }

    //提交订单
    public void PullPost(int state, String error, TakeLatestOrderBean transferBean) {
        if (transferBean == null) return;
        FormBody.Builder requestBody = new FormBody.Builder();
        if (error.equals("Transaction in Progress")) {
            state = 1;
        }
        if (state == 1) {
            requestBody.add("paymentCertificate", "Transaction Successful");
            Logs.d(transferBean.getOrderNo() + "转账完毕，结果:成功");
        } else {
            Logs.d(transferBean.getOrderNo() + "转账完毕，结果:失败 原因:" + error);
        }
        requestBody.add("state", String.valueOf(state));
        String timeStr = new android.icu.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE).format(System.currentTimeMillis());
        requestBody.add("paymentTime", timeStr);
        requestBody.add("failReason", error);
        requestBody.add("amount", String.valueOf(transferBean.getAmount()));
        requestBody.add("orderNo", transferBean.getOrderNo());
        Request request = new Request.Builder()
                .post(requestBody.build())
                .url(appConfig.getPayUrl() + "app/payoutOrderCallback")
                .build();
        OkHttpClient client = new OkHttpClient();
        int finalState = state;
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppDatabase appDatabase = AppDatabase.getInstance(PayAccessibilityService.this);
                PostPayErrorDao billDao = appDatabase.postPayErrorDao();
                PostPayErrorEntity postCollectionErrorEntity = new PostPayErrorEntity();
                postCollectionErrorEntity.setOrderNo(transferBean.getOrderNo());
                postCollectionErrorEntity.setAmount(String.valueOf(transferBean.getAmount()));
                postCollectionErrorEntity.setState(finalState);
                postCollectionErrorEntity.setFailReason(error);
                postCollectionErrorEntity.setPaymentTime(timeStr);
                billDao.insert(postCollectionErrorEntity);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) return;
                AppDatabase appDatabase = AppDatabase.getInstance(PayAccessibilityService.this);
                PostPayErrorDao billDao = appDatabase.postPayErrorDao();
                PostPayErrorEntity postCollectionErrorEntity = new PostPayErrorEntity();
                postCollectionErrorEntity.setOrderNo(transferBean.getOrderNo());
                postCollectionErrorEntity.setAmount(String.valueOf(transferBean.getAmount()));
                postCollectionErrorEntity.setState(finalState);
                postCollectionErrorEntity.setFailReason(error);
                postCollectionErrorEntity.setPaymentTime(timeStr);
                billDao.insert(postCollectionErrorEntity);
            }
        });
    }

    //转账
    private void Transfer(Map<String, AccessibilityNodeInfo> nodeInfoMap, AccessibilityNodeInfo nodeInfo) {
        if (takeLatestOrderBean == null) return;

        //点击转账按钮
        if (!takeLatestOrderBean.isMoney()) {
            if (!isTransfer && nodeInfoMap.containsKey("Bank\n" +
                    "Transfer")) {
                clickButton(nodeInfoMap.get("Bank\n" +
                        "Transfer"));
                isTransfer = true;
            }
        } else {//进行钱包转账
            if (!isTransfer && nodeInfoMap.containsKey("Topup\n" +
                    "e-Wallet")) {
                clickButton(nodeInfoMap.get("Topup\n" +
                        "e-Wallet"));
                isTransfer = true;
            }
        }

        //等待转账状态
        if (nodeInfoMap.containsKey("Uang Berhasil Dikirim!")) {
            TakeLatestOrderBean id = takeLatestOrderBean;
            postCollectStatus(1, "转账成功", id);
            takeLatestOrderBean = null;
            isTransfer = false;
            balance = "0";
            logWindow.printA("归集成功");
            Logs.d("转账成功");
            if (nodeInfoMap.containsKey("Selesai")) {
                clickButton(nodeInfoMap.get("Selesai"));
            }
        }

        //选择银行
        if (!takeLatestOrderBean.isMoney()) {
            if (nodeInfoMap.containsKey("Title Transfer ke Bank")) {
                //输入银行搜索
                initCard(nodeInfoMap, takeLatestOrderBean.getBankName());
            }
        }

        //银行存在
        if (!takeLatestOrderBean.isMoney()) {
            if (nodeInfoMap.containsKey(takeLatestOrderBean.getBankName() + "\n" +
                    "BI-FAST")) {
                clickButton(nodeInfoMap.get(takeLatestOrderBean.getBankName() + "\n" +
                        "BI-FAST"));
            }
        } else {//选择钱包银行
            if (nodeInfoMap.containsKey(takeLatestOrderBean.getBankName())) {
                clickButton(nodeInfoMap.get(takeLatestOrderBean.getBankName()));
            }
        }


        //确定转账
        if (nodeInfoMap.containsKey("Cek Ulang Transaksi")) {
            if (nodeInfoMap.containsKey("Kirim Sekarang ")) {
                AccessibilityNodeInfo nodeInfo1 = nodeInfoMap.get("Kirim Sekarang ");
                if (nodeInfo1 != null) {
                    clickButton(nodeInfo1);
                }
            }
        }

        //输入密码
        if (nodeInfoMap.containsKey("Masukkan PIN")) {
            String pass = appConfig.getLockPass();
            for (int i = 0; i < pass.length(); i++) {
                String key = String.valueOf(pass.charAt(i));
                if (nodeInfoMap.containsKey(key)) {
                    AccessibilityNodeInfo accessibilityNodeInfo = nodeInfoMap.get(key);
                    if (accessibilityNodeInfo == null) return;
                    accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        }

        if (nodeInfoMap.containsKey("Keterangan penerima")) {
            //判断是否输入成功
            if (nodeInfoMap.containsKey("Lanjut ")) {
                clickButton(nodeInfoMap.get("Lanjut "));
            }
        }

        //输入金额
        if (nodeInfoMap.containsKey("Text Input Amount")) {
            AccessibilityNodeInfo info = nodeInfoMap.get("Text Input Amount");
            if (info != null) {
                AccessibilityNodeInfo accessibilityNodeInfo1 = info.getChild(0);
                if (accessibilityNodeInfo1 != null) {
                    accessibilityNodeInfo1.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                    AccessibleUtil.inputTextByAccessibility(accessibilityNodeInfo1, String.valueOf(takeLatestOrderBean.getAmount()));
                }

                //判断是否输入成功
                if (nodeInfoMap.containsKey("Lanjut ")) {
                    AccessibilityNodeInfo Lanjut = nodeInfoMap.get("Lanjut ");
                    if (Lanjut != null) {
                        if (Lanjut.isClickable()) {
                            clickButton(Lanjut);
                        }
                    }
                }
            }
        }


        //输入银行卡号
        if (nodeInfoMap.containsKey("Periksa") && nodeInfoMap.containsKey(takeLatestOrderBean.getBankName())) {
            initCard(nodeInfoMap, takeLatestOrderBean.getCardNumber());
        }

        //输入卡号成功后
        Map<String, AccessibilityNodeInfo> nodeInfoMap1 = AccessibleUtil.toTextMap(nodeInfo);
        if (nodeInfoMap.containsKey("Periksa") && nodeInfoMap.containsKey(takeLatestOrderBean.getBankName()) && nodeInfoMap1.containsKey(takeLatestOrderBean.getBankName())) {
            clickButton(nodeInfoMap.get("Periksa"));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }


    //底部导航栏处理
    private void BottomNavigationBar(Map<String, AccessibilityNodeInfo> nodeInfoMap) {
        if (isTransfer) return;
        if (takeLatestOrderBean != null && nodeInfoMap.containsKey("Aktivitas Terakhir")) {//首页特征码
            if (nodeInfoMap.containsKey("Transaksi\n" +
                    "Tab 3 dari 5")) {
                AccessibilityNodeInfo Transaksi = nodeInfoMap.get("Transaksi\n" +
                        "Tab 3 dari 5");
                clickButton(Transaksi);
            }
        }
        if (takeLatestOrderBean == null && nodeInfoMap.containsKey("Bank\n" +
                "Transfer")) {//转账页面,点击前往首页
            if (nodeInfoMap.containsKey("Beranda\n" +
                    "Tab 1 dari 5")) {
                clickButton(nodeInfoMap.get("Beranda\n" +
                        "Tab 1 dari 5"));
            }
        }
    }

    private void clickButton(AccessibilityNodeInfo nodeInfo) {
        if (nodeInfo == null) return;
        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
    }

    //获取首页的余额
    private void getBalance(Map<String, AccessibilityNodeInfo> nodeInfoMap) {
        if (!nodeInfoMap.containsKey("Aktivitas Terakhir")) return;
        AccessibilityNodeInfo nodeInfo = AccessibleUtil.toStateContentDescMap(nodeInfoMap, "Rp");
        if (nodeInfo == null) return;
        String balance = nodeInfo.getContentDescription().toString();
        String numbersOnly = balance.replaceAll("[^0-9]", "");
        if (!numbersOnly.isEmpty()) {
            this.balance = numbersOnly;
            Logs.d("余额：" + numbersOnly);
        }
        clickButton(nodeInfo);
    }

    //屏幕输入密码
    private void ScreenLockPassword(Map<String, AccessibilityNodeInfo> nodeInfoMap) {
        if (!nodeInfoMap.containsKey("GUNAKAN PASSWORD")) return;
        String pass = appConfig.getLockPass();
        for (int i = 0; i < pass.length(); i++) {
            String key = String.valueOf(pass.charAt(i));
            if (nodeInfoMap.containsKey(key)) {
                AccessibilityNodeInfo accessibilityNodeInfo = nodeInfoMap.get(key);
                if (accessibilityNodeInfo == null) return;
                accessibilityNodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    private void initCard(Map<String, AccessibilityNodeInfo> nodeInfoMap, String text) {
        if (nodeInfoMap.containsKey("Search Text Field")) {
            AccessibilityNodeInfo accessibilityNodeInfo = nodeInfoMap.get("Search Text Field");
            if (accessibilityNodeInfo == null) return;
            AccessibilityNodeInfo accessibilityNodeInfo1 = accessibilityNodeInfo.getChild(0);
            if (accessibilityNodeInfo1 != null) {
                accessibilityNodeInfo1.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                AccessibleUtil.inputTextByAccessibility(accessibilityNodeInfo1, text);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        logWindow.destroy();
        isRunning = false;
    }

    @Override
    public void onInterrupt() {

    }

    public static class LogWindow {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final PayAccessibilityService service;
        private final LayoutLogBinding binding;
        private WindowManager windowManager;

        public LogWindow(PayAccessibilityService service) {
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
