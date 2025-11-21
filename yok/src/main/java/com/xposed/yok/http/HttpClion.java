package com.xposed.yok.http;

import android.os.Handler;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public record HttpClion<T>(Builder<T> builder) {

    public T synchronizedCallPost() {
        if (builder.formBody == null) {
            return HttpUtil.post(builder.url, builder.post, builder.clz);
        } else {
            return HttpUtil.post(builder.url, builder.formBody, builder.clz);
        }
    }

    public T synchronizedCallGet() {
        return HttpUtil.get(builder.url, builder.clz);
    }

    public void post() {
        if (builder.formBody == null) {
            HttpUtil.post(builder.url, builder.post, callback());
        } else {
            HttpUtil.post(builder.url, builder.formBody, callback());
        }
    }

    private Callback callback() {
        return new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (builder.restCallback != null) {
                    if (builder.handler != null) {
                        builder.handler.post(() -> builder.restCallback.onFailure(call, e));
                    } else {
                        builder.restCallback.onFailure(call, e);
                    }
                }
                call.clone();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (builder.restCallback != null) {
                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody != null) {
                            String text = responseBody.string();
                            T t = HttpUtil.parse(text, builder.clz);
                            if (builder.handler != null) {
                                builder.handler.post(() -> builder.restCallback.onResponse(t));
                            } else {
                                builder.restCallback.onResponse(t);
                            }
                        }
                    }
                }
                response.close();
                call.clone();
            }
        };
    }


    public void get() {
        HttpUtil.get(builder.url, callback());
    }

    public static class Builder<T> {
        private String url;
        private String post;
        private FormBody formBody;
        private Class<T> clz;
        private Handler handler;
        private RestCallback<T> restCallback;


        public Builder<T> url(String url, Class<T> clz) {
            this.url = url;
            this.clz = clz;
            return this;
        }

        public Builder<T> setFormBody(FormBody formBody) {
            this.formBody = formBody;
            return this;
        }

        public Builder<T> setRestCallback(RestCallback<T> restCallback) {
            this.restCallback = restCallback;
            return this;
        }

        public Builder<T> setPost(String post) {
            this.post = post;
            return this;
        }

        public Builder<T> setHandler(Handler handler) {
            this.handler = handler;
            return this;
        }

        public HttpClion<T> build() {
            return new HttpClion<>(this);
        }
    }


    public interface RestCallback<T> {
        default void onFailure(@NonNull Call call, @NonNull IOException e) {

        }

        void onResponse(T response);
    }
}
