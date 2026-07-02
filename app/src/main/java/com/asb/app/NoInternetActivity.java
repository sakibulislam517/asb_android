package com.asb.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class NoInternetActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_internet);

        // Find views
        TextView iconTextView = findViewById(R.id.iconTextView);
        View containerView = findViewById(R.id.container);
        Button retryButton = findViewById(R.id.retryButton);

        // Load animations
        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation bounceAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce);

        // Apply animations
        containerView.startAnimation(fadeInAnimation);
        iconTextView.startAnimation(bounceAnimation);

        // Set button click listener
        retryButton.setOnClickListener(v -> {
            Intent intent = new Intent(NoInternetActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}