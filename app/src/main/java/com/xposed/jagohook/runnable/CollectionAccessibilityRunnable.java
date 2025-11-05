package com.xposed.jagohook.runnable;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xposed.jagohook.room.AppDatabase;
import com.xposed.jagohook.room.dao.PostCollectionErrorDao;
import com.xposed.jagohook.room.entity.PostCollectionErrorEntity;
import com.xposed.jagohook.runnable.response.CollectBillResponse;
import com.xposed.jagohook.runnable.response.ResultResponse;
import com.xposed.jagohook.server.CollectionAccessibilityService;
import com.xposed.jagohook.utils.BankUtils;
import com.xposed.jagohook.utils.Logs;
import com.xposed.jagohook.utils.TimeUtils;

import java.io.IOException;

import lombok.Getter;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

//获取归集
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
        service.getLogWindow().print("归集账单启动...");

        while (service.isRunning()) {
            if (TimeUtils.isNightToMorning()) {
                stop();
                continue;
            }
            if (cardNumber == null || collectUrl == null) {
                initData();
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
            if (service.getCollectBillResponse() != null) {
                stop();
                continue;
            }
            CollectBillResponse collectBillResponse = getCollectBean();
            if (collectBillResponse != null) {
                if (!BankUtils.getBankMap().containsKey(collectBillResponse.getBank())) {
                    postCollectStatus(2, "不支持该银行", collectBillResponse.getId());
                } else {
                    String bank = BankUtils.getBankMap().get(collectBillResponse.getBank());
                    collectBillResponse.setBank(bank);
                }
                service.setCollectBillResponse(collectBillResponse);
                service.getLogWindow().print("归集账单成功:" + collectBillResponse.getId());
            }
            stop();
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
                AppDatabase appDatabase = AppDatabase.getInstance(service);
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
                    AppDatabase appDatabase = AppDatabase.getInstance(service);
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

    private void stop() {
        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private CollectBillResponse getCollectBean() {
        String getCollectRequest = getCollect();
        if (getCollectRequest == null) return null;
        Logs.d("请求归集结果:" + getCollectRequest);
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
                .url(String.format("%sv1/getCollect?cardNumber=%s&balance=%s", collectUrl, cardNumber, service.getBalance()))
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
