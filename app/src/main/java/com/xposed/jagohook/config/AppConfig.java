package com.xposed.jagohook.config;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 应用配置管理类
 * 统一管理所有配置项的读写操作
 */
public class AppConfig {
    
    private static final String PREF_NAME = "info";
    private static final String KEY_CARD_NUMBER = "cardNumber";
    private static final String KEY_COLLECT_URL = "collectUrl";
    private static final String KEY_PAY_URL = "payUrl";
    
    private final SharedPreferences sharedPreferences;
    
    public AppConfig(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    // ========== Getter方法 ==========
    
    public String getCardNumber() {
        return sharedPreferences.getString(KEY_CARD_NUMBER, "");
    }
    
    public String getCollectUrl() {
        return sharedPreferences.getString(KEY_COLLECT_URL, "");
    }
    
    public String getPayUrl() {
        return sharedPreferences.getString(KEY_PAY_URL, "");
    }
    
    // ========== Setter方法 ==========
    
    public void setCardNumber(String cardNumber) {
        sharedPreferences.edit().putString(KEY_CARD_NUMBER, cardNumber).apply();
    }
    
    public void setCollectUrl(String collectUrl) {
        sharedPreferences.edit().putString(KEY_COLLECT_URL, collectUrl).apply();
    }
    
    public void setPayUrl(String payUrl) {
        sharedPreferences.edit().putString(KEY_PAY_URL, payUrl).apply();
    }
    
    // ========== 验证方法 ==========
    
    public boolean isConfigValid() {
        String cardNumber = getCardNumber();
        String collectUrl = getCollectUrl();
        String payUrl = getPayUrl();
        
        return cardNumber != null && !cardNumber.isEmpty() &&
               collectUrl != null && !collectUrl.isEmpty() &&
               payUrl != null && !payUrl.isEmpty();
    }
    
    // ========== 批量设置方法 ==========
    
    public void setAllConfig(String cardNumber, String collectUrl, String payUrl) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CARD_NUMBER, cardNumber);
        editor.putString(KEY_COLLECT_URL, collectUrl);
        editor.putString(KEY_PAY_URL, payUrl);
        editor.apply();
    }
}