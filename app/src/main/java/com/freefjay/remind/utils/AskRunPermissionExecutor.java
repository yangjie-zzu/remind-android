package com.freefjay.remind.utils;

import static androidx.constraintlayout.motion.widget.MotionScene.TAG;

import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AskRunPermissionExecutor {

    private final ActivityResultLauncher<String[]> activityResultLauncher;

    private CompletableFuture<Map<String, Boolean>> completableFuture;

    public AskRunPermissionExecutor(AppCompatActivity activity) {
        this.completableFuture = new CompletableFuture<>();
        this.activityResultLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    if (this.completableFuture != null) {
                        this.completableFuture.complete(result);
                    }
                });
    }

    public CompletableFuture<Map<String, Boolean>> ask(String[] permissions) {
        this.completableFuture = new CompletableFuture<>();
        this.activityResultLauncher.launch(permissions);
        return completableFuture;
    }

}
