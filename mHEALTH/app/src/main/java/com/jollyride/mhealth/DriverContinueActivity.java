package com.jollyride.mhealth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.jollyride.mhealth.helper.DistanceUtils;

public class DriverContinueActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private ListenerRegistration rideListener;
    private String rideId, driverId;
    private double pickupLat, pickupLng, fare;
    private TextView txtFare, txtMinutesAway;

    private LatLng pickupLocation;
    private boolean hasArrived = false;

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

        pickupLocation = new LatLng(pickupLat, pickupLng);

        // UI
        ImageView toolbar = findViewById(R.id.leftImage);
        toolbar.setOnClickListener(v -> finish());

        txtFare = findViewById(R.id.fare);
        txtMinutesAway = findViewById(R.id.txtMinutesAway);
        txtFare.setText(String.format("SSP %.2f", fare));
        listenForDriverResponse();
        // Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Confirm Button
        MaterialButton confirmButton = findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(v -> confirmAndNavigate());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Simulated initial driver location (replace with real GPS in production)
        LatLng driverLocation = new LatLng(4.8594, 31.5713);

        mMap.addMarker(new MarkerOptions().position(driverLocation).title("You"));
        mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup"));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverLocation, 13));

        mMap.addPolyline(new PolylineOptions()
                .add(driverLocation, pickupLocation)
                .color(Color.BLACK)
                .width(5));

        // Initial ETA
        int minutes = DistanceUtils.calculateMinutesAway(driverLocation, pickupLocation);
        txtMinutesAway.setText(minutes + " min away");

        // Start location updates
        startTrackingDriverLocation();
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
                    if (location != null && pickupLocation != null && !hasArrived) {
                        float[] results = new float[1];
                        Location.distanceBetween(
                                location.getLatitude(), location.getLongitude(),
                                pickupLocation.latitude, pickupLocation.longitude,
                                results
                        );
                        float distanceMeters = results[0];

                        // Update ETA
                        LatLng currentDriverLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        int minutes = DistanceUtils.calculateMinutesAway(currentDriverLocation, pickupLocation);
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
                    intent.putExtra("rideId", rideId);
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
                    intent.putExtra("rideId", rideId);
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
