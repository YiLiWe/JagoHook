package com.xposed.jagohook.runnable.respon;


import lombok.Data;

@Data
public class GetCollectRequest {
    //设备卡号
    private String cardNumber;
    //设备余额
    private long balance;
}
