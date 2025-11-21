package com.xposed.jagohook.utils;

import com.alibaba.fastjson.JSONObject;
import okhttp3.*;

import java.io.IOException;

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
    public static <T> T post(String url, String json, Class<T> clz) {
        return parse(post(url, json), clz);
    }

    public static String put(String url, String json) {
        RequestBody body = RequestBody.create(JSON, json);

        Request request = new Request.Builder()
                .url(url)
                .put(body)
                .build();

        return synchronizedCall(request);
    }

    public static String put(String url) {
        return put(url, "");
    }


    public static <T> T put(String url, String json, Class<T> clz) {
        return parse(put(url, json), clz);
    }


    public static String delete(String url, String json) {
        RequestBody body = RequestBody.create(JSON, json);

        Request request = new Request.Builder()
                .url(url)
                .delete(body)
                .build();
        return synchronizedCall(request);
    }

    public static String delete(String url) {
        return delete(url, "");
    }

    public static <T> T delete(String url, Class<T> clz) {
        return parse(
                delete(url, ""),
                clz
        );
    }

    public static <T> T delete(String url, String json, Class<T> clz) {
        return parse(delete(url, json), clz);
    }

    private static String synchronizedCall(Request request) {
        try ( Response response = HTTP_CLIENT.newCall(request).execute() ){
            return response.body().toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static<T> T synchronizedCall(Request request, Class<T> clz) {
        return parse(synchronizedCall(request), clz);
    }

    public static <T> T parse(String json, Class<T> clz) {
        return JSONObject.parseObject(json, clz);
    }
}

