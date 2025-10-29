package com.xposed.jagohook.runnable;


import android.content.Context;
import android.content.SharedPreferences;

import com.xposed.jagohook.room.AppDatabase;
import com.xposed.jagohook.room.dao.BillDao;
import com.xposed.jagohook.room.dao.PostCollectionErrorDao;
import com.xposed.jagohook.room.entity.BillEntity;
import com.xposed.jagohook.room.entity.PostCollectionErrorEntity;
import com.xposed.jagohook.server.CollectionAccessibilityService;

import java.io.IOException;
import java.util.List;

import lombok.Getter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Getter
public class PostCollectionErrorRunnable implements Runnable {
    private final CollectionAccessibilityService accessibilityService;
    private String cardNumber;
    private String collectUrl;

    public PostCollectionErrorRunnable(CollectionAccessibilityService accessibilityService) {
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
        while (accessibilityService.isRunning()) {
            PostCollectionErrorDao billDao = appDatabase.postCollectionErrorDao();
            List<PostCollectionErrorEntity> billEntities = billDao.queryPageVideo(10, 0);
            for (PostCollectionErrorEntity billEntity : billEntities) {
                postBill(billDao, billEntity);
            }
            try {
                Thread.sleep(10_000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void postBill(PostCollectionErrorDao billDao, PostCollectionErrorEntity billEntity) {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(String.format("%sv1/collectStatus?id=%s&state=%s&error=%s", collectUrl, billEntity.getId(), billEntity.getState(), billEntity.getError()))
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (response.isSuccessful()) {
                billDao.delete(billEntity.getUid());
            }
        } catch (IOException e) {
        }
    }
}
