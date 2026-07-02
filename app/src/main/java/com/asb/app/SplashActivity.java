package com.asb.app;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    // Duration of the splash screen in milliseconds
    private static final int SPLASH_DURATION = 2500;

    // UI Components
    private ImageView logoImageView;
    private TextView appNameTextView;
    private TextView taglineTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make the activity full screen
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        // Hide the action bar if it exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Set the layout for the activity
        setContentView(R.layout.activity_splash);

        // Initialize UI components
        logoImageView = findViewById(R.id.splash_logo);
        appNameTextView = findViewById(R.id.splash_app_name);
        taglineTextView = findViewById(R.id.splash_tagline);

        // Load animations
        Animation fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        // Apply animations
        logoImageView.startAnimation(fadeIn);
        appNameTextView.startAnimation(slideUp);
        taglineTextView.startAnimation(slideUp);

        // Delayed navigation to main activity
        new Handler().postDelayed(this::navigateToMainActivity, SPLASH_DURATION);
    }

    /**
     * Navigate to the main activity and finish the splash activity
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}