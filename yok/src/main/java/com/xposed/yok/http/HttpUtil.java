package com.xposed.yok.http;

import com.alibaba.fastjson.JSONObject;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import lombok.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * @author : yaowenbin
 */
public class HttpUtil {

    static OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .build();

    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    public static String get(String url) {
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();

        return synchronizedCall(request);
    }

    public static void get(String url, Callback callback) {
        Request request = new Request.Builder()
                .get()
                .url(url)
                .build();

        call(request, callback);
    }

    public static <T> T get(String url, Class<T> clz) {
        return parse(get(url), clz);
    }

    public static String post(String url, String json) {
        RequestBody requestBody = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .post(requestBody)
                .url(url)
                .build();

        return synchronizedCall(request);
    }

    public static void post(String url, String json, Callback callback) {
        RequestBody requestBody = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .post(requestBody)
                .url(url)
                .build();

        call(request, callback);
    }

    public static String post(String url, FormBody json) {
        Request request = new Request.Builder()
                .post(json)
                .url(url)
                .build();

        return synchronizedCall(request);
    }

    public static void post(String url, FormBody json, Callback callback) {
        Request request = new Request.Builder()
                .post(json)
                .url(url)
                .build();

        call(request, callback);
    }


    public static <T> T post(String url, String json, Class<T> clz) {
        return parse(post(url, json), clz);
    }


    public static <T> T post(String url, FormBody json, Class<T> clz) {
        return parse(post(url, json), clz);
    }

    private static void call(Request request, Callback call) {
        HTTP_CLIENT.newCall(request).enqueue(call);
    }


    @Nullable
    private static String synchronizedCall(Request request) {
        try (Response response = HTTP_CLIENT.newCall(request).execute()) {
            try (ResponseBody responseBody = response.body()) {
                if (responseBody != null) {
                    return responseBody.string();
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    private static <T> T synchronizedCall(Request request, Class<T> clz) {
        return parse(synchronizedCall(request), clz);
    }

    public static <T> T parse(String json, Class<T> clz) {
        return JSONObject.parseObject(json, clz);
    }
}

