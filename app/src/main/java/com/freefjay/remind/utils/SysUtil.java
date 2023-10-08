package com.freefjay.remind.utils;

import static androidx.constraintlayout.motion.utils.Oscillator.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.concurrent.CompletableFuture;

public class SysUtil {

    public static boolean hasPermission(Context context, String permission) {
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }
    @SuppressLint("MissingPermission")
    public static CompletableFuture<Location> getCurrentLocation(Context context) {
        CompletableFuture<Location> completableFuture = new CompletableFuture<>();
        LocationManager locationManager = context.getSystemService(LocationManager.class);
        Ref<LocationListener> locationListenerRef = new Ref<>();
        locationListenerRef.value = location -> {
            Log.i(TAG, "getCurrentLocation: 接收到定位" + location);
            completableFuture.complete(location);
            locationManager.removeUpdates(locationListenerRef.value);
        };
        new Handler(Looper.getMainLooper()).post(() -> locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, locationListenerRef.value));
        return completableFuture;
    }
}
