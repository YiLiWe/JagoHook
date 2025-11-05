package com.xposed.jagohook.server;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import com.xposed.jagohook.config.AppConfig;
import com.xposed.jagohook.room.AppDatabase;
import com.xposed.jagohook.room.dao.PostPayErrorDao;
import com.xposed.jagohook.room.entity.PostPayErrorEntity;
import com.xposed.jagohook.runnable.PayRunnable;
import com.xposed.jagohook.runnable.PostPayErrorRunnable;
import com.xposed.jagohook.runnable.response.TakeLatestOrderBean;
import com.xposed.jagohook.utils.AccessibleUtil;
import com.xposed.jagohook.utils.Logs;
import com.xposed.jagohook.utils.TimeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    private final Handler handler = new Handler(Looper.getMainLooper());

    // ========== 代付相关 ==========
    private boolean isRunning = true;
    private TakeLatestOrderBean takeLatestOrderBean;
    private PostPayErrorRunnable postPayErrorRunnable;
    private PayRunnable payRunnable;
    private String balance = "0";

    private String orderNo;//执行完毕以后记录避免二次执行

    // ========== ui操作 ==========
    private LogWindow logWindow;
    private boolean isTransfer = false;
    private AccessibilityNodeInfo scrollView;
    // ========== 配置 ==========
    private AppConfig appConfig;


    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;

        appConfig = new AppConfig(this);
        logWindow = new LogWindow(this);

        postPayErrorRunnable = new PostPayErrorRunnable(this);
        payRunnable = new PayRunnable(this);
        new Thread(postPayErrorRunnable).start();
        new Thread(payRunnable).start();

        logWindow.printA("代付运行中");

        scrollDown();

        handlerAccessibility();
    }

    private boolean isBACKWARD = true;

    //下拉
    private void scrollDown() {
        if (TimeUtils.isNightToMorning()) {
            handler.postDelayed(this::scrollDown, 10_000);
            return;
        }
        if (getTakeLatestOrderBean() != null) {
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
                    Logs.d("下拉");
                }
            }
            handler.postDelayed(this::scrollDown, 10_000);
        }
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    }

    public synchronized void setTakeLatestOrderBean(TakeLatestOrderBean takeLatestOrderBean) {
        this.takeLatestOrderBean = takeLatestOrderBean;
    }

    public synchronized TakeLatestOrderBean getTakeLatestOrderBean() {
        return takeLatestOrderBean;
    }

    //执行界面点击事件
    private void handlerAccessibility() {
        if (this.orderNo != null && this.getTakeLatestOrderBean() != null) {
            if (this.orderNo.equals(getTakeLatestOrderBean().getOrderNo())) {
                logWindow.printA("订单号重复异常，停止执行");
                return;
            }
        }
        if (TimeUtils.isNightToMorning()) {
            handler.postDelayed(this::handlerAccessibility, 10_000);
            return;
        }
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            handler.postDelayed(this::handlerAccessibility, 2000);
            return;
        }
        if (nodeInfo.getChildCount() == 0) {
            handler.postDelayed(this::handlerAccessibility, 2000);
            return;
        }
        List<AccessibilityNodeInfo> nodeInfos = new ArrayList<>();
        AccessibleUtil.getAccessibilityNodeInfoS(nodeInfos, nodeInfo);
        Map<String, AccessibilityNodeInfo> nodeInfoMap = AccessibleUtil.toContentDescMap(nodeInfos);
        try {
            Dialogs(nodeInfoMap);
            ScreenLockPassword(nodeInfoMap);
            getBalance(nodeInfoMap);
            BottomNavigationBar(nodeInfoMap);
            Transfer(nodeInfoMap, nodeInfo);
        } catch (Throwable e) {
            Logs.d("异常:" + e.getMessage());
        }
        handler.postDelayed(this::handlerAccessibility, 2000);
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

    //归集成功
    private void success(Map<String, AccessibilityNodeInfo> nodeInfoMap) {
        this.orderNo = getTakeLatestOrderBean().getOrderNo();
        TakeLatestOrderBean id = getTakeLatestOrderBean();
        PullPost(1, "转账成功", id);
        setTakeLatestOrderBean(null);
        isTransfer = false;
        balance = "0";
        logWindow.printA("转账成功");
        Logs.d("转账成功");
        if (nodeInfoMap.containsKey("Selesai")) {
            clickButton(nodeInfoMap.get("Selesai"));
        }
    }

    //归集失败
    private void error(Map<String, AccessibilityNodeInfo> nodeInfoMap, String text) {
        this.orderNo = takeLatestOrderBean.getOrderNo();
        TakeLatestOrderBean id = getTakeLatestOrderBean();
        PullPost(0, text, id);
        setTakeLatestOrderBean(null);
        isTransfer = false;
        balance = "0";
        Logs.d("转账失败");
        logWindow.printA("转账失败");
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
                }
            }
            if (nodeInfoMap.containsKey("Konfirmasi ")) {
                clickButton(nodeInfoMap.get("Konfirmasi "));
            }
        }


        if (getTakeLatestOrderBean() == null) {
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

        //关闭弹窗
        if (getTakeLatestOrderBean()==null){
            if (nodeInfoMap.containsKey("Contact Item")){
                AccessibilityNodeInfo accessibilityNodeInfo = nodeInfoMap.get("Contact Item");
                if (accessibilityNodeInfo != null) {
                    AccessibilityNodeInfo nodeInfo = accessibilityNodeInfo.getParent();
                    if (nodeInfo != null) {
                        AccessibilityNodeInfo nodeInfo1 = nodeInfo.getChild(2);
                        clickButton(nodeInfo1);
                    }
                }
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
        if (getTakeLatestOrderBean() == null) return;

        //点击转账按钮
        if (!getTakeLatestOrderBean().isMoney()) {
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
            success(nodeInfoMap);
        }

        //选择银行
        if (!getTakeLatestOrderBean().isMoney()) {
            if (nodeInfoMap.containsKey("Title Transfer ke Bank")) {
                //输入银行搜索
                initCard(nodeInfoMap, getTakeLatestOrderBean().getBankName());
            }
        }

        //银行存在
        if (!getTakeLatestOrderBean().isMoney()) {
            if (nodeInfoMap.containsKey(getTakeLatestOrderBean().getBankName() + "\n" +
                    "BI-FAST")) {
                clickButton(nodeInfoMap.get(getTakeLatestOrderBean().getBankName() + "\n" +
                        "BI-FAST"));
            }
        } else {//选择钱包银行
            if (nodeInfoMap.containsKey(getTakeLatestOrderBean().getBankName())) {
                clickButton(nodeInfoMap.get(getTakeLatestOrderBean().getBankName()));
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
                    AccessibleUtil.inputTextByAccessibility(accessibilityNodeInfo1, String.valueOf(getTakeLatestOrderBean().getAmount()));
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

        //确认钱包转账
        if (nodeInfoMap.containsKey("Cek Ulang Transaksi") && nodeInfoMap.containsKey("Top Up Sekarang ")) {
            clickButton(nodeInfoMap.get("Top Up Sekarang "));
        }

        //输入银行卡号
        if (nodeInfoMap.containsKey("Periksa") && nodeInfoMap.containsKey(getTakeLatestOrderBean().getBankName())) {
            initCard(nodeInfoMap, getTakeLatestOrderBean().getCardNumber());
        }

        //输入卡号成功后
        Map<String, AccessibilityNodeInfo> nodeInfoMap1 = AccessibleUtil.toTextMap(nodeInfo);
        if (nodeInfoMap.containsKey("Periksa") && nodeInfoMap.containsKey(getTakeLatestOrderBean().getBankName()) && nodeInfoMap1.containsKey(getTakeLatestOrderBean().getCardNumber())) {
            clickButton(nodeInfoMap.get("Periksa"));
        }
    }


    //底部导航栏处理
    private void BottomNavigationBar(Map<String, AccessibilityNodeInfo> nodeInfoMap) {
        if (isTransfer) return;
        if (getTakeLatestOrderBean() != null && nodeInfoMap.containsKey("Aktivitas Terakhir")) {//首页特征码
            if (nodeInfoMap.containsKey("Transaksi\n" +
                    "Tab 3 dari 5")) {
                AccessibilityNodeInfo Transaksi = nodeInfoMap.get("Transaksi\n" +
                        "Tab 3 dari 5");
                clickButton(Transaksi);
            }
        }
        if (getTakeLatestOrderBean() == null && nodeInfoMap.containsKey("Bank\n" +
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
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        logWindow.destroy();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onInterrupt() {

    }
}
