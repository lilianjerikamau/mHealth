package com.jollyride.mhealth;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SearchingAmbulanceActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searching_ambulance);

        ImageView closeButton = findViewById(R.id.closeButton);
        ImageView searchingImage = findViewById(R.id.searchingImage);
        startPoppingAnimation(searchingImage);

        //To Stop the Animation (e.g., when driver is found):
        //searchingImage.clearAnimation();

        closeButton.setOnClickListener(v -> {
            Intent intent = new Intent(SearchingAmbulanceActivity.this, RiderHomeActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });
    }

    private void startPoppingAnimation(ImageView imageView) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(
                1.0f, 1.2f, // X: from 100% to 120%
                1.0f, 1.2f, // Y: from 100% to 120%
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );

        scaleAnimation.setDuration(500); // half a second
        scaleAnimation.setRepeatMode(Animation.REVERSE);
        scaleAnimation.setRepeatCount(Animation.INFINITE);
        imageView.startAnimation(scaleAnimation);
    }

}