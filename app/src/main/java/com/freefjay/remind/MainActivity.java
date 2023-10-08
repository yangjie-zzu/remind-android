package com.freefjay.remind;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static androidx.constraintlayout.motion.widget.MotionScene.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.freefjay.remind.constant.Constant;
import com.freefjay.remind.service.AlarmService;
import com.freefjay.remind.utils.AskRunPermissionExecutor;
import com.freefjay.remind.utils.Async;
import com.freefjay.remind.utils.BeanUtil;
import com.freefjay.remind.utils.ClockUtil;
import com.freefjay.remind.utils.CronExpression;
import com.freefjay.remind.utils.DateUtil;
import com.freefjay.remind.utils.DialogUtil;
import com.freefjay.remind.utils.EditTextUtil;
import com.freefjay.remind.utils.LogUtil;
import com.freefjay.remind.utils.NotificationUtil;
import com.freefjay.remind.utils.SysUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final Logger logger = LogUtil.getLogger(MainActivity.class);

    EditText editText;

    TextView textView;

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        logger.info("日志测试");
        super.onCreate(savedInstanceState);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        Button button = new Button(this);
        button.setText("开始提醒");
        button.setOnClickListener(v -> {
            String cron = this.editText.getText().toString();
            this.saveClock(cron);
            NotificationUtil.checkPermissions(this);
            ClockUtil.startClock(this, cron);
        });
        Button notKillButton = new Button(this);
        notKillButton.setText("锁定检查");
        notKillButton.setOnClickListener(v -> {
            startForegroundService(new Intent(this, AlarmService.class));
        });
        this.editText = new EditText(this);
        this.editText.setInputType(InputType.TYPE_CLASS_TEXT);
        this.editText.setHint("cron");
        this.editText.setGravity(Gravity.CENTER);
        this.editText.setKeyListener(DigitsKeyListener.getInstance("1234567890*? "));
        this.editText.addTextChangedListener(EditTextUtil.onChange(s -> this.updateText(s.toString())));
        this.textView = new TextView(this);
        linearLayout.addView(this.editText);
        linearLayout.addView(this.textView);
        linearLayout.addView(button);
        linearLayout.addView(notKillButton);
        linearLayout.addView(BeanUtil.create(() -> new Button(this), newButton -> {
            newButton.setText("打开钉钉");
            newButton.setOnClickListener(v -> {
                this.openApp("com.alibaba.android.rimet");
            });
        }));
        this.setContentView(linearLayout);
        PackageManager packageManager = this.getPackageManager();
        AskRunPermissionExecutor askRunPermissionExecutor = new AskRunPermissionExecutor(this);
        Async.async(() -> {
            if (!SysUtil.hasPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                if (!SysUtil.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Map<String, Boolean> result = Async.await(askRunPermissionExecutor.ask(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }));
                    if (!Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))) {
                        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                            if (Async.await(DialogUtil.showDialog(this, "权限说明", "需要位置权限", "去设置"))) {
                                result = Async.await(askRunPermissionExecutor.ask(new String[]{
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                }));
                            }
                        }
                    }
                }
                if (SysUtil.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Map<String, Boolean> result = Async.await(askRunPermissionExecutor.ask(new String[] {
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    }));
                    if (!Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_BACKGROUND_LOCATION))) {
                        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                if (Async.await(DialogUtil.showDialog(this, "权限说明", "为了根据位置决定是否提醒，设置位置权限时，请选择 " + packageManager.getBackgroundPermissionOptionLabel(), "去设置"))) {
                                    result = Async.await(askRunPermissionExecutor.ask(new String[]{
                                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                    }));
                                }
                            } else {
                                if (Async.await(DialogUtil.showDialog(this, "权限说明", "为了根据位置决定是否提醒，设置位置权限时，请允许后台访问位置服务", "去设置"))) {
                                    result = Async.await(askRunPermissionExecutor.ask(new String[]{
                                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                    }));
                                }
                            }
                        }
                    }
                }
            }
            String cron = this.getAlarm();
            this.editText.setText(cron);
            this.updateText(cron);
            if (cron != null) {
                this.updateText(cron);
                ClockUtil.startClock(this, cron);
            }
        });
        Intent intent = this.getIntent();
        if (intent != null && "alarmActivity".equals(intent.getAction())) {
            logger.info("onCreate: alarmActivity vibratorKey" + intent.getStringExtra("vibratorKey"));
            Intent stopAlarmIntent = new Intent(this, AlarmService.class);
            stopAlarmIntent.setAction(Constant.stopAlarm);
            stopAlarmIntent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            stopAlarmIntent.putExtra("vibratorKey", intent.getStringExtra("vibratorKey"));
            this.startService(stopAlarmIntent);
        }
    }

    public void saveClock(String cron) {
        SharedPreferences sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("clockIn", cron);
        editor.apply();
    }

    public String getAlarm() {
        SharedPreferences sharedPreferences = this.getPreferences(Context.MODE_PRIVATE);
        return sharedPreferences.getString("clockIn", null);
    }

    @SuppressLint("SetTextI18n")
    public void updateText(String cron) {
        try {
            CronExpression cronExpression = new CronExpression(cron);
            this.textView.setText("下一次提醒时间：" + DateUtil.format(cronExpression.getTimeAfter(new Date()), "yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            new Handler(Looper.getMainLooper()).post(() -> {
                this.textView.setText("转换错误: " + e);
            });
        }
    }

    public void openApp(String packageName) {
        PackageManager packageManager = this.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
            resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            resolveIntent.setPackage(packageName);
            List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(resolveIntent, 0);
            if (resolveInfoList.size() > 0) {
                ResolveInfo resolveInfo = resolveInfoList.get(0);
                String className = resolveInfo.activityInfo.name;
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                intent.setClassName(packageName, className);
                this.startActivity(intent);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "openApp: ", e);
            Toast.makeText(this, "未安装该应用", Toast.LENGTH_LONG).show();
        }
    }

}
