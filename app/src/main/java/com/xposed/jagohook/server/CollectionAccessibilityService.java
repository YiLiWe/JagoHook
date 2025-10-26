package com.xposed.jagohook.server;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
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
import com.xposed.jagohook.room.dao.BillDao;
import com.xposed.jagohook.room.dao.PostCollectionErrorDao;
import com.xposed.jagohook.room.entity.BillEntity;
import com.xposed.jagohook.room.entity.PostCollectionErrorEntity;
import com.xposed.jagohook.runnable.BillRunnable;
import com.xposed.jagohook.runnable.CollectionAccessibilityRunnable;
import com.xposed.jagohook.runnable.PostCollectionErrorRunnable;
import com.xposed.jagohook.runnable.response.CollectBillResponse;
import com.xposed.jagohook.utils.AccessibleUtil;
import com.xposed.jagohook.utils.Logs;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Getter
@Setter
public class CollectionAccessibilityService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());

    // ========== 归集相关变量 ==========
    private CollectionAccessibilityRunnable collectionAccessibilityRunnable;
    private PostCollectionErrorRunnable postCollectionErrorRunnable;
    private volatile CollectBillResponse collectBillResponse;
    private volatile String balance = "0";
    private volatile boolean isRunning = false;
    private String cardNumber;
    private String collectUrl;


    //收款提交
    private BillRunnable billRunnable;

    // ========== ui相关变量 ==========
    private boolean isBill = true;
    //转账中，不点击转账按钮
    private boolean isTransfer = false;
    private AppConfig appConfig;
    private long lastExecutionTime = 0;

    // ========== 悬浮窗 ==========
    private LogWindow logWindow;


    @Override
    public void onCreate() {
        super.onCreate();
        initData();
        billRunnable = new BillRunnable(this);
        logWindow = new LogWindow(this);
        appConfig = new AppConfig(this);
        postCollectionErrorRunnable = new PostCollectionErrorRunnable(this);
        collectionAccessibilityRunnable = new CollectionAccessibilityRunnable(this);
        new Thread(billRunnable).start();
        new Thread(collectionAccessibilityRunnable).start();
        new Thread(postCollectionErrorRunnable).start();
        isRunning = true;
        logWindow.printA("代收/归集运行中");

    }

    public void initData() {
        SharedPreferences sharedPreferences = getSharedPreferences("info", Context.MODE_PRIVATE);
        cardNumber = sharedPreferences.getString("cardNumber", null);
        collectUrl = sharedPreferences.getString("collectUrl", null);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        logWindow.destroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        try {
            AccessibilityNodeInfo nodeInfo = accessibilityEvent.getSource();
            Map<String, AccessibilityNodeInfo> nodeInfoMap = AccessibleUtil.toContentDescMap(nodeInfo);
            ScreenLockPassword(nodeInfoMap);
            getBalance(nodeInfoMap);
            BottomNavigationBar(nodeInfoMap);
            clickBill(nodeInfoMap);
            getBill(nodeInfoMap);
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
            long id = collectBillResponse.getId();
            postCollectStatus(2, "卡号错误", id);
            collectBillResponse = null;
            isTransfer = false;
            balance = "0";
            Logs.d("转账失败");
            logWindow.printA("归集失败");
            clickButton(nodeInfoMap.get("Oke "));
        }

        if (collectBillResponse == null) {
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

    private void postCollectStatus(int state, String error, long id) {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(String.format("%sv1/collectStatus?id=%s&state=%s&error=%s", collectUrl, id, state, error))
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppDatabase appDatabase = AppDatabase.getInstance(CollectionAccessibilityService.this);
                PostCollectionErrorDao billDao = appDatabase.postCollectionErrorDao();
                PostCollectionErrorEntity postCollectionErrorEntity = new PostCollectionErrorEntity();
                postCollectionErrorEntity.setId(id);
                postCollectionErrorEntity.setState(state);
                postCollectionErrorEntity.setError(error);
                billDao.insert(postCollectionErrorEntity);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    AppDatabase appDatabase = AppDatabase.getInstance(CollectionAccessibilityService.this);
                    PostCollectionErrorDao billDao = appDatabase.postCollectionErrorDao();
                    PostCollectionErrorEntity postCollectionErrorEntity = new PostCollectionErrorEntity();
                    postCollectionErrorEntity.setId(id);
                    postCollectionErrorEntity.setState(state);
                    postCollectionErrorEntity.setError(error);
                    billDao.insert(postCollectionErrorEntity);
                }
            }
        });
    }

    //转账
    private void Transfer(Map<String, AccessibilityNodeInfo> nodeInfoMap, AccessibilityNodeInfo nodeInfo) {
        if (collectBillResponse == null) return;

        //点击转账按钮
        if (!isTransfer && nodeInfoMap.containsKey("Bank\n" +
                "Transfer")) {
            clickButton(nodeInfoMap.get("Bank\n" +
                    "Transfer"));
            isTransfer = true;
        }

        //等待转账状态
        if (nodeInfoMap.containsKey("Uang Berhasil Dikirim!")) {
            long id = collectBillResponse.getId();
            postCollectStatus(1, "转账成功", id);
            collectBillResponse = null;
            isTransfer = false;
            balance = "0";
            logWindow.printA("归集成功");
            Logs.d("转账成功");
            if (nodeInfoMap.containsKey("Selesai")) {
                clickButton(nodeInfoMap.get("Selesai"));
            }
        }

        //选择银行
        if (nodeInfoMap.containsKey("Title Transfer ke Bank")) {
            //输入银行搜索
            initCard(nodeInfoMap, collectBillResponse.getBank());
        }

        //银行存在
        if (nodeInfoMap.containsKey(collectBillResponse.getBank() + "\n" +
                "BI-FAST")) {
            clickButton(nodeInfoMap.get(collectBillResponse.getBank() + "\n" +
                    "BI-FAST"));
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
                    AccessibleUtil.inputTextByAccessibility(accessibilityNodeInfo1, String.valueOf(collectBillResponse.getIdPlgn()));
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
        if (nodeInfoMap.containsKey("Periksa") && nodeInfoMap.containsKey(collectBillResponse.getBank())) {
            initCard(nodeInfoMap, collectBillResponse.getPhone());
        }

        //输入卡号成功后
        Map<String, AccessibilityNodeInfo> nodeInfoMap1 = AccessibleUtil.toTextMap(nodeInfo);
        if (nodeInfoMap.containsKey("Periksa") && nodeInfoMap.containsKey(collectBillResponse.getBank()) && nodeInfoMap1.containsKey(collectBillResponse.getPhone())) {
            clickButton(nodeInfoMap.get("Periksa"));
        }


    }

    //获取账单
    private void getBill(Map<String, AccessibilityNodeInfo> nodeInfoMap) {
        if (nodeInfoMap.containsKey("Aktivitas Semua Kantong")) {
            Map<String, AccessibilityNodeInfo> nodeInfoMap1 = AccessibleUtil.toStateContentDescMapX(nodeInfoMap, "Transaction Item");
            Logs.d("获取账单总数:" + nodeInfoMap1.size());
            handlerData(nodeInfoMap1);
            //Transaction Item : +Rp10.000 : 23 Okt 2025 : Uang keluar
            if (nodeInfoMap.containsKey("Back Button")) {
                AccessibilityNodeInfo BackButton = nodeInfoMap.get("Back Button");
                clickButton(BackButton);
            }
        }
    }

    //处理账单
    private void handlerData(Map<String, AccessibilityNodeInfo> nodeInfoMap1) {
        AppDatabase appDatabase = AppDatabase.getInstance(this);
        BillDao dao = appDatabase.billDao();
        List<BillEntity> billEntities = new ArrayList<>();
        for (String key : nodeInfoMap1.keySet()) {
            if (dao.countByText(key) > 0) continue;
            if (!key.contains("+Rp")) continue;
            BillEntity bill = new BillEntity();
            bill.setState(0);
            bill.setText(key);
            bill.setTime(getSystemFormattedTime());
            billEntities.add(bill);
        }
        dao.insert(billEntities);
    }

    /**
     * 使用Android系统提供的格式化工具
     */
    public String getSystemFormattedTime() {
        // 根据系统设置自动适配12/24小时制
        return DateFormat.format("yyyy-MM-dd hh:mm:ss", new Date()).toString();
    }


    //在转换导航页，点击账单按钮
    private void clickBill(Map<String, AccessibilityNodeInfo> nodeInfoMap) {
        if (isTransfer) return;
        if (collectBillResponse != null) return;
        if (isBill) return;
        if (nodeInfoMap.containsKey("Bank\n" +//在转换导航页，点击账单按钮
                "Transfer")) {
            AccessibilityNodeInfo nodeInfo = nodeInfoMap.get("Transaksi");
            if (nodeInfo != null) {
                AccessibilityNodeInfo accessibilityNodeInfo = nodeInfo.getParent();
                AccessibilityNodeInfo accessibilityNodeInfo1 = accessibilityNodeInfo.getChild(1);
                clickButton(accessibilityNodeInfo1);
            }
        }
    }


    //底部导航栏处理
    private void BottomNavigationBar(Map<String, AccessibilityNodeInfo> nodeInfoMap) {
        if (isTransfer) return;
        if (isBill && nodeInfoMap.containsKey("Aktivitas Terakhir")) {//首页特征码
            if (nodeInfoMap.containsKey("Transaksi\n" +
                    "Tab 3 dari 5")) {
                AccessibilityNodeInfo Transaksi = nodeInfoMap.get("Transaksi\n" +
                        "Tab 3 dari 5");
                clickButton(Transaksi);
            }
            isBill = false;
        } else if (!isBill && nodeInfoMap.containsKey("Bank\n" +
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
        }
        isBill = true;
    }

    //屏幕输入密码
    private void ScreenLockPassword(Map<String, AccessibilityNodeInfo> nodeInfoMap) {
        if (!nodeInfoMap.containsKey("GUNAKAN PASSWORD")) return;
        String pass = appConfig.getLockPass();
        Logs.d("输入密码：" + pass);
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
                accessibilityNodeInfo1.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                accessibilityNodeInfo1.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                AccessibleUtil.inputTextByAccessibility(accessibilityNodeInfo1, text);
            }
        }
    }

    @Override
    public void onInterrupt() {

    }

    public class LogWindow {
        private final CollectionAccessibilityService service;
        private final LayoutLogBinding binding;
        private WindowManager windowManager;

        public LogWindow(CollectionAccessibilityService service) {
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
