package com.asb.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;
import androidx.work.impl.utils.ForceStopRunnable;

@SuppressLint("RestrictedApi")
public class RestartReceiver extends ForceStopRunnable.BroadcastReceiver {
    @SuppressLint("RestrictedApi")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (
                intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                        intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED) ||
                        intent.getAction().equals("android.intent.action.ACTION_SHUTDOWN") ||
                        intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON")
        ) {
            Intent serviceIntent = new Intent(context, LocationForegroundService.class);
            ContextCompat.startForegroundService(context, serviceIntent);
        }
    }
}