package com.xposed.jagohook.runnable;

import android.content.Context;
import android.content.SharedPreferences;

import com.alibaba.fastjson2.JSON;
import com.xposed.jagohook.runnable.response.MessageBean;
import com.xposed.jagohook.runnable.response.TakeLatestOrderBean;
import com.xposed.jagohook.server.PayAccessibilityService;
import com.xposed.jagohook.utils.Logs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
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
    private final List<String> Banks = List.of("GOPAY", "OVO", "SHOPEEPAY", "DANA", "LINKAJA");
    private final Map<String, String> Banks2 = new HashMap<>() {
        {
            put("OVO", "OVO");
            put("LinkAja", "LINKAJA");
            put("ShopeePay", "SHOPEEPAY");
            put("Gopay Customer", "GOPAY");
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
            if (service.getTakeLatestOrderBean() != null) continue;
            if (cardNumber == null || collectUrl == null) continue;
            TakeLatestOrderBean takeLatestOrderBean = getOrder();
            if (takeLatestOrderBean != null) {
                service.getLogWindow().print("获取到订单:" + takeLatestOrderBean.getOrderNo());
                service.setTakeLatestOrderBean(takeLatestOrderBean);
            }
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public TakeLatestOrderBean getOrder() {
        String text = takeLatestPayoutOrder();
        if (text == null) return null;
        Logs.d("代付订单:" + text);
        MessageBean messageBean = JSON.to(MessageBean.class, text);
        if (messageBean == null) return null;
        if (messageBean.getData() == null) return null;
        TakeLatestOrderBean takeLatestOrderBean1 = messageBean.getData().to(TakeLatestOrderBean.class);
        takeLatestOrderBean1.setBankName(getBank(takeLatestOrderBean1.getBankName()));
        if (Banks.contains(takeLatestOrderBean1.getBankName())) {
            takeLatestOrderBean1.setMoney(true);
        }
        return takeLatestOrderBean1;
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
            ResponseBody responseBody = response.body();
            if (responseBody != null) return responseBody.string();
        } catch (Exception e) {
            return null;
        }
        return null;
    }

}
