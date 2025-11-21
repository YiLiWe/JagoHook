package com.xposed.jagohook.runnable;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.alibaba.fastjson2.JSON;
import com.xposed.jagohook.room.AppDatabase;
import com.xposed.jagohook.room.dao.PostPayErrorDao;
import com.xposed.jagohook.room.entity.PostPayErrorEntity;
import com.xposed.jagohook.runnable.response.MessageBean;
import com.xposed.jagohook.runnable.response.TakeLatestOrderBean;
import com.xposed.jagohook.server.PayAccessibilityService;
import com.xposed.jagohook.utils.BankUtils;
import com.xposed.jagohook.utils.Logs;
import com.xposed.jagohook.utils.TimeUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.Getter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Getter
public class PayRunnable implements Runnable {
    private final PayAccessibilityService service;
    private String cardNumber;
    private String collectUrl;

    private final List<String> Banks = List.of("Gopay Customer", "OVO", "ShopeePay", "DANA", "LinkAja");

    private final Map<String, String> Banks2 = new HashMap<>() {
        {
            put("OVO", "OVO");
            put("LinkAja", "LinkAja");
            put("ShopeePay", "ShopeePay");
            put("Gopay Customer", "GoPay");
            put("DANA", "DANA");
        }
    };

    public PayRunnable(PayAccessibilityService suShellService) {
        this.service = suShellService;
        initData();
    }

    public void initData() {
        SharedPreferences sharedPreferences = service.getSharedPreferences("info", Context.MODE_PRIVATE);
        cardNumber = sharedPreferences.getString("cardNumber", null);
        collectUrl = sharedPreferences.getString("payUrl", null);
    }

    @Override
    public void run() {
        while (service.isRunning()) {
            if (TimeUtils.isNightToMorning()) {
                stop();
                continue;
            }
            if (service.getTakeLatestOrderBean() != null) {
                stop();
                continue;
            }
            if (cardNumber == null || collectUrl == null) {
                stop();
                continue;
            }
            if (service.getBalance().isEmpty()) {
                stop();
                continue;
            }
            if (service.getBalance().equals("0")) {
                stop();
                continue;
            }

            TakeLatestOrderBean takeLatestOrderBean = getOrder();
            if (takeLatestOrderBean != null) {
                service.getLogWindow().print("获取到订单:" + takeLatestOrderBean.getOrderNo());
                    service.setTakeLatestOrderBean(takeLatestOrderBean);
            }
            stop();
        }
    }

    public TakeLatestOrderBean getTest() {
        TakeLatestOrderBean takeLatestOrderBean = new TakeLatestOrderBean();
        takeLatestOrderBean.setBankName("Bank NTT");
        takeLatestOrderBean.setOrderNo("6666666666");
        takeLatestOrderBean.setCardNumber("66666666");
        takeLatestOrderBean.setAmount(666666);
        return takeLatestOrderBean;
    }

    private void stop() {
        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public TakeLatestOrderBean getOrder() {
        String text = takeLatestPayoutOrder();
        if (text == null) return null;
        if (!text.startsWith("{")) return null;
        MessageBean messageBean = JSON.to(MessageBean.class, text);
        if (messageBean == null) return null;
        if (messageBean.getData() == null) return null;
        TakeLatestOrderBean takeLatestOrderBean1 = messageBean.getData().to(TakeLatestOrderBean.class);
        if (Banks.contains(takeLatestOrderBean1.getBankName())) {
            takeLatestOrderBean1.setMoney(true);
            takeLatestOrderBean1.setBankName(getBank(takeLatestOrderBean1.getBankName()));
        } else {
            takeLatestOrderBean1.setMoney(false);
            if (!BankUtils.getBankMap().containsKey(takeLatestOrderBean1.getBankName())) {
                PullPost(0, "bank name error", takeLatestOrderBean1);
                return null;
            } else {
                String bank = BankUtils.getBankMap().get(takeLatestOrderBean1.getBankName());
                takeLatestOrderBean1.setBankName(bank);
            }
        }
        return takeLatestOrderBean1;
    }

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
                .url(collectUrl + "app/payoutOrderCallback")
                .build();
        OkHttpClient client = new OkHttpClient();
        int finalState = state;
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                AppDatabase appDatabase = AppDatabase.getInstance(service);
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
                if (response.isSuccessful()) {
                    response.close();
                    return;
                }
                AppDatabase appDatabase = AppDatabase.getInstance(service);
                PostPayErrorDao billDao = appDatabase.postPayErrorDao();
                PostPayErrorEntity postCollectionErrorEntity = new PostPayErrorEntity();
                postCollectionErrorEntity.setOrderNo(transferBean.getOrderNo());
                postCollectionErrorEntity.setAmount(String.valueOf(transferBean.getAmount()));
                postCollectionErrorEntity.setState(finalState);
                postCollectionErrorEntity.setFailReason(error);
                postCollectionErrorEntity.setPaymentTime(timeStr);
                billDao.insert(postCollectionErrorEntity);
                response.close();
            }
        });
    }

    private String getBank(String key) {
        String value = Banks2.get(key);
        if (value == null) return key;
        return value;
    }


    //获取代付订单
    public String takeLatestPayoutOrder() {
        RequestBody requestBody = new FormBody.Builder()
                .add("cardNumber", cardNumber)
                .add("balance", service.getBalance())
                .build();
        Request request = new Request.Builder()
                .post(requestBody)
                .url(collectUrl + "app/takeLatestPayoutOrder")
                .build();
        OkHttpClient client = new OkHttpClient();
        try (Response response = client.newCall(request).execute()) {
            try (ResponseBody responseBody = response.body()) {
                if (responseBody != null) {
                    String text = responseBody.string();
                    return text;
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

}
