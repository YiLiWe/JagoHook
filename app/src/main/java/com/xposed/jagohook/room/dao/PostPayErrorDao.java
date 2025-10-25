package com.xposed.jagohook.room.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.xposed.jagohook.room.entity.PostCollectionErrorEntity;
import com.xposed.jagohook.room.entity.PostPayErrorEntity;

import java.util.List;

@Dao
public interface PostPayErrorDao {
    @Insert
    long insert(PostPayErrorEntity postPayErrorEntity);

    @Query("DELETE FROM post_pay_error WHERE uid = :id")
    void delete(long id);

    @Query("SELECT * FROM post_pay_error ORDER BY uid DESC LIMIT :limit OFFSET :offset ")
    List<PostPayErrorEntity> queryPageVideo(int limit, int offset);
}
