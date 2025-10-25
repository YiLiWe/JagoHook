package com.xposed.jagohook.runnable;

import android.content.Context;
import android.content.SharedPreferences;

import com.xposed.jagohook.room.AppDatabase;
import com.xposed.jagohook.room.dao.PostCollectionErrorDao;
import com.xposed.jagohook.room.dao.PostPayErrorDao;
import com.xposed.jagohook.room.entity.PostCollectionErrorEntity;
import com.xposed.jagohook.room.entity.PostPayErrorEntity;
import com.xposed.jagohook.server.CollectionAccessibilityService;
import com.xposed.jagohook.server.PayAccessibilityService;
import com.xposed.jagohook.utils.Logs;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PostPayErrorRunnable implements Runnable {
    private final PayAccessibilityService accessibilityService;
    private String collectUrl;

    public PostPayErrorRunnable(PayAccessibilityService accessibilityService) {
        this.accessibilityService = accessibilityService;
        initData();
    }

    public void initData() {
        SharedPreferences sharedPreferences = accessibilityService.getSharedPreferences("info", Context.MODE_PRIVATE);
        collectUrl = sharedPreferences.getString("payUrl", null);
    }

    @Override
    public void run() {
        AppDatabase appDatabase = AppDatabase.getInstance(accessibilityService);
        PostPayErrorDao billDao = appDatabase.postPayErrorDao();
        while (accessibilityService.isRunning()) {
            List<PostPayErrorEntity> billEntities = billDao.queryPageVideo(10, 0);
            for (PostPayErrorEntity billEntity : billEntities) {
                postBill(billDao, billEntity);
            }
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void postBill(PostPayErrorDao billDao, PostPayErrorEntity billEntity) {
        FormBody.Builder requestBody = new FormBody.Builder();
        requestBody.add("state", String.valueOf(billEntity.getState()));
        requestBody.add("paymentTime", billEntity.getPaymentTime());
        requestBody.add("failReason", billEntity.getFailReason());
        requestBody.add("amount", billEntity.getAmount());
        requestBody.add("orderNo", billEntity.getOrderNo());
        Request request = new Request.Builder()
                .post(requestBody.build())
                .url(collectUrl + "app/payoutOrderCallback")
                .build();
        OkHttpClient client = new OkHttpClient();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                billDao.delete(billEntity.getUid());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
