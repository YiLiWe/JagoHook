package com.xposed.jagohook.room;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.xposed.jagohook.room.dao.BillDao;
import com.xposed.jagohook.room.dao.PostCollectionErrorDao;
import com.xposed.jagohook.room.entity.BillEntity;
import com.xposed.jagohook.room.entity.PostCollectionErrorEntity;

@Database(entities = {BillEntity.class, PostCollectionErrorEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase mAppDatabase;

    public static AppDatabase getInstance(Context context) {
        if (mAppDatabase == null) {
            synchronized (AppDatabase.class) {
                if (mAppDatabase == null) {
                    mAppDatabase = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "Bill.db")
                            .addMigrations()
                            // 默认不允许在主线程中连接数据库
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return mAppDatabase;
    }

    public abstract BillDao billDao();

    public abstract PostCollectionErrorDao postCollectionErrorDao();
}