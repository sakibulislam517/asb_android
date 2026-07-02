package com.asb.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintJob;
import android.print.PrintManager;
import android.provider.Settings;

import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;



public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1002;
    // if your website starts with www, exclude it
    private static final String myWebSite = "https://asbsoft.app/";
//    private static final String CHANNEL_ID = "noty_id";
    private WebView mMainWebview;
    WebView webView;
    ProgressDialog progressDialog;

    String print_status = "";

    // for handling file upload, set a static value, any number you like
    // this value will be used by WebChromeClient during file upload
    private static final int file_chooser_activity_code = 1;
    private static ValueCallback<Uri[]> mUploadMessageArr;

    private TextView left_arrow;
    private TextView right_arrow;
    private LinearLayout parent_layout;
    private LinearLayout left_arrow_parent;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int REQUEST_CHECK_SETTINGS = 1002;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private static final int REQUEST_CODE_SCHEDULE_EXACT_ALARM = 1003;
    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;
    private static final int PERMISSION_REQUEST_CODE = 100; // Define request code
    @SuppressLint("BatteryLife")
    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(MainActivity.this);
        String loginInfo = sharedPrefHelper.getData("login_info");
       if (readFile().isEmpty()) {
            sharedPrefHelper.clearAllData();
            Intent intent = new Intent(MainActivity.this, SetupActivity.class);
            startActivity(intent);
            finish();
        } else {
           String alarm_service = sharedPrefHelper.getData("alarm_service");
           String login_info = sharedPrefHelper.getData("login_info");
           String loc_service = sharedPrefHelper.getData("loc_service");
           if (!alarm_service.isEmpty() && !login_info.isEmpty() && Objects.equals(loc_service, "active")){
               Intent intent = new Intent(MainActivity.this, LocationAlarmService.class);
               startActivity(intent);
               finish();
           }else{
               load_web(readFile(), "");
           }
        }
        String loc_service = sharedPrefHelper.getData("loc_service");
        if(Objects.equals(loc_service, "active")){
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            checkLocationStatus();
            checkAndRequestExactAlarmPermission();

            //check battery optimization
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }

        }
        clearOldSharedImages();

    }




    @Override
    protected void onStop() {
        super.onStop();
        SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(MainActivity.this);
        sharedPrefHelper.addData("alarm_service","Active");
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


    private boolean compareVersions(String currentVersion, String latestVersion) {
        // Simple version comparison (you can improve this logic based on your versioning system)
        return currentVersion.compareTo(latestVersion) < 0;
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
        boolean locationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // Check if notification permission is granted (Android 13+)
        boolean notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

        // If both permissions are granted, show a message
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (areRequiredPermissionsGranted(this)) {
                SharedPreferences sharedPreferencesd = getSharedPreferences("user_data", Context.MODE_PRIVATE);
                String userId = sharedPreferencesd.getString("user_id", "default_value");
                if (!userId.equals("default_value") && !userId.isEmpty()) {
                    startLocationService();
                }

                return; // Exit function
            }
        }

        // Add permissions to request list if not granted
        if (!locationGranted) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
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




    @SuppressLint("ObsoleteSdkInt")
    private void startLocationService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (areRequiredPermissionsGranted(this)) {
                Intent serviceIntent = new Intent(this, LocationForegroundService.class);
                startForegroundService(serviceIntent);
            }
        }
    }




    private void downloadImageNew(String filename, String downloadUrlOfImage, String share){
        try{
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri downloadUri = Uri.parse(downloadUrlOfImage);
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                    .setAllowedOverRoaming(false)
                    .setTitle(filename)
                    .setMimeType("image/jpeg") // Your file type. You can use this code to download other file types also.
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES,File.separator + filename + ".jpg");
            dm.enqueue(request);
            Toast.makeText(this, "Image download started.", Toast.LENGTH_SHORT).show();

            if(Objects.equals(share, "share")){
                Toast.makeText(this, "Opening share options...", Toast.LENGTH_LONG).show();
                new Handler().postDelayed(() -> shareImage(filename), 5000); // Wait for 5 seconds
            }

        }catch (Exception e){
            Toast.makeText(this, "Image download failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareImage(String filename) {
        // Get the file from the Pictures directory
        File imagePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), filename + ".jpg");

        if (!imagePath.exists()) {
            Toast.makeText(this, "Image not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use FileProvider to generate a content Uri
        Uri imageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", imagePath);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Share Image"));
    }

    private void shareImageFromUrl(String url) {
        new Thread(() -> {
            try {
                // Download the image from URL
                URL imageUrl = new URL(url);
                Bitmap bitmap = BitmapFactory.decodeStream(imageUrl.openConnection().getInputStream());

                // Save to cache
                File cachePath = new File(getCacheDir(), "images");
                cachePath.mkdirs(); // create if not exists
                File file = new File(cachePath, "shared_image.png");
                FileOutputStream stream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                stream.close();

                // Get URI with FileProvider
                Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

                // Share intent
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                runOnUiThread(() -> {
                    startActivity(Intent.createChooser(shareIntent, "Share via"));
//                    Toast.makeText(this, "Image ready to share!", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to share image", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void clearOldSharedImages() {
        File dir = new File(getCacheDir(), "images");
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                file.delete();
            }
        }
    }






    TextView printBtn;
    @SuppressLint({"JavascriptInterface", "MissingInflatedId", "SetJavaScriptEnabled"})
    private void load_web(String shortName, String type) {
        setContentView(R.layout.activity_main);
        mMainWebview = new WebView(MainActivity.this);
        // initialize the progressDialog
        new Handler(Looper.getMainLooper()).post(() -> {
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setCancelable(true);
            progressDialog.setMessage("Loading...");
            progressDialog.show();
        });

        // get the web-view from the layout
        webView = findViewById(R.id.webView);

        printBtn = findViewById(R.id.printBtn);
        printBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createWebPrintJob(webView);
            }
        });


        left_arrow = findViewById(R.id.left_arrow);
        left_arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.left_arrow_parent).setVisibility(View.INVISIBLE);
                findViewById(R.id.right_arrow_parent).setVisibility(View.VISIBLE);
            }
        });


        right_arrow = findViewById(R.id.right_arrow);
        right_arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.right_arrow_parent).setVisibility(View.INVISIBLE);
                findViewById(R.id.left_arrow_parent).setVisibility(View.VISIBLE);
            }
        });





        // for handling Android Device [Back] key press
        webView.canGoBackOrForward(99);

        // handling web page browsing mechanism
        webView.setWebViewClient(new myWebViewClient());

        // handling file upload mechanism
        webView.setWebChromeClient(new myWebChromeClient());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true);
        }

        // some other settings
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setUserAgentString(new WebView(this).getSettings().getUserAgentString());
        @SuppressLint("HardwareIds") String androidId = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );
        // set the download listener
        webView.setDownloadListener(downloadListener);
        String app_url = "?from_where=app&device_id="+androidId;


        Intent intent = getIntent();
        String page = intent.getStringExtra("page");
        if (page != null) {
            shortName = page;
        }
        // load the website
        String load_web_url = myWebSite + shortName + app_url;

        if (type.contains("url")) {
            load_web_url = shortName;
        }

        SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(MainActivity.this);
        String loginInfo = sharedPrefHelper.getData("login_info");
        if (!loginInfo.isEmpty()){
            load_web_url += "&login_info="+loginInfo;
        }
        webView.loadUrl(load_web_url);
        webView.addJavascriptInterface(new WebViewJavaScriptInterface(this), "app");
    }

    public class WebViewJavaScriptInterface {

        public WebViewJavaScriptInterface(Context context) {

        }

        @JavascriptInterface
        public void print(final String data, String type, String FileName) {
            if (type.contains("download")) {
                downloadImageNew(FileName, data, "");
            }else {
                writeToFile(data, "config.txt", false);
            }
        }

        @JavascriptInterface
        public void set_userid(final String data) {
            checkAndRequestPermissions();
            SharedPreferences sharedPreferences = getSharedPreferences("user_data", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("user_id", data);
            editor.apply();
        }

        @JavascriptInterface
        public void check_version(final String data) {
//            String currentVersion = "1.0"; // Get current version from your app's metadata
//            if (compareVersions(currentVersion, data)) {
//                Toast.makeText(MainActivity.this, "Please Update your apps", Toast.LENGTH_SHORT).show();
//            }
        }
        @JavascriptInterface
        public void set_login_info(final String data) {
            if (data != null && !data.isEmpty()) {
                SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(MainActivity.this);
                sharedPrefHelper.addData("login_info",data);
//                Toast.makeText(MainActivity.this, data, Toast.LENGTH_SHORT).show();
            }
        }

        @JavascriptInterface
        public void set_user_info(final String name,final String desig,final String img,final String logo) {
            SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(MainActivity.this);
            sharedPrefHelper.addData("name",name);
            sharedPrefHelper.addData("desig",desig);
            sharedPrefHelper.addData("img",img);
            sharedPrefHelper.addData("logo",logo);

        }
        @JavascriptInterface
        public void log_out(final String data) {
            if (data != null && !data.isEmpty()) {
                SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(MainActivity.this);
                sharedPrefHelper.deleteData("login_info");
            }
        }
        @JavascriptInterface
        public void start_service() {
            SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(MainActivity.this);
            sharedPrefHelper.addData("alarm_service","Active");
            Intent intent = new Intent(MainActivity.this, LocationAlarmService.class);
            startActivity(intent);
            finish();
//            Toast.makeText(MainActivity.this, "Service is Activated", Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void location_service_active(String service) {
            SharedPrefHelper sharedPrefHelper = new SharedPrefHelper(MainActivity.this);
            if(Objects.equals(service, "active")){
                sharedPrefHelper.addData("loc_service","active");
//                Toast.makeText(MainActivity.this, "loc is Activated", Toast.LENGTH_SHORT).show();
            }else{
                sharedPrefHelper.addData("loc_service","inactive");
//                Toast.makeText(MainActivity.this, "not", Toast.LENGTH_SHORT).show();
            }
        }

        @JavascriptInterface
        public void share(final String data, String FileName) {
            shareImageFromUrl(data);
        }



    }



    private void createWebPrintJob(WebView webView) {
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter();
        String jobName = getString(R.string.app_name) + " Document";
        PrintAttributes.Builder builder = new PrintAttributes.Builder();
        PrintJob printJob = printManager.print(jobName, printAdapter, builder.build());
    }

    private void writeToFile(String soft_name,String file_name, Boolean append) {
//        String soft_name = shortName.getText().toString();
//        if (soft_name.isEmpty()) {
//            Toast.makeText(MainActivity.this, "Please enter your software short name", Toast.LENGTH_SHORT).show();
//            return;
//        }


        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File directory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File txtFile = new File(directory, file_name);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(txtFile,append);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(soft_name);
            osw.flush();
            osw.close();
            fos.close();
            Toast.makeText(contextWrapper, "Successfully set as default app : " + soft_name, Toast.LENGTH_SHORT).show();
            if (soft_name.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            } else {
                load_web(soft_name, "");
            }

        } catch (Exception e) {
            // on below line handling the exception.
            e.printStackTrace();
        }
    }

    private boolean Create_File(String data, String file_name, Boolean append) {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File directory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File txtFile = new File(directory, file_name);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(txtFile,append);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(data);
            osw.flush();
            osw.close();
            fos.close();
            return true;
        } catch (Exception e) {
            // on below line handling the exception.
            e.printStackTrace();
        }
        return false;
    }

    private String readFile() {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File directory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
//        Log.e("TAG", "download is : " + directory.getAbsolutePath() + "" + directory);

        File txtFile = new File(directory, "config.txt");
        StringBuilder text = new StringBuilder();
        try {
            // on below line creating and initializing buffer reader.
            BufferedReader br = new BufferedReader(new FileReader(txtFile));
            // on below line creating a string variable/
            String line;
            String new_text = "";
            // on below line setting the data to text
            while ((line = br.readLine()) != null) {
//                text.append(line);
//                text.append('');
                new_text += line;
            }
//            br.close();
            // on below line handling the exception
            return new_text;
        } catch (Exception e) {
            return "";
        }
    }

    // after the file chosen handled, variables are returned back to MainActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // check if the chrome activity is a file choosing session
        if (requestCode == file_chooser_activity_code) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri[] results = null;

                // Check if response is a multiple choice selection containing the results
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    // Response is a single choice selection
                    results = new Uri[]{data.getData()};
                }

                mUploadMessageArr.onReceiveValue(results);
                mUploadMessageArr = null;
            } else {
                mUploadMessageArr.onReceiveValue(null);
                mUploadMessageArr = null;
                Toast.makeText(MainActivity.this, "Error getting file", Toast.LENGTH_LONG).show();
            }
        }
    }

    class myWebViewClient extends WebViewClient {
        private boolean errorOccurred = false;

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressDialog.show();
            errorOccurred = false; // reset error state
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressDialog.dismiss();

            if (errorOccurred) {
                // Load local offline page only once per failure
//                webView.loadUrl("file:///android_asset/no_internet.html");
                Intent intent = new Intent(MainActivity.this, NoInternetActivity.class);
                startActivity(intent);
                finish();
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            errorOccurred = true;
            Toast.makeText(getApplicationContext(), "Internet issue", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }



    // Calling WebChromeClient to select files from the device
    public class myWebChromeClient extends WebChromeClient {
        @SuppressLint("NewApi")
        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> valueCallback, FileChooserParams fileChooserParams) {

            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            // set single file type, e.g. "image/*" for images
            intent.setType("/");

            // set multiple file types
            String[] mimeTypes = {"image/", "application/pdf", "application/excel", "application/csv", "application/"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);

            Intent chooserIntent = Intent.createChooser(intent, "Choose file");
            ((Activity) webView.getContext()).startActivityForResult(chooserIntent, file_chooser_activity_code);

            // Save the callback for handling the selected file
            mUploadMessageArr = valueCallback;

            return true;
        }
    }

    DownloadListener downloadListener = new DownloadListener() {
        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {

            progressDialog.dismiss();
            Intent i = new Intent(Intent.ACTION_VIEW);

            // example of URL = https://www.example.com/invoice.pdf
            i.setData(Uri.parse(url));
            startActivity(i);
        }
    };

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            finish();
        }
    }


}