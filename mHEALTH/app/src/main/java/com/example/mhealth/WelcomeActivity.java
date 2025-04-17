package com.example.mhealth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class WelcomeActivity extends AppCompatActivity {

    private static final int WELCOME_DURATION = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Animate logo
        ImageView logo = findViewById(R.id.logo);

        // 🔄 Rotate Animation
        RotateAnimation rotate = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(1000);
        rotate.setFillAfter(true);

        // 🔍 Zoom In Animation
        ScaleAnimation zoomIn = new ScaleAnimation(
                0.5f, 1f,
                0.5f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        zoomIn.setDuration(2000);
        zoomIn.setFillAfter(true);

        // 🎬 Combine both animations
        AnimationSet animationSet = new AnimationSet(true);
        animationSet.addAnimation(rotate);
        animationSet.addAnimation(zoomIn);
        animationSet.setFillAfter(true);

        logo.startAnimation(animationSet);


        new Handler().postDelayed(() -> {
            startActivity(new Intent(WelcomeActivity.this, SignInActivity.class));
            finish();
        }, WELCOME_DURATION);
    }
}