package com.xposed.jagohook.runnable;


import android.content.Context;
import android.content.SharedPreferences;

import com.xposed.jagohook.room.AppDatabase;
import com.xposed.jagohook.room.dao.BillDao;
import com.xposed.jagohook.room.entity.BillEntity;
import com.xposed.jagohook.server.CollectionAccessibilityService;
import com.xposed.jagohook.utils.Logs;

import java.io.IOException;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

//上传代收
public class BillRunnable implements Runnable {
    private final CollectionAccessibilityService accessibilityService;
    private String cardNumber;
    private String collectUrl;

    public BillRunnable(CollectionAccessibilityService accessibilityService) {
        this.accessibilityService = accessibilityService;
        initData();
    }

    public void initData() {
        SharedPreferences sharedPreferences = accessibilityService.getSharedPreferences("info", Context.MODE_PRIVATE);
        cardNumber = sharedPreferences.getString("cardNumber", null);
        collectUrl = sharedPreferences.getString("collectUrl", null);
    }

    @Override
    public void run() {
        AppDatabase appDatabase = AppDatabase.getInstance(accessibilityService);
        BillDao billDao = appDatabase.billDao();
        while (accessibilityService.isRunning()) {
            List<BillEntity> billEntities = billDao.queryByState(10, 0, 0);
            for (BillEntity billEntity : billEntities) {
                postBill(billDao, billEntity);
            }
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void postBill(BillDao billDao, BillEntity billEntity) {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(String.format("%sv1/harvest?cardNumber=%s&text=%s", collectUrl, cardNumber, billEntity.getText()))
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                billDao.updateStateById(billEntity.getUid(), 1);
            }
            try (ResponseBody body = response.body()) {
                if (body == null) return;
                String result = body.string();
                Logs.d(result);
            }
        } catch (IOException e) {
            billDao.updateStateById(billEntity.getUid(), 0);
        }
    }
}
