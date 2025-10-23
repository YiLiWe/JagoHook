package com.xposed.jagohook.server;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.xposed.jagohook.config.AppConfig;
import com.xposed.jagohook.runnable.CollectionAccessibilityRunnable;
import com.xposed.jagohook.runnable.response.CollectBillResponse;
import com.xposed.jagohook.utils.AccessibleUtil;
import com.xposed.jagohook.utils.Logs;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CollectionAccessibilityService extends AccessibilityService {

    // ========== 归集相关变量 ==========
    private CollectionAccessibilityRunnable collectionAccessibilityRunnable;
    private CollectBillResponse collectBillResponse;
    private String balance = "0";
    private boolean isRunning = false;

    // ========== ui相关变量 ==========
    private boolean isBill = true;
    //转账中，不点击转账按钮
    private boolean isTransfer = false;
    private AppConfig appConfig;

    @Override
    public void onCreate() {
        super.onCreate();
        appConfig = new AppConfig(this);
        collectionAccessibilityRunnable = new CollectionAccessibilityRunnable(this);
        new Thread(collectionAccessibilityRunnable).start();
        isRunning = true;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        AccessibilityNodeInfo nodeInfo = accessibilityEvent.getSource();
        Map<String, AccessibilityNodeInfo> nodeInfoMap = AccessibleUtil.toContentDescMap(nodeInfo);
        ScreenLockPassword(nodeInfoMap);
        getBalance(nodeInfoMap);
        BottomNavigationBar(nodeInfoMap);
        clickBill(nodeInfoMap);
        getBill(nodeInfoMap);
        Transfer(nodeInfoMap, nodeInfo);
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
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    //获取账单
    private void getBill(Map<String, AccessibilityNodeInfo> nodeInfoMap) {
        if (nodeInfoMap.containsKey("Aktivitas Semua Kantong")) {
            Map<String, AccessibilityNodeInfo> nodeInfoMap1 = AccessibleUtil.toStateContentDescMapX(nodeInfoMap, "Transaction Item");
            Logs.d("获取账单总数:" + nodeInfoMap1.size());
            if (nodeInfoMap.containsKey("Back Button")) {
                AccessibilityNodeInfo BackButton = nodeInfoMap.get("Back Button");
                clickButton(BackButton);
            }
        }
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
        this.balance = numbersOnly;
        Logs.d("余额：" + numbersOnly);
        isBill = true;
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
    public void onInterrupt() {

    }
}
