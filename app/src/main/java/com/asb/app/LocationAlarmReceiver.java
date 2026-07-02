package com.asb.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class LocationAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
//        Log.d("LocationAlarmReceiver", "Alarm Triggered! Starting Service...");

        Intent serviceIntent = new Intent(context, LocationForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        }

        // Reschedule the alarm
        LocationAlarmService.scheduleLocationUpdates(context,18);
    }
}
