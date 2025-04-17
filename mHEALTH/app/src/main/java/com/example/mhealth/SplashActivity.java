package com.example.mhealth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 4000; // 4 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Animate logo
        ImageView logo = findViewById(R.id.logo);
        TextView title = findViewById(R.id.m_health_am);

        // 🔄 Rotate Animation
        /*RotateAnimation rotate = new RotateAnimation(
                0, 360, // fromDegree toDegree
                Animation.RELATIVE_TO_SELF, 0.5f, // pivotX
                Animation.RELATIVE_TO_SELF, 0.5f); // pivotY
        rotate.setDuration(1000);
        rotate.setRepeatCount(0);
        rotate.setFillAfter(true); // Keeps logo at final rotation state

        logo.startAnimation(rotate);*/

        // Zoom In Animation
        /*ScaleAnimation zoomIn = new ScaleAnimation(
                0.5f, 1f, // Start and end values for the X axis scaling
                0.5f, 1f, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f); // Pivot point of Y scaling
        zoomIn.setDuration(2000); // Animation duration
        zoomIn.setFillAfter(true); // Keep the final state

        logo.startAnimation(zoomIn);*/

        // Create a blink animation
        AlphaAnimation blinkAnimation = new AlphaAnimation(0.0f, 1.0f);
        blinkAnimation.setDuration(900);         // duration - half a second
        blinkAnimation.setStartOffset(20);       // delay before starting
        blinkAnimation.setRepeatMode(Animation.REVERSE);
        blinkAnimation.setRepeatCount(Animation.INFINITE);

        // Apply it to the logo or title
        logo.startAnimation(blinkAnimation); // or use title.startAnimation(blinkAnimation);

        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            finish();
        }, SPLASH_DURATION);
    }
}