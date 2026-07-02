package com.asb.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LocationAlarmService extends Activity {
    private static final int REQUEST_CODE_SCHEDULE_EXACT_ALARM = 1003;

    private static final int PERMISSION_REQUEST_CODE = 100; // Define request code

    private static final String LOCATION_ENABLED = "location_enabled";
    private static final String ALARM_ENABLED = "alarm_enabled";
    private static final String NOTIFICATION_ENABLED = "notification_enabled";



    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private FusedLocationProviderClient fusedLocationClient;
    @SuppressLint({"MissingInflatedId", "ObsoleteSdkInt"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location_service);
        SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(LocationAlarmService.this);

        String name = sharedPrefHelper.getData("name");
        String desig = sharedPrefHelper.getData("desig");
        String img = sharedPrefHelper.getData("img");
        String logo = sharedPrefHelper.getData("logo");

        TextView profiName = findViewById(R.id.profile_name);
        profiName.setText(name);

        TextView profildesig = findViewById(R.id.profile_designation);
        profildesig.setText(desig);

        ImageView profileImage = findViewById(R.id.profile_image);
        if (!img.isEmpty()) {
            Glide.with(this)
                    .load(img)
                    .into(profileImage);
        }

        ImageView company_logo = findViewById(R.id.company_logo);
        if (!logo.isEmpty()) {
            Glide.with(this)
                    .load(logo)
                    .into(company_logo);
        }




        CardView createSales = findViewById(R.id.create_sales);
        CardView collection = findViewById(R.id.collection);
        CardView home = findViewById(R.id.home);
        CardView sales_report = findViewById(R.id.sales_report);

        createSales.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirect("sales");
            }
        });

        collection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirect("receipt");
            }
        });

        home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirect("dashboard?from_app=1");
            }
        });
        sales_report.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                redirect("sales-report");
            }
        });





        String alarm_service = sharedPrefHelper.getData("alarm_service");
        String loc_service = sharedPrefHelper.getData("loc_service");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if(Objects.equals(loc_service, "active") && !alarm_service.isEmpty() && areRequiredPermissionsGranted(this)){
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                checkLocationStatus();
                checkAndRequestExactAlarmPermission();
                scheduleLocationUpdates(this,1);
                startLocationService();
            }
        }


    }
    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private boolean areRequiredPermissionsGranted(Context context) {
        // Check ACCESS_FINE_LOCATION
        boolean locationGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // Check FOREGROUND_SERVICE_LOCATION (Android 10+)
        boolean fgServiceLocationGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // Check POST_NOTIFICATIONS (Android 13+)
        boolean notificationGranted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

        return locationGranted && fgServiceLocationGranted && notificationGranted;
    }

    @SuppressLint("redirect")
    public void redirect(String page) {
        SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(LocationAlarmService.this);
        sharedPrefHelper.deleteData("alarm_service");
        Intent intent = new Intent(LocationAlarmService.this, MainActivity.class);
        intent.putExtra("page", page);
        startActivity(intent);
        finish();
    }

    @SuppressLint("ScheduleExactAlarm")
    public static void scheduleLocationUpdates(Context context,Integer time) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, LocationAlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long intervalMillis = time * 1000; // 40 seconds
        long triggerTime = System.currentTimeMillis() + intervalMillis;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    private void checkAndRequestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                // Request the permission
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivityForResult(intent, REQUEST_CODE_SCHEDULE_EXACT_ALARM);
            }
        }
    }
    private void checkLocationStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showEnableLocationDialog();
        }
    }
    private void showEnableLocationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Enable Location")
                .setMessage("Location services are required for this app. Please turn on your GPS.")
                .setCancelable(false)  // Prevents the user from dismissing the dialog by clicking outside
                .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);  // Open location settings
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }
    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        // Check if location permission is granted
        boolean locationGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // Check if notification permission is granted (Android 13+)
        boolean notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

        // If both permissions are granted, show a message
        if (locationGranted && notificationGranted) {
            SharedPreferences sharedPreferencesd = getSharedPreferences("user_data", Context.MODE_PRIVATE);
            String userId = sharedPreferencesd.getString("user_id", "default_value");
//            if (!userId.equals("default_value") && !userId.isEmpty()) {

//                Toast.makeText(this, "All permissions are already granted", Toast.LENGTH_SHORT).show();
//                startLocationService();

            return; // Exit function
        }

        // Add permissions to request list if not granted
        if (!locationGranted) {
            permissionsNeeded.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (!notificationGranted) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        // Request missing permissions
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
//                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
            } else {
//                Toast.makeText(this, "One or more permissions were denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }
}
