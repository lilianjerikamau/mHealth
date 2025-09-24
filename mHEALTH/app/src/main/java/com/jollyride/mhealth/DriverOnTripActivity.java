package com.jollyride.mhealth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.android.gms.maps.model.LatLng;
import com.jollyride.mhealth.widget.CustomRouteView;

public class DriverOnTripActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PANIC_REASON = 101;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 200;

    private ImageView leftImage, rightImage;
    private TextView centerText, timeText, textPickup, textDestination;
    private MaterialButton buttonPanic, buttonEnd;
    private ProgressBar progressBarPanic, progressBarEnd;

    private ListenerRegistration rideListener;
    private String rideId;
    private double fare;
    private LatLng pickupLocation, destinationLocation;

    private boolean tripEnded = false;

    private com.jollyride.mhealth.widget.CustomRouteView customRouteView;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driver_ontrip_activity);

        // Bind views
        leftImage = findViewById(R.id.leftImage);
        rightImage = findViewById(R.id.rightImage);
        centerText = findViewById(R.id.centerText);
        timeText = findViewById(R.id.timeText);
        textPickup = findViewById(R.id.textPickup);
        textDestination = findViewById(R.id.textDestination);

        buttonPanic = findViewById(R.id.buttonPanic);
        buttonEnd = findViewById(R.id.buttonEnd);
        progressBarPanic = findViewById(R.id.progressBarPanic);
        progressBarEnd = findViewById(R.id.progressBarEnd);

        customRouteView = findViewById(R.id.customRouteView);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Top bar actions
        leftImage.setOnClickListener(v -> onBackPressed());
        rightImage.setOnClickListener(v -> Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show());

        // Panic button
        buttonPanic.setOnClickListener(v -> {
            progressBarPanic.setVisibility(View.VISIBLE);
            buttonPanic.setEnabled(false);
            Intent intent = new Intent(this, PanicReasonActivity.class);
            startActivityForResult(intent, REQUEST_CODE_PANIC_REASON);
        });

        // End Trip button
        buttonEnd.setOnClickListener(v -> {
            if (rideId != null && !tripEnded) {
                progressBarEnd.setVisibility(View.VISIBLE);
                buttonEnd.setEnabled(false);

                FirebaseFirestore.getInstance().collection("rides").document(rideId)
                        .update("status", "ended", "endedAt", FieldValue.serverTimestamp())
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(this, "Trip ended", Toast.LENGTH_SHORT).show();
                            tripEnded = true;

                            Intent intent = new Intent(DriverOnTripActivity.this, DriverEndTripActivity.class);
                            intent.putExtra("rideId", rideId);
                            intent.putExtra("fare", fare);
                            startActivity(intent);

                            progressBarEnd.setVisibility(View.GONE);
                            buttonEnd.setEnabled(true);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            progressBarEnd.setVisibility(View.GONE);
                            buttonEnd.setEnabled(true);
                            Toast.makeText(this, "Error ending trip: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
        });

        // Get rideId from Intent extras
        rideId = getIntent().getStringExtra("rideId");
        if (rideId == null || rideId.isEmpty()) {
            Toast.makeText(this, "Ride ID is required", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        listenForDriverResponse();
        fetchRideDetailsAndSetup();
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

    private void fetchRideDetailsAndSetup() {
        FirebaseFirestore.getInstance().collection("rides")
                .document(rideId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Double fareValue = doc.getDouble("fare");
                        GeoPoint pickupPoint = doc.getGeoPoint("pickupLocation");
                        GeoPoint destPoint = doc.getGeoPoint("destination");
                        String pickupName = doc.getString("pickupLocationName");
                        String destName = doc.getString("destinationName");

                        if (fareValue != null) fare = fareValue;
                        if (pickupPoint != null) {
                            pickupLocation = new LatLng(pickupPoint.getLatitude(), pickupPoint.getLongitude());
                        }
                        if (destPoint != null) {
                            destinationLocation = new LatLng(destPoint.getLatitude(), destPoint.getLongitude());
                        }

                        textPickup.setText(pickupName != null ? pickupName : "Pickup location");
                        textDestination.setText(destName != null ? destName : "Destination");

                        customRouteView.setLocations(pickupLocation, destinationLocation);
                        customRouteView.setPickupDrawable(ContextCompat.getDrawable(this, R.drawable.ambulance));
                        customRouteView.setDestinationDrawable(ContextCompat.getDrawable(this, R.drawable.ic_hospital_marker));
                        customRouteView.setDriverDrawable(ContextCompat.getDrawable(this, R.drawable.ambulance));
                        startUpdatingDriverLocation();
                    } else {
                        Toast.makeText(this, "Ride details not found", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error getting ride details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void startUpdatingDriverLocation() {
        View mapView = findViewById(R.id.customRouteView);
        mapView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(DriverOnTripActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(DriverOnTripActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_CODE_LOCATION_PERMISSION);
                    return;
                }

                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(location -> {
                            if (location != null) {
                                LatLng driverLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                customRouteView.updateDriverLocation(driverLatLng);
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(DriverOnTripActivity.this,
                                "Unable to fetch location", Toast.LENGTH_SHORT).show());

                mapView.postDelayed(this, 5000);
            }
        }, 0);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startUpdatingDriverLocation();
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Handle result from panic reason
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PANIC_REASON && resultCode == RESULT_OK) {
            progressBarPanic.setVisibility(View.GONE);
            buttonPanic.setEnabled(true);

            if (data != null) {
                String panicReason = data.getStringExtra("panicReason");
                String panicMessage = data.getStringExtra("panicMessage");

                Toast.makeText(this,
                        "Panic Reason: " + panicReason +
                                (panicMessage != null && !panicMessage.isEmpty() ? "\nMessage: " + panicMessage : ""),
                        Toast.LENGTH_LONG).show();
                // send to backend if needed
            }
        }
    }
}
