package com.asb.app;


import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class AutoSetupActivity extends AppCompatActivity {
    ProgressDialog progressDialog;
    WebView webView;
    @SuppressLint("MissingInflatedId")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auto_setup);
//        progressDialog = new ProgressDialog(AutoSetupActivity.this);
//        progressDialog.setCancelable(true);
//        progressDialog.setMessage("Loading...");
//        progressDialog.show();

        webView = findViewById(R.id.ViewAutoSetup);
        webView.canGoBackOrForward(99);
        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setUserAgentString(new WebView(this).getSettings().getUserAgentString());
        webView.loadUrl("https://accounts.asb.com.bd/app.php");
        webView.addJavascriptInterface(new AutoSetupActivity.WebViewJavaScriptInterface(this), "app");


    }

    public class WebViewJavaScriptInterface {
        public WebViewJavaScriptInterface(Context context) {

        }
        @JavascriptInterface
        public void get_data(final String data) {
//            Toast.makeText(AutoSetupActivity.this, data, Toast.LENGTH_SHORT).show();
            if (data.contains("null")) {
                Intent intent = new Intent(AutoSetupActivity.this, SetupActivity.class);
                startActivity(intent);
                finish();
            }else{
                writeToFile(data);
            }

        }
    }


    private void writeToFile(String soft_name) {

        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File directory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File txtFile = new File(directory, "config.txt");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(txtFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(soft_name);
            osw.flush();
            osw.close();
            fos.close();
            Toast.makeText(contextWrapper, "Successfully set as default app : " + soft_name, Toast.LENGTH_SHORT).show();
            if (soft_name.isEmpty()) {}else{
                Intent intent = new Intent(AutoSetupActivity.this, MainActivity.class);
                intent.putExtra("setup_status","empty");
                startActivity(intent);
                finish();
            }

        } catch (Exception e) {
            // on below line handling the exception.
            e.printStackTrace();
        }
    }

}