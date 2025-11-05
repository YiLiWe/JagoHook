package com.xposed.jagohook.server;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import com.xposed.jagohook.config.AppConfig;
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
import com.xposed.jagohook.utils.TimeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    private String balance = "0";
    private boolean isRunning = true;
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
        isRunning = true;

        initData();

        billRunnable = new BillRunnable(this);
        logWindow = new LogWindow(this);
        appConfig = new AppConfig(this);

        postCollectionErrorRunnable = new PostCollectionErrorRunnable(this);
        collectionAccessibilityRunnable = new CollectionAccessibilityRunnable(this);

        new Thread(billRunnable).start();
        new Thread(collectionAccessibilityRunnable).start();
        new Thread(postCollectionErrorRunnable).start();

        scrollDown();

        logWindow.printA("2.8代收服务启动成功...");
        handlerAccessibility();
    }

    public synchronized void setCollectBillResponse(CollectBillResponse collectBillResponse) {
        this.collectBillResponse = collectBillResponse;
    }

    public synchronized CollectBillResponse getCollectBillResponse() {
        return collectBillResponse;
    }

    //下拉
    private void scrollDown() {
        if (TimeUtils.isNightToMorning()) {
            handler.postDelayed(this::scrollDown, 10_000);
            return;
        }
        if (collectBillResponse != null) {
            handler.postDelayed(this::scrollDown, 10_000);
        } else {
            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
            List<AccessibilityNodeInfo> accessibilityNodeInfos = new ArrayList<>();
            AccessibleUtil.getAccessibilityNodeInfoS(accessibilityNodeInfos, nodeInfo);
            AccessibilityNodeInfo scrollView = handlerScrollView(accessibilityNodeInfos);
            Map<String, AccessibilityNodeInfo> nodeInfoMap = AccessibleUtil.toContentDescMap(accessibilityNodeInfos);
            if (nodeInfoMap.containsKey("Aktivitas Terakhir")) {//首页下拉
                if (scrollView != null) {
                    AccessibleUtil.performPullDown(this, 300, 1000, 1000);
                    isBill = true;
                    Logs.d("下拉");
                }
            }
            handler.postDelayed(this::scrollDown, 5_000);
        }
    }

    //处理滑动
    private AccessibilityNodeInfo handlerScrollView(List<AccessibilityNodeInfo> accessibilityNodeInfos) {
        for (AccessibilityNodeInfo accessibilityNodeInfo : accessibilityNodeInfos) {
            if (accessibilityNodeInfo.isScrollable()) {
                return accessibilityNodeInfo;
            }
        }
        return null;
    }

    //执行界面点击事件
    private void handlerAccessibility() {
        if (TimeUtils.isNightToMorning()) {
            handler.postDelayed(this::handlerAccessibility, 5_000);
            return;
        }
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            handler.postDelayed(this::handlerAccessibility, 5_000);
            return;
        }
        if (nodeInfo.getChildCount() == 0) {
            handler.postDelayed(this::handlerAccessibility, 5_000);
            return;
        }
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
        AccessibleUtil.getAccessibilityNodeInfoS(nodeInfos, nodeInfo);
        Map<String, AccessibilityNodeInfo> nodeInfoMap = AccessibleUtil.toContentDescMap(nodeInfos);
        CollectBillResponse collectBillResponse1 = getCollectBillResponse();
        try {
            Dialogs(nodeInfoMap);
            ScreenLockPassword(nodeInfoMap);
            getBalance(nodeInfoMap);
            BottomNavigationBar(nodeInfoMap);
            clickBill(nodeInfoMap);
            getBill(nodeInfoMap);
            Transfer(nodeInfoMap, nodeInfo, collectBillResponse1);
        } catch (Throwable e) {
            Logs.d("异常:" + e.getMessage());
            e.printStackTrace();
        }
        handler.postDelayed(this::handlerAccessibility, 5_000);
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
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    }

    //归集成功
    private void success(Map<String, AccessibilityNodeInfo> nodeInfoMap) {
        if (this.collectBillResponse == null) return;
        long id = this.collectBillResponse.getId();
        postCollectStatus(1, "归集成功", id);
        setCollectBillResponse(null);
        isTransfer = false;
        balance = "0";
        logWindow.printA("归集成功");
        Logs.d("归集成功");
        if (nodeInfoMap.containsKey("Selesai")) {
            clickButton(nodeInfoMap.get("Selesai"));
        }
    }

    //归集失败
    private void error(Map<String, AccessibilityNodeInfo> nodeInfoMap, String text) {
        if (this.collectBillResponse == null) return;
        long id = this.collectBillResponse.getId();
        postCollectStatus(2, text, id);
        setCollectBillResponse(null);
        isTransfer = false;
        balance = "0";
        Logs.d("转账失败");
        logWindow.printA("归集失败");
        clickButton(nodeInfoMap.get("Oke "));
    }

    //弹窗直接点击确认
    private void Dialogs(Map<String, AccessibilityNodeInfo> nodeInfoMap) throws IOException {
        if (nodeInfoMap.containsKey("Sesi berakhir")) {//登录失效
            clickButton(nodeInfoMap.get("Oke "));
        }

        if (nodeInfoMap.containsKey("Autentikasi")) {//登录密码
            if (nodeInfoMap.containsKey("password_field")) {
                AccessibilityNodeInfo accessibilityNodeInfo = nodeInfoMap.get("password_field");
                if (accessibilityNodeInfo == null) return;
                AccessibilityNodeInfo accessibilityNodeInfo1 = accessibilityNodeInfo.getChild(0);
                if (accessibilityNodeInfo1 != null) {
                    accessibilityNodeInfo1.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    accessibilityNodeInfo1.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                    AccessibleUtil.inputTextByAccessibility(accessibilityNodeInfo1, appConfig.getPASS());
                    if (getCollectBillResponse() != null) {
                        success(nodeInfoMap);
                    }
                }
            }
            if (nodeInfoMap.containsKey("Konfirmasi ")) {
                clickButton(nodeInfoMap.get("Konfirmasi "));
            }
        }

        if (this.collectBillResponse == null) {
            if (nodeInfoMap.containsKey("Search Text Field")) {
                AccessibilityNodeInfo accessibilityNodeInfo = nodeInfoMap.get("Search Text Field");
                if (accessibilityNodeInfo != null) {
                    AccessibilityNodeInfo nodeInfo = accessibilityNodeInfo.getParent();
                    if (nodeInfo != null) {
                        clickButton(nodeInfo.getChild(3));
                    }
                }
            }
        }

        //关闭弹窗
        if (getCollectBillResponse() == null) {
            if (nodeInfoMap.containsKey("Back Button")) {
                clickButton(nodeInfoMap.get("Back Button"));
            }
        }

        //批量处理转账失败
        for (String error : PayErrors.errors) {
            if (nodeInfoMap.containsKey(error)) {
                error(nodeInfoMap, error);
                throw new IOException(error);
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
                response.close();
            }
        });
    }

    //转账
    private void Transfer(Map<String, AccessibilityNodeInfo> nodeInfoMap, AccessibilityNodeInfo nodeInfo, CollectBillResponse collectBillResponse1) {

        if (getCollectBillResponse() == null) {
            //转账成功
            if (nodeInfoMap.containsKey("Selesai")) {
                clickButton(nodeInfoMap.get("Selesai"));
            }
            //确认
            if (nodeInfoMap.containsKey("Oke")) {
                if (nodeInfoMap.containsKey("Memilih \"Oke\" di perangkat tidak akan membatalkan transaksi kamu. Notifikasi akan kamu terima, setelah uang berhasil dikirim.")) {
                    clickButton(nodeInfoMap.get("Oke"));
                }
            }
            return;
        }

        //点击转账按钮
        if (!isTransfer && nodeInfoMap.containsKey("Bank\n" +
                "Transfer")) {
            clickButton(nodeInfoMap.get("Bank\n" +
                    "Transfer"));
            isTransfer = true;
        }

      /*  //等待转账状态
        if (nodeInfoMap.containsKey("Uang Berhasil Dikirim!")) {
            success(nodeInfoMap);
            return;
        }

        */
        //转账界面
        if (nodeInfoMap.containsKey("Memilih \"Oke\" di perangkat tidak akan membatalkan transaksi kamu. Notifikasi akan kamu terima, setelah uang berhasil dikirim.")) {
            success(nodeInfoMap);
            if (nodeInfoMap.containsKey("Oke")) {
                clickButton(nodeInfoMap.get("Oke"));
            }
            return;
        }


        //选择银行
        if (nodeInfoMap.containsKey("Title Transfer ke Bank")) {
            //输入银行搜索
            initCard(nodeInfoMap, collectBillResponse1.getBank());
        }

        //银行存在
        if (nodeInfoMap.containsKey(collectBillResponse1.getBank() + "\n" +
                "BI-FAST")) {
            clickButton(nodeInfoMap.get(collectBillResponse1.getBank() + "\n" +
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
                    AccessibleUtil.inputTextByAccessibility(accessibilityNodeInfo1, String.valueOf(collectBillResponse1.getIdPlgn()));
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
        if (nodeInfoMap.containsKey("Periksa") && nodeInfoMap.containsKey(collectBillResponse1.getBank())) {
            initCard(nodeInfoMap, collectBillResponse1.getPhone());
        }

        //输入卡号成功后
        Map<String, AccessibilityNodeInfo> nodeInfoMap1 = AccessibleUtil.toTextMap(nodeInfo);
        if (nodeInfoMap.containsKey("Periksa") && nodeInfoMap.containsKey(collectBillResponse1.getBank()) && nodeInfoMap1.containsKey(collectBillResponse1.getPhone())) {
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
            if (key.contains("+Rp") || key.endsWith("Pemasukan")) {
                BillEntity bill = new BillEntity();
                bill.setState(0);
                bill.setText(key);
                bill.setTime(getSystemFormattedTime());
                billEntities.add(bill);
            }
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
        if (getCollectBillResponse() != null) return;
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
}
