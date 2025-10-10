package com.jollyride.mhealth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.constraintlayout.widget.ConstraintLayout;

public class BookingDemoActivity extends BaseActivity {

    private static final String TAG = "BookingDemoActivity";
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_demo);

        Log.d(TAG, "onCreate: Activity started");

        // Handle back press navigation
        this.getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        Log.d(TAG, "onBackPressed: Navigating to SignInActivity");
                        Intent intent = new Intent(BookingDemoActivity.this, SignInActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        finish();
                    }
                });

        // Initialize GestureDetector
        gestureDetector = new GestureDetector(this, new SwipeGestureListener());
        Log.d(TAG, "onCreate: GestureDetector initialized");

        // Attach listener directly to the root layout
        ConstraintLayout mainLayout = findViewById(R.id.main);
        if (mainLayout == null) {
            Log.e(TAG, "onCreate: main layout not found! Check your XML id");
        } else {
            mainLayout.setClickable(true);
            mainLayout.setFocusable(true);
            mainLayout.setOnTouchListener((v, event) -> {
                Log.d(TAG, "Main layout onTouch: action=" + event.getAction());
                return gestureDetector.onTouchEvent(event);
            });
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent: action=" + event.getAction() +
                " x=" + event.getX() + " y=" + event.getY());

        boolean handledByDetector = gestureDetector.onTouchEvent(event);
        Log.d(TAG, "onTouchEvent: handledByDetector=" + handledByDetector);

        boolean handledBySuper = super.onTouchEvent(event);
        Log.d(TAG, "onTouchEvent: handledBySuper=" + handledBySuper);

        return handledByDetector || handledBySuper;
    }

    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            Log.d(TAG, "onDown: MotionEvent detected");
            // Must return true so that onFling is called later
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(TAG, "onFling: START");
            if (e1 == null || e2 == null) {
                Log.e(TAG, "onFling: One of the MotionEvents is null");
                return false;
            }

            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();
            Log.d(TAG, "onFling: diffX=" + diffX + ", diffY=" + diffY +
                    ", velocityX=" + velocityX + ", velocityY=" + velocityY);

            if (Math.abs(diffX) > Math.abs(diffY)) {
                // Horizontal swipe
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        Log.i(TAG, "onFling: Detected RIGHT swipe");
                        onSwipeRight();
                    } else {
                        Log.i(TAG, "onFling: Detected LEFT swipe");
                        onSwipeLeft();
                    }
                    return true;
                } else {
                    Log.d(TAG, "onFling: Horizontal movement below threshold");
                }
            } else {
                Log.d(TAG, "onFling: Vertical movement ignored");
            }
            Log.d(TAG, "onFling: END - returning false");
            return false;
        }
    }

    private void onSwipeLeft() {
        Log.i(TAG, "onSwipeLeft: Launching RideDemoActivity");
        Intent intent = new Intent(BookingDemoActivity.this, RideDemoActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void onSwipeRight() {
        Log.i(TAG, "onSwipeRight: Launching SignInActivity");
        Intent intent = new Intent(BookingDemoActivity.this, SignInActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
