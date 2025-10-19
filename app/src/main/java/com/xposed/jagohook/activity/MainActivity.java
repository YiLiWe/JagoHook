package com.xposed.jagohook.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.permission.PermissionLists;
import com.xposed.jagohook.activity.base.BaseActivity;
import com.xposed.jagohook.databinding.ActivityMainBinding;
import com.xposed.jagohook.server.SuShellService;


public class MainActivity extends BaseActivity<ActivityMainBinding> implements ServiceConnection {
    private SuShellService suShellService = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initXXPermissions();
        initViewClick();
    }

    private void initXXPermissions() {
        if (XXPermissions.isGrantedPermission(this, PermissionLists.getSystemAlertWindowPermission())) {
            connectService();
        } else {
            XXPermissions.with(MainActivity.this)
                    .permission(PermissionLists.getSystemAlertWindowPermission())
                    .request((grantedList, deniedList) -> {
                        if (!grantedList.isEmpty()) {
                            connectService();
                        } else {
                            Toast.makeText(MainActivity.this, "请授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    public void connectService() {
        Intent intent = new Intent(this, SuShellService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    private void initViewClick() {
        binding.start.setTag(0);
        binding.start.setOnClickListener(view -> {
            if (suShellService == null) {
                Toast.makeText(MainActivity.this, "操作失败，服务未连接,关闭重新打开", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.start.getTag() instanceof Integer tag) {
                if (tag == 0) {
                    binding.start.setText("关闭服务");
                    binding.start.setTag(1);
                    suShellService.start();
                } else {
                    binding.start.setText("开启服务");
                    binding.start.setTag(0);
                    suShellService.stop();
                }
            }
        });
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        SuShellService.ScreenRecordBinder binder = (SuShellService.ScreenRecordBinder) iBinder;
        suShellService = binder.getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        suShellService = null;
        binding.start.setText("开启服务");
        binding.start.setTag(1);
    }
}
