package com.xposed.jagohook.runnable.response;

import com.alibaba.fastjson2.JSONObject;

import lombok.Data;

@Data
public class MessageBean {
    private int code;
    private String msg;
    private JSONObject data;
    private boolean success;
}
