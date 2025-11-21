package com.xposed.yok.activity;

import android.os.Handler;
import android.os.Looper;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.xposed.yok.activity.adapter.OrderAdapter;
import com.xposed.yok.activity.base.BaseActivity;

public abstract class OrderActivity<T extends ViewBinding, B extends OrderAdapter> extends BaseActivity<T> {
    public final Handler handler = new Handler(Looper.getMainLooper());
    public int current = 1;//位置 需要-1
    public final int pageSize = 20;//数量
    private B adapter;

    public void initRecycler(RecyclerView recyclerView) {
        adapter = getAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    public void initSmart(SmartRefreshLayout smart) {
        smart.setOnRefreshListener(refreshLayout -> {
            current = 1;
            initData();
        });
        smart.setOnLoadMoreListener(refreshLayout -> {
            current++;
            initData();
        });
    }


    public abstract void initData();

    public abstract B getAdapter();

}
