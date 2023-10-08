package com.freefjay.remind.service;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.freefjay.remind.MainActivity;
import com.freefjay.remind.R;
import com.freefjay.remind.constant.Constant;
import com.freefjay.remind.utils.Async;
import com.freefjay.remind.utils.DateUtil;
import com.freefjay.remind.utils.LogUtil;
import com.freefjay.remind.utils.NotificationUtil;
import com.freefjay.remind.utils.SysUtil;

import org.slf4j.Logger;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AlarmService extends Service {

    private static final Logger logger = LogUtil.getLogger(AlarmService.class);

    public static Map<String, Vibrator> vibratorMap = new ConcurrentHashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Notification notification = null;
        NotificationUtil.createNotificationChannel(this, "REMIND_FOREGROUND_SERVICE", "service", "前台服务");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Intent notifyIntent = new Intent(this, MainActivity.class);
            notifyIntent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, Constant.runRequestCode, notifyIntent, PendingIntent.FLAG_IMMUTABLE);
            notification = new Notification.Builder(this, "REMIND_FOREGROUND_SERVICE")
                    .setContentTitle("提醒")
                    .setContentText("正在运行：" + DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"))
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentIntent(pendingIntent)
                    .build();
        }
        this.startForeground(Constant.foregroundNotificationId, notification);
        Async.async(() -> {
            while (true) {
                NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
                Intent notifyIntent = new Intent(this, MainActivity.class);
                notifyIntent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, Constant.runRequestCode, notifyIntent, PendingIntent.FLAG_IMMUTABLE);
                Notification testNotification = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    testNotification = new Notification.Builder(this, "REMIND_FOREGROUND_SERVICE")
                            .setContentTitle("提醒")
                            .setContentText("正在运行：" + DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"))
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentIntent(pendingIntent)
                            .build();
                }
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                assert testNotification != null;
                notificationManagerCompat.notify(Constant.foregroundNotificationId, testNotification);
                try {
                    Thread.sleep(30 * 1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if ("alarm".equals(intent.getAction())) {
                Log.i(BroadcastReceiver.class.getName(), "接收到广播");
                Toast.makeText(this, "服务广播提醒", Toast.LENGTH_LONG).show();
                Async.async(() -> {
                    Location location = Async.await(SysUtil.getCurrentLocation(this));
                    NotificationUtil.createNotificationChannel(this, "remind", "remind", "remind");
                    Intent notifyIntent = new Intent(this, MainActivity.class);
                    notifyIntent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    String vibratorKey = UUID.randomUUID().toString();
                    notifyIntent.putExtra("vibratorKey", vibratorKey);
                    notifyIntent.setAction("alarmActivity");
                    PendingIntent pendingIntent = PendingIntent.getActivity(this, Constant.remindRequestCode, notifyIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "remind")
                            .setSmallIcon(R.drawable.ic_launcher_background)
                            .setContentText("打卡提醒")
                            .setContentText("打卡了" + location)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true);
                    NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        logger.warn("onReceive: 无法创建通知，权限不足");
                        return;
                    }
                    notificationManagerCompat.notify(Constant.remindNotificationId, builder.build());
                    Vibrator vibrator = this.getSystemService(Vibrator.class);
                    if (vibrator.hasVibrator()) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            VibrationEffect vibrationEffect = VibrationEffect.createWaveform(new long[]{200, 200}, 1);
                            vibrator.vibrate(vibrationEffect);
                        } else {
                            vibrator.vibrate(new long[]{200, 100}, 1);
                        }
                    }
                    vibratorMap.put(vibratorKey, vibrator);
                });
//                ClockUtil.startAlarm(this, intent.getStringExtra("cron"));
            }
            if (Constant.stopAlarm.equals(intent.getAction())) {
                logger.info("onStartCommand: 取消振动 " + intent.getExtras());
                String vibratorKey = intent.getStringExtra("vibratorKey");
                if (vibratorKey != null) {
                    Vibrator vibrator = vibratorMap.get(vibratorKey);
                    logger.info("onStartCommand: vibrator " + vibratorMap);
                    if (vibrator != null) {
                        vibrator.cancel();
                        logger.info("onStartCommand: 振动已取消 ");
                        Toast.makeText(this, "取消振动", Toast.LENGTH_LONG).show();
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
