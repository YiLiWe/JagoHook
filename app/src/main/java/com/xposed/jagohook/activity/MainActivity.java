package com.xposed.jagohook.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
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
        binding.save.setOnClickListener(view -> {
            Editable cardNumberEdit = binding.cardNumber.getText();
            Editable collectUrlEdit = binding.collectUrl.getText();
            Editable payUrlEdit = binding.payUrl.getText();
            if (cardNumberEdit == null || collectUrlEdit == null || payUrlEdit == null) {
                Toast.makeText(MainActivity.this, "请填写完整信息", Toast.LENGTH_SHORT).show();
                return;
            }
            String cardNumber = cardNumberEdit.toString();
            String collectUrl = collectUrlEdit.toString();
            String payUrl = payUrlEdit.toString();
            if (cardNumberEdit.length() <= 0 || collectUrlEdit.length() <= 0 || payUrlEdit.length() <= 0) {
                Toast.makeText(MainActivity.this, "请填写完整信息", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences sharedPreferences = getSharedPreferences("info", Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putString("cardNumber", cardNumber);
            edit.putString("collectUrl", collectUrl);
            edit.putString("payUrl", payUrl);
            edit.apply();

            Toast.makeText(MainActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
        });
        binding.start.setTag(0);
        binding.start.setOnClickListener(view -> {
            if (suShellService == null) {
                Toast.makeText(MainActivity.this, "操作失败，服务未连接,关闭重新打开", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.start.getTag() instanceof Integer tag) {
                SharedPreferences sharedPreferences = getSharedPreferences("info", Context.MODE_PRIVATE);
                String cardNumber = sharedPreferences.getString("cardNumber", null);
                String collectUrl = sharedPreferences.getString("collectUrl", null);
                String payUrl = sharedPreferences.getString("payUrl", null);
                if (cardNumber == null || collectUrl == null || payUrl == null) {
                    Toast.makeText(MainActivity.this, "请先保存信息", Toast.LENGTH_SHORT).show();
                    return;
                }
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
