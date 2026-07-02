package com.asb.app;

import android.content.ContextWrapper;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SetupActivity extends AppCompatActivity {
    private Button saveBtn;
    private EditText shortName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup);

        saveBtn = findViewById(R.id.saveBtn);
        shortName = findViewById(R.id.soft_name);

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String soft_name = shortName.getText().toString().trim();
                if (soft_name.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Software short name must not be empty", Toast.LENGTH_SHORT).show();
                } else {
                    new VerifySoftNameTask().execute(soft_name);
                }
            }
        });
    }

    private class VerifySoftNameTask extends AsyncTask<String, Void, String> {
        private String softName;

        @Override
        protected String doInBackground(String... params) {
            softName = params[0];
            try {
                URL url = new URL("https://asbsoft.app/verify.php?name=" + URLEncoder.encode(softName, "UTF-8"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    return response.toString().trim();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null && result.equalsIgnoreCase("success")) {
                writeToFile(softName);
            } else {
                Toast.makeText(getApplicationContext(), "Wrong name! Please input correct short name", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void writeToFile(String soft_name) {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File directory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File txtFile = new File(directory, "config.txt");

        try {
            FileOutputStream fos = new FileOutputStream(txtFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(soft_name);
            osw.flush();
            osw.close();
            fos.close();

            Toast.makeText(contextWrapper, "Successfully set as default app: " + soft_name, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SetupActivity.this, MainActivity.class);
            startActivity(intent);
            finish();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(contextWrapper, "Error saving file", Toast.LENGTH_SHORT).show();
        }
    }
}
