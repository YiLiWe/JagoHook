package com.xposed.jagohook.runnable;

import android.content.Context;
import android.content.SharedPreferences;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xposed.jagohook.runnable.response.CollectBillResponse;
import com.xposed.jagohook.runnable.response.ResultResponse;
import com.xposed.jagohook.server.CollectionAccessibilityService;
import com.xposed.jagohook.server.SuShellService;
import com.xposed.jagohook.utils.Logs;

import java.io.IOException;

import lombok.Getter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Getter
public class CollectionAccessibilityRunnable implements Runnable {
    private final CollectionAccessibilityService service;
    private String cardNumber;
    private String collectUrl;

    public CollectionAccessibilityRunnable(CollectionAccessibilityService suShellService) {
        this.service = suShellService;
        initData();
    }

    public void initData() {
        SharedPreferences sharedPreferences = service.getSharedPreferences("info", Context.MODE_PRIVATE);
        cardNumber = sharedPreferences.getString("cardNumber", null);
        collectUrl = sharedPreferences.getString("collectUrl", null);
    }

    @Override
    public void run() {
        while (service.isRunning()) {
            if (cardNumber == null || collectUrl == null) {
                initData();
                continue;
            }
            if (service.getCollectBillResponse() == null) continue;
            CollectBillResponse collectBillResponse = getCollectBean();
            if (collectBillResponse != null) {
                service.setCollectBillResponse(collectBillResponse);
            }
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private CollectBillResponse getCollectBean() {
        Logs.d("网络请求");
        String getCollectRequest = getCollect();
        ResultResponse response = JSON.to(ResultResponse.class, getCollectRequest);
        if (response != null) {
            if (response.getCode() == 200) {
                if (response.getData() instanceof JSONObject jsonObject) {
                    return jsonObject.to(CollectBillResponse.class);
                }
            }
        }
        return null;
    }

    private String getCollect() {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(String.format("%sgetCollect?cardNumber=%s&balance=%s", collectUrl, cardNumber, service.getBalance()))
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                if (responseBody == null) return null;
                return responseBody.string();
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }
}
