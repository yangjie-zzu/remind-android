package com.freefjay.remind.utils;

import static androidx.constraintlayout.motion.widget.MotionScene.TAG;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.CompletableFuture;

public class DialogUtil {

    public static CompletableFuture<Boolean> showDialog(Context context, String title, String msg, String okText) {
        Handler handler = new Handler(Looper.getMainLooper());
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        handler.post(() -> {
            Log.i(TAG, "showDialog: 弹窗1");
            Log.i(TAG, "showDialog: 弹窗2");
            AlertDialog alertDialog = new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(msg)
                    .setPositiveButton(okText, ((dialog, which) -> {
                        completableFuture.complete(true);
                    }))
                    .setOnCancelListener((dialogInterface) -> {
                        completableFuture.complete(false);
                    }).show();
            Log.i(TAG, "showDialog: 弹窗3");
        });
        return completableFuture;
    }

}
