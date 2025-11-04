package com.xposed.jagohook.server.script;

import java.util.ArrayList;
import java.util.List;

public class PayErrors {
    // ========== 错误信息 ==========
    public static List<String> errors = new ArrayList<>() {
        {
            add("Bank tujuan tidak merespon");
            add("Akun tidak ditemukan");
            add("Saldo kamu tidak mencukupi");//余额不足
            add("Ada yang salah.");
            add("Ups! Koneksi Internet Hilang");
        }
    };
}
