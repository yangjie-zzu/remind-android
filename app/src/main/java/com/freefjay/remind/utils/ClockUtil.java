package com.freefjay.remind.utils;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.freefjay.remind.MainActivity;
import com.freefjay.remind.R;
import com.freefjay.remind.constant.Constant;
import com.freefjay.remind.service.AlarmService;

import org.slf4j.Logger;

import java.text.ParseException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;

public class ClockUtil {

    private static PendingIntent pendingIntent;

    private static ScheduledFuture<?> scheduledFuture;

    private static Thread thread;

    private static final Logger logger = LogUtil.getLogger(ClockUtil.class);

    public static void startClock(Context context, String cron) {
        CronExpression cronExpression = null;
        try {
            cronExpression = new CronExpression(cron);
        } catch (ParseException e) {
            Toast.makeText(context, "cron 转换异常", Toast.LENGTH_LONG).show();
            return;
        }
        Date alarmDate = cronExpression.getTimeAfter(new Date());
        if (alarmDate == null) {
            Toast.makeText(context, "时间为空", Toast.LENGTH_LONG).show();
            return;
        }
        logger.info("startSchedule: 配置定时任务");
        if (thread != null) {
            logger.info("startSchedule: 取消上一个定时任务");
            thread.interrupt();
        }
        thread = Async.async(() -> {
            logger.info("startSchedule: 1");
            try {
                sleep(alarmDate);
            } catch (InterruptedException e) {
                logger.warn("startClock: ", e);
                return;
            }
            logger.info("startSchedule: 2");
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> Toast.makeText(context, "定时任务", Toast.LENGTH_LONG).show());
            Location location = Async.await(SysUtil.getCurrentLocation(context));
            NotificationUtil.createNotificationChannel(context, "remind", "remind", "remind");
            Intent notifyIntent = new Intent(context, MainActivity.class);
            notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            String vibratorKey = UUID.randomUUID().toString();
            notifyIntent.putExtra("vibratorKey", vibratorKey);
            notifyIntent.setAction("alarmActivity");
            PendingIntent pendingIntent = PendingIntent.getActivity(context, Constant.remindRequestCode, notifyIntent,PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "remind")
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentText("打卡了 [" + cron + "] " + location)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                logger.warn("onReceive: 无法创建通知，权限不足");
                return;
            }
            notificationManagerCompat.notify(Constant.remindNotificationId, builder.build());
            Vibrator vibrator = context.getSystemService(Vibrator.class);
            logger.info("睡眠后10000开启下一次定时任务2");
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    VibrationEffect vibrationEffect = VibrationEffect.createWaveform(new long[]{200, 200}, 1);
                    vibrator.vibrate(vibrationEffect);
                } else {
                    vibrator.vibrate(new long[]{200, 100}, 1);
                }
            }
            logger.info("睡眠后10000开启下一次定时任务1");
            AlarmService.vibratorMap.put(vibratorKey, vibrator);
            logger.info("睡眠后10000开启下一次定时任务");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                logger.info("开启下一次定时任务");
                Async.async(() -> startClock(context, cron));
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, AlarmService.class));
        }
    }

    public static void sleep(Date date) throws InterruptedException {
        long unit = 30 * 1000;
        while (true) {
            long diff = date.getTime() - System.currentTimeMillis();
            if (diff <= 0) {
                break;
            } else {
                logger.info("继续休眠");
                Thread.sleep(Math.min(diff, unit));
            }
        }
    }
}
