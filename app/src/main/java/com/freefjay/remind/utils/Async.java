package com.freefjay.remind.utils;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Async {

    public static <T> T await(CompletableFuture<T> completableFuture) {
        try {
            return completableFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public static Thread async(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.start();
        return thread;
    }

}
