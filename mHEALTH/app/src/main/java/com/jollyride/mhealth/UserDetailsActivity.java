package com.jollyride.mhealth;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.widget.TextView;

public class UserDetailsActivity extends AppCompatActivity {

    private TextView userName, gender, sn, rating, likes, membershipDuration;
    private TextView memberSince, vehicleType, plateNumber;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        userName = findViewById(R.id.userName);
        gender = findViewById(R.id.gender);
        sn = findViewById(R.id.sn);
        rating = findViewById(R.id.rating);
        likes = findViewById(R.id.likes);
        membershipDuration = findViewById(R.id.membershipDuration);

        memberSince = findViewById(R.id.memberSince);
        vehicleType = findViewById(R.id.vehicleType);
        plateNumber = findViewById(R.id.plateNumber);

        // Load user data (here just static demo data, replace with real data)
        userName.setText("Suzzane Gideon");
        gender.setText("Fe-male");
        sn.setText("SN: T728");
        rating.setText("4.8");
        likes.setText("126");
        membershipDuration.setText("2 years");

        memberSince.setText("16.06.2020");
        vehicleType.setText("SUV, Ambulance");
        plateNumber.setText("SSD 246CE");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
