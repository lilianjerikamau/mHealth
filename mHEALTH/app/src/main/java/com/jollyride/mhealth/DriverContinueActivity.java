package com.jollyride.mhealth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.jollyride.mhealth.helper.DistanceUtils;
import com.jollyride.mhealth.widget.CustomRouteView;

public class DriverContinueActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private ListenerRegistration rideListener;
    private String rideId, driverId;
    private double pickupLat, pickupLng, fare;
    private TextView txtFare, txtMinutesAway;

    private boolean hasArrived = false;

    private CustomRouteView customRouteView; // our custom view

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_continue);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Extract ride info
        rideId = getIntent().getStringExtra("rideId");
        pickupLat = getIntent().getDoubleExtra("pickupLat", 0);
        pickupLng = getIntent().getDoubleExtra("pickupLng", 0);
        fare = getIntent().getDoubleExtra("fare", 0);

        // UI
        ImageView toolbar = findViewById(R.id.leftImage);
        toolbar.setOnClickListener(v -> finish());

        txtFare = findViewById(R.id.fare);
        txtMinutesAway = findViewById(R.id.txtMinutesAway);
        txtFare.setText(String.format("SSP %.2f", fare));

        customRouteView = findViewById(R.id.customRouteView);

        listenForDriverResponse();

        // Simulated initial driver location
        double startLat = 4.8594;
        double startLng = 31.5713;

        // set initial route
        customRouteView.setRoutePoints(startLat, startLng, pickupLat, pickupLng);

        // ETA
        int minutes = DistanceUtils.calculateMinutesAway(
                new com.google.android.gms.maps.model.LatLng(startLat, startLng),
                new com.google.android.gms.maps.model.LatLng(pickupLat, pickupLng)
        );
        txtMinutesAway.setText(minutes + " min away");

        // start location tracking
        startTrackingDriverLocation();

        // Confirm Button
        MaterialButton confirmButton = findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(v -> confirmAndNavigate());
    }

    private void startTrackingDriverLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null && !hasArrived) {
                        float[] results = new float[1];
                        Location.distanceBetween(
                                location.getLatitude(), location.getLongitude(),
                                pickupLat, pickupLng,
                                results
                        );
                        float distanceMeters = results[0];

                        // update custom view
                        customRouteView.updateDriverLocation(
                                new com.google.android.gms.maps.model.LatLng(location.getLatitude(), location.getLongitude())
                        );

                        // Update ETA
                        int minutes = DistanceUtils.calculateMinutesAway(
                                new com.google.android.gms.maps.model.LatLng(location.getLatitude(), location.getLongitude()),
                                new com.google.android.gms.maps.model.LatLng(pickupLat, pickupLng)
                        );
                        txtMinutesAway.setText(minutes + " min away");

                        if (distanceMeters <= 50) {
                            hasArrived = true;
                            navigateToArrived();
                        }
                    }

                    // Repeat every 5 seconds
                    txtMinutesAway.postDelayed(this::startTrackingDriverLocation, 5000);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                });
    }

    private void listenForDriverResponse() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference rideRef = db.collection("rides").document(rideId);

        rideListener = rideRef.addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) return;

            String rideStatus = snapshot.getString("status");
            if (rideStatus == null) return;

            switch (rideStatus) {
                case "canceled":
                    Toast.makeText(this, "Rider canceled your request!", Toast.LENGTH_SHORT).show();
                    if (rideListener != null) rideListener.remove();
                    startActivity(new Intent(this, DriverHomeActivity.class));
                    finish();
                    break;

                case "declined":
                    Toast.makeText(this, "Driver declined your request.", Toast.LENGTH_LONG).show();
                    if (rideListener != null) rideListener.remove();
                    startActivity(new Intent(this, DriverHomeActivity.class));
                    finish();
                    break;
            }
        });
    }

    private void confirmAndNavigate() {
        if (rideId == null) {
            Toast.makeText(this, "Missing ride ID", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("rides").document(rideId)
                .update(
                        "status", "onTheWay",
                        "onTheWayAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Booking Confirmed. Heading to rider...", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(DriverContinueActivity.this, DriverOnTripActivity.class);
                    intent.putExtra("rideId", rideId);
                    intent.putExtra("fare", fare);
                    intent.putExtra("pickupLat", pickupLat);
                    intent.putExtra("pickupLng", pickupLng);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update ride: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToArrived() {
        db.collection("rides").document(rideId)
                .update(
                        "status", "arrived",
                        "arrivedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(unused -> {
                    Intent intent = new Intent(this, DriverOnTripActivity.class);
                    intent.putExtra("rideId", rideId);
                    intent.putExtra("fare", fare);
                    intent.putExtra("pickupLat", pickupLat);
                    intent.putExtra("pickupLng", pickupLng);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update ride status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
