package com.asb.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LocationForegroundService extends Service {
    private static final String TAG = "LocationForegroundService";
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private static final float LOCATION_UPDATE_THRESHOLD = 50; // 50 meters
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private double lastLatitude = 0;
    private double lastLongitude = 0;


    @Override
    public void onCreate() {
        super.onCreate();

        SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(LocationForegroundService.this);
        String alarm_service = sharedPrefHelper.getData("alarm_service");
        String loc_service = sharedPrefHelper.getData("loc_service");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if(Objects.equals(loc_service, "active") && !alarm_service.isEmpty() && areRequiredPermissionsGranted(this)){
                createNotificationChannel();
                startForeground(1, createNotification());
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
                startLocationUpdates();
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Service will be restarted if it gets terminated by the system
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't need to bind to this service
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Channel for Location Foreground Service");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ASB ERP Software")
                .setContentText("ASB is active in the background")
                .setSmallIcon(R.drawable.asb)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(80000); // 2 minutes
        locationRequest.setFastestInterval(70000); // 1 minute

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(new Date());

                    // Check if location has moved more than 50 meters before saving
                    if (isLocationChanged(latitude, longitude)) {
//                        Toast.makeText(LocationForegroundService.this, "Saved!", Toast.LENGTH_SHORT).show();
                        saveLocationToDatabase(latitude, longitude, timestamp);
                    }

                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } else {
            stopSelf(); // Stop the service if permission is not granted
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private boolean isLocationChanged(double latitude, double longitude) {
        float[] results = new float[1];
        Location.distanceBetween(lastLatitude, lastLongitude, latitude, longitude, results);
        if (results[0] > LOCATION_UPDATE_THRESHOLD) {
            // Location has changed by more than the threshold
            lastLatitude = latitude;
            lastLongitude = longitude;
            return true;
        }
        return false;
    }

    private void saveLocationToDatabase(double latitude, double longitude, String timestamp) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SharedPreferences sharedPreferences = getSharedPreferences("user_data", Context.MODE_PRIVATE);
        String userId = sharedPreferences.getString("user_id", "default_value");
        String softName = get_soft_name();
        dbHelper.insertLocation(latitude, longitude, timestamp, userId, softName);
        if (NetworkUtils.isInternetAvailable(this)) {
            uploadLocationDataToServer();
        }

    }


    private String get_soft_name() {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File directory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File txtFile = new File(directory, "config.txt");
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(txtFile));
            String line;
            String new_text = "";
            while ((line = br.readLine()) != null) {
                new_text += line;
            }
            return new_text;
        } catch (Exception e) {
            return "";
        }
    }

    public void uploadLocationDataToServer() {
        // Step 1: Read data from SQLite Database
        JSONArray locationDataArray = getLocationDataFromSQLite();

        // Step 2: Check if data exists
        if (locationDataArray.length() == 0) {
//            Toast.makeText(this, "No location data found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Step 3: Convert JSON to string
        String jsonData = locationDataArray.toString();
//        Log.d("UploadData", "JSON Data: " + jsonData);

        // Step 4: Create a RequestBody with JSON data
        RequestBody requestBody = RequestBody.create(
                jsonData,
                MediaType.parse("application/json; charset=utf-8")
        );

        // Step 5: Create a POST request
        Request request = new Request.Builder()
                .url("https://asbsoft.app/asbsoft/location_upload.php?short="+get_soft_name())  // Replace with your server URL
                .post(requestBody)
                .build();

        // Step 6: Initialize OkHttpClient with increased timeout
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        // Step 7: Send the request asynchronously
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
//                Log.e("UploadData", "Upload failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    DatabaseHelper dbHelper = new DatabaseHelper(LocationForegroundService.this);
                    // Delete all data from the locations table
                    dbHelper.delete("DELETE FROM locations");

//                    Log.d("UploadData", "Upload successful: " + response.body().string());
                } else {
//                    Log.e("UploadData", "Upload failed with status code: " + response.code());
                }
            }
        });
    }


    private JSONArray getLocationDataFromSQLite() {
        JSONArray jsonArray = new JSONArray();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            // Open the database
            db = openOrCreateDatabase("LocationDB", MODE_PRIVATE, null);

            // Query to get all location records
            cursor = db.rawQuery("SELECT * FROM locations", null);

            // Get column indices
            int idIndex = cursor.getColumnIndex("id");
            int latitudeIndex = cursor.getColumnIndex("latitude");
            int longitudeIndex = cursor.getColumnIndex("longitude");
            int timestampIndex = cursor.getColumnIndex("timestamp");
            int user_idIndex = cursor.getColumnIndex("user_id");

            // Loop through all rows
            while (cursor.moveToNext()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", cursor.getInt(idIndex));
                jsonObject.put("latitude", cursor.getDouble(latitudeIndex));
                jsonObject.put("longitude", cursor.getDouble(longitudeIndex));
                jsonObject.put("timestamp", cursor.getString(timestampIndex));
                jsonObject.put("user_id", cursor.getString(user_idIndex));
                jsonArray.put(jsonObject);
            }
        } catch (Exception e) {
//            Log.e("SQLiteError", "Error reading database: " + e.getMessage());
        } finally {
            // Close the cursor and database
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return jsonArray;
    }
}
