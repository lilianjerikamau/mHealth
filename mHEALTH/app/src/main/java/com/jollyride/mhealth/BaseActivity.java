package com.jollyride.mhealth;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;
    protected ImageView rightImage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Force light mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        hideSystemUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        // Inflate base layout with DrawerLayout
        DrawerLayout fullLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_base, null);
        View contentContainer = fullLayout.findViewById(R.id.activity_content);

        // Inflate child layout into container
        getLayoutInflater().inflate(layoutResID, (ViewGroup) contentContainer, true);

        // Set as content view
        super.setContentView(fullLayout);

        // Initialize drawer components
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        rightImage = findViewById(R.id.rightImage);

        setupDrawer();
    }

    private void setupDrawer() {
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.nav_profile) {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                            ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                            : null;

                    if (uid == null) {
                        Toast.makeText(BaseActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
                        if (drawerLayout != null) {
                            drawerLayout.closeDrawer(GravityCompat.END);
                        }
                        return true;
                    }

                    // Fetch userType asynchronously
                    db.collection("users").document(uid)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String userType = documentSnapshot.getString("userType");
                                    if ("driver".equalsIgnoreCase(userType)) {
                                        startActivity(new Intent(BaseActivity.this, ProfileActivity.class));
                                    } else {
                                        startActivity(new Intent(BaseActivity.this, UserDetailsActivity.class));
                                    }
                                } else {
                                    Toast.makeText(BaseActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                                }
                                if (drawerLayout != null) {
                                    drawerLayout.closeDrawer(GravityCompat.END);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(BaseActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                if (drawerLayout != null) {
                                    drawerLayout.closeDrawer(GravityCompat.END);
                                }
                            });

                    return true;

                }

                else if (id == R.id.nav_ride_history) {
                    startActivity(new Intent(this, RideHistoryActivity.class));
                } else if (id == R.id.nav_faq) {
                    startActivity(new Intent(this, ActivityFAQ.class));
                } else if (id == R.id.nav_settings) {
                    startActivity(new Intent(this, ProfileActivity.class));
                } else if (id == R.id.nav_terms) {
                    startActivity(new Intent(this, TermsConditionsActivity.class));
                } else if (id == R.id.nav_logout) {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, SignInActivity.class));
                    finish();
                } else {
                    return false;
                }

                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.END); // or START if drawer opens from left
                }

                return true;
            });
        }


        if (rightImage != null) {
            rightImage.setOnClickListener(v -> toggleDrawer());
        }
    }

    protected void toggleDrawer() {
        if (drawerLayout == null) return;

        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            drawerLayout.openDrawer(GravityCompat.END);
        }
    }

    public void openDrawer() {
        if (drawerLayout != null && !drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.openDrawer(GravityCompat.END);
        }
    }

    public void closeDrawer() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        }
    }
}
