package com.xposed.jagohook.storage;

import lombok.Data;

@Data
public class DataStorage {
    private boolean isHome = false;
    private static DataStorage instance;

    public static DataStorage getInstance() {
        if (instance == null) {
            instance = new DataStorage();
        }
        return instance;
    }
}
