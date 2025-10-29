package com.xposed.jagohook.activity;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.xposed.jagohook.adapter.BillAdapter;
import com.xposed.jagohook.databinding.ActivityBillBinding;
import com.xposed.jagohook.room.AppDatabase;
import com.xposed.jagohook.room.entity.BillEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description 账单界面
 * @Author 不一样的风景
 * @Time 2024/11/2 17:20
 */
public class BillActivity extends AppCompatActivity implements Runnable {
    private ActivityBillBinding binding;
    private int pageSize = 10, pageNumber = 1;
    private final List<BillEntity> beans = new ArrayList<>();
    private BillAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBillBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initRecycler();
        initToolbar();
        initSmart();
        initData();
    }

    /**
     * @Description 初始化列表
     * @code 1
     * @Author 不一样的风景
     * @Time 2024/11/2 17:21
     */
    private void initRecycler() {
        adapter = new BillAdapter(beans);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        binding.recycler.setAdapter(adapter);
        binding.recycler.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    /**
     * @Description 初始化刷新
     * @code 1
     * @Author 不一样的风景
     * @Time 2024/11/2 17:21
     */
    private void initSmart() {
        binding.smart.setOnRefreshListener(this::onRefresh);
        binding.smart.setOnLoadMoreListener(this::onLoadMore);
    }

    /**
     * @Description 加载更多通知
     * @code 1
     * @Author 不一样的风景
     * @Time 2024/11/2 17:21
     */
    private void onLoadMore(RefreshLayout refreshLayout) {
        pageNumber = pageNumber + 1;
        initData();
    }

    /**
     * @Description 提交数据
     * @code 1
     * @Author 不一样的风景
     * @Time 2024/11/2 17:22
     */
    private void initData() {
        new Thread(this).start();
    }

    /**
     * @Description 刷新数据
     * @code 1
     * @Author 不一样的风景
     * @Time 2024/11/2 17:31
     */
    private void onRefresh(RefreshLayout refreshLayout) {
        adapter.notifyItemRangeRemoved(0, beans.size() + 1);
        beans.clear();
        pageSize = 10;
        pageNumber = 1;
        initData();
    }

    /**
     * @Description 初始化标题栏
     * @code 1
     * @Author 不一样的风景
     * @Time 2024/11/2 17:32
     */
    private void initToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            new AlertDialog.Builder(this).setTitle("提示").setMessage("是否清空账单").setNegativeButton("取消", null).setPositiveButton("确定", (dialog, which) -> {
                new Thread(() -> {
                    AppDatabase helper = AppDatabase.getInstance(BillActivity.this);
                    helper.billDao().deleteAll();
                });
                Toast.makeText(BillActivity.this, "已清空", Toast.LENGTH_SHORT).show();
            }).show();
            return false;
        });
    }

    @Override
    public void run() {
        int offset = (pageNumber - 1) * pageSize;
        AppDatabase helper = AppDatabase.getInstance(this);
        List<BillEntity> billEntities = helper.billDao().queryPageVideo(pageSize, offset);
        runOnUiThread(() -> {
            binding.smart.finishLoadMore(0);
            binding.smart.finishRefresh(0);
            if (billEntities.isEmpty()) {
                binding.smart.setEnableLoadMore(false);
            }
            beans.addAll(billEntities);
            adapter.notifyItemRangeChanged(0, beans.size() - 1);
        });
    }
}
