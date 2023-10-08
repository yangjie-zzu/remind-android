package com.freefjay.remind.utils;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;

import com.freefjay.remind.MyApplication;

public class NotificationUtil {

    public static void createNotificationChannel(Context context, String id, String name, String description) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public static void checkPermissions(Activity activity) {
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(MyApplication.getInstance());
        if (notificationManagerCompat.areNotificationsEnabled()) {
            return;
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

}
