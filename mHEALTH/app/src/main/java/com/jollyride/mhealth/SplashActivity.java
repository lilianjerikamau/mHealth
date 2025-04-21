package com.jollyride.mhealth;

import android.content.SharedPreferences;
import android.os.Bundle;

import android.content.Intent;
import android.os.Handler;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import com.jollyride.mhealth.R;

public class SplashActivity extends BaseActivity {

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
            loadLoginDetails();
        }, SPLASH_DURATION);
    }

    private void loadLoginDetails() {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    "secure_login_prefs",
                    masterKeyAlias,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            if (sharedPreferences.contains("username")){
                startActivity(new Intent(SplashActivity.this, SignInActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            }else{
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                finish();
            }

            /*if (sharedPreferences.contains("username") &&
                    sharedPreferences.contains("phone") &&
                    sharedPreferences.contains("password")) {

                String savedUsername = sharedPreferences.getString("username", "");
                String savedPhone = sharedPreferences.getString("phone", "");
                String savedPassword = sharedPreferences.getString("password", "");

                usernameEditText.setText(savedUsername);
                phoneEditText.setText(savedPhone);
                passwordEditText.setText(savedPassword);
            }*/

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}