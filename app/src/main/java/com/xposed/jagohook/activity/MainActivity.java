package com.xposed.jagohook.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.xposed.jagohook.activity.base.BaseActivity;
import com.xposed.jagohook.config.AppConfig;
import com.xposed.jagohook.databinding.ActivityMainBinding;
import com.xposed.jagohook.utils.PermissionManager;

public class MainActivity extends BaseActivity<ActivityMainBinding> {

    //  private SuShellService suShellService = null;
    private AppConfig appConfig;
    private PermissionManager permissionManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeComponents();
        initViewClick();
        initData();
        checkPermissions();
    }

    /**
     * 初始化组件
     */
    private void initializeComponents() {
        appConfig = new AppConfig(this);
        permissionManager = new PermissionManager(this);
    }

    /**
     * 初始化数据
     */
    private void initData() {
        binding.collectUrl.setText(appConfig.getCollectUrl());
        binding.cardNumber.setText(appConfig.getCardNumber());
        binding.payUrl.setText(appConfig.getPayUrl());
        binding.lockPass.setText(appConfig.getLockPass());
    }

    /**
     * 检查权限
     */
    private void checkPermissions() {
        permissionManager.requestOverlayPermission(new PermissionManager.PermissionCallback() {
            @Override
            public void onPermissionGranted() {
                //connectService();
            }

            @Override
            public void onPermissionDenied() {
                // 权限被拒绝，可以在这里处理
            }
        });
    }

    /**
     * 初始化视图点击事件
     */
    private void initViewClick() {
        binding.toolbar.setOnMenuItemClickListener(item -> {
            Intent intent = new Intent(MainActivity.this, BillActivity.class);
            startActivity(intent);
            return false;
        });
        binding.save.setOnClickListener(view -> handleSaveClick());
        binding.start.setTag(0);
        binding.start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                jumpToSettingPage(view.getContext());
            }
        });
        // binding.start.setOnClickListener(view -> handleStartClick());
    }


    /**
     * 跳转到无障碍服务设置页面
     * @param context 设备上下文
     */
    public void jumpToSettingPage(Context context) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * 处理保存按钮点击
     */
    private void handleSaveClick() {
        Editable cardNumberEdit = binding.cardNumber.getText();
        Editable collectUrlEdit = binding.collectUrl.getText();
        Editable payUrlEdit = binding.payUrl.getText();
        Editable LockPass = binding.lockPass.getText();

        if (!isInputValid(cardNumberEdit, collectUrlEdit, payUrlEdit, LockPass)) {
            Toast.makeText(MainActivity.this, "请填写完整信息", Toast.LENGTH_SHORT).show();
            return;
        }

        String cardNumber = cardNumberEdit.toString();
        String collectUrl = collectUrlEdit.toString();
        String payUrl = payUrlEdit.toString();
        String lockPass = LockPass.toString();

        appConfig.setAllConfig(cardNumber, collectUrl, payUrl, lockPass);
        Toast.makeText(MainActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
    }

    /**
     * 验证输入是否有效
     */
    private boolean isInputValid(Editable cardNumber, Editable collectUrl, Editable payUrl, Editable lockPass) {
        return cardNumber != null && collectUrl != null && payUrl != null && lockPass != null &&
                cardNumber.length() > 0 && collectUrl.length() > 0 && payUrl.length() > 0 && lockPass.length() > 0;
    }
}
