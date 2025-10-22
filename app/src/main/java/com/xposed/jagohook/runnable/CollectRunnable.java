package com.xposed.jagohook.runnable;

import android.content.Context;
import android.content.SharedPreferences;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xposed.jagohook.runnable.response.CollectBillResponse;
import com.xposed.jagohook.runnable.response.ResultResponse;
import com.xposed.jagohook.server.SuShellService;

import java.io.IOException;

import lombok.Getter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Getter
public class CollectRunnable implements Runnable {
    private final SuShellService suShellService;
    private final String cardNumber;
    private final String collectUrl;

    public CollectRunnable(SuShellService suShellService) {
        this.suShellService = suShellService;
        SharedPreferences sharedPreferences = suShellService.getSharedPreferences("info", Context.MODE_PRIVATE);
        cardNumber = sharedPreferences.getString("cardNumber", null);
        collectUrl = sharedPreferences.getString("collectUrl", null);
    }

    @Override
    public void run() {
        while (suShellService.isRunning()) {
            if (suShellService.getCollectBillResponse() == null) continue;
            String getCollectRequest = getCollect();
            ResultResponse response = JSON.to(ResultResponse.class, getCollectRequest);
            if (response == null) continue;
            if (response.getCode() != 200) continue;
            if (response.getData() instanceof JSONObject jsonObject) {
                CollectBillResponse collectBillResponse = jsonObject.to(CollectBillResponse.class);
                if (collectBillResponse == null) continue;
                suShellService.setCollectBillResponse(collectBillResponse);
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            CollectBillResponse collectBillResponse = new CollectBillResponse();
            collectBillResponse.setCardNumber("66666");
            collectBillResponse.setBank("BRI");
            suShellService.setCollectBillResponse(collectBillResponse);
        }
    }

    private String getCollect() {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(String.format("%sgetCollect?cardNumber=%s&balance=%s", collectUrl, cardNumber, suShellService.getBalance()))
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
