package com.xposed.jagohook.activity;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.xposed.jagohook.activity.base.BaseActivity;
import com.xposed.jagohook.config.AppConfig;
import com.xposed.jagohook.databinding.ActivityMainBinding;
import com.xposed.jagohook.server.SuShellService;
import com.xposed.jagohook.utils.PermissionManager;

public class MainActivity extends BaseActivity<ActivityMainBinding> implements ServiceConnection {
    
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

    public void connectService() {
        Intent intent = new Intent(this, SuShellService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    /**
     * 初始化视图点击事件
     */
    private void initViewClick() {
        binding.kill.setOnClickListener(view -> System.exit(0));
        binding.save.setOnClickListener(view -> handleSaveClick());
        binding.start.setTag(0);
       // binding.start.setOnClickListener(view -> handleStartClick());
    }

    /**
     * 处理保存按钮点击
     */
    private void handleSaveClick() {
        Editable cardNumberEdit = binding.cardNumber.getText();
        Editable collectUrlEdit = binding.collectUrl.getText();
        Editable payUrlEdit = binding.payUrl.getText();
        
        if (!isInputValid(cardNumberEdit, collectUrlEdit, payUrlEdit)) {
            Toast.makeText(MainActivity.this, "请填写完整信息", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String cardNumber = cardNumberEdit.toString();
        String collectUrl = collectUrlEdit.toString();
        String payUrl = payUrlEdit.toString();
        
        appConfig.setAllConfig(cardNumber, collectUrl, payUrl);
        Toast.makeText(MainActivity.this, "保存成功", Toast.LENGTH_SHORT).show();
    }

    /**
     * 处理启动按钮点击
     */
    /*private void handleStartClick() {
        if (suShellService == null) {
            Toast.makeText(MainActivity.this, "操作失败，服务未连接,关闭重新打开", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (binding.start.getTag() instanceof Integer tag) {
            if (!appConfig.isConfigValid()) {
                Toast.makeText(MainActivity.this, "请先保存信息", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (tag == 0) {
                startService();
            } else {
                stopService();
            }
        }
    }*/

    /**
     * 验证输入是否有效
     */
    private boolean isInputValid(Editable cardNumber, Editable collectUrl, Editable payUrl) {
        return cardNumber != null && collectUrl != null && payUrl != null &&
               cardNumber.length() > 0 && collectUrl.length() > 0 && payUrl.length() > 0;
    }

    /**
     * 启动服务
     */
    private void startService() {
        binding.start.setText("关闭服务");
        binding.start.setTag(1);
       // suShellService.start();
    }

    /**
     * 停止服务
     */
    private void stopService() {
        binding.start.setText("开启服务");
        binding.start.setTag(0);
       // suShellService.stop();
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        SuShellService.ScreenRecordBinder binder = (SuShellService.ScreenRecordBinder) iBinder;
       // suShellService = binder.getService();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      //  suShellService = null;
        binding.start.setText("开启服务");
        binding.start.setTag(1);
    }
}
