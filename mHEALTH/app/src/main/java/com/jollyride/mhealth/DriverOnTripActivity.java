package com.jollyride.mhealth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

public class DriverOnTripActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_CODE_PANIC_REASON = 101;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 200;

    private ImageView leftImage, rightImage;
    private TextView centerText, timeText, textPickup, textDestination;
    private MaterialButton buttonPanic, buttonEnd;
    private ProgressBar progressBarPanic, progressBarEnd;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private ListenerRegistration rideListener;
    private String rideId;
    private double fare;  // consider using double instead of String
    private LatLng pickupLocation, destinationLocation;
    private Marker driverMarker;

    private boolean tripEnded = false;

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        leftImage.setOnClickListener(v -> onBackPressed());
        rightImage.setOnClickListener(v -> Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show());

        buttonPanic.setOnClickListener(v -> {
            progressBarPanic.setVisibility(View.VISIBLE);
            buttonPanic.setEnabled(false);
            Intent intent = new Intent(this, PanicReasonActivity.class);
            startActivityForResult(intent, REQUEST_CODE_PANIC_REASON);
        });

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

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        listenForDriverResponse();
        // Fetch data from Firestore to fill pickup/destination
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
                        // Extract values
                        Double fareValue = doc.getDouble("fare");
                        GeoPoint pickupPoint = doc.getGeoPoint("pickupLocation");
                        GeoPoint destPoint = doc.getGeoPoint("destination");
                        String pickupName = doc.getString("pickupLocationName");
                        String destName = doc.getString("destinationName");

                        // Set class fields
                        if (fareValue != null) fare = fareValue;
                        if (pickupPoint != null) {
                            pickupLocation = new LatLng(pickupPoint.getLatitude(), pickupPoint.getLongitude());
                        }
                        if (destPoint != null) {
                            destinationLocation = new LatLng(destPoint.getLatitude(), destPoint.getLongitude());
                        }

                        // Update UI
                        textPickup.setText(pickupName != null ? pickupName : "Pickup location");
                        textDestination.setText(destName != null ? destName : "Destination");

                        // If map is ready, show markers etc.
                        if (mMap != null) {
                            // show markers & polyline
                            if (pickupLocation != null) {
                                mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup"));
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pickupLocation, 13f));
                            }
                            if (destinationLocation != null) {
                                mMap.addMarker(new MarkerOptions().position(destinationLocation).title("Destination"));
                            }
                            if (pickupLocation != null && destinationLocation != null) {
                                mMap.addPolyline(new PolylineOptions()
                                        .add(pickupLocation, destinationLocation)
                                        .color(Color.BLUE)
                                        .width(8));
                            }
                        }

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

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION_PERMISSION);
            return;
        }

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // If we already loaded the ride details by now, show markers
        if (pickupLocation != null) {
            mMap.addMarker(new MarkerOptions().position(pickupLocation).title("Pickup"));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pickupLocation, 13f));
        }
        if (destinationLocation != null) {
            mMap.addMarker(new MarkerOptions().position(destinationLocation).title("Destination"));
        }
        if (pickupLocation != null && destinationLocation != null) {
            mMap.addPolyline(new PolylineOptions()
                    .add(pickupLocation, destinationLocation)
                    .color(Color.BLUE)
                    .width(8));
        }

        startUpdatingDriverLocation();
    }

    private void startUpdatingDriverLocation() {
        View mapView = findViewById(R.id.mapFragment);
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
                            if (location != null && mMap != null) {
                                LatLng driverLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                                if (driverMarker == null) {
                                    driverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("You"));
                                } else {
                                    driverMarker.setPosition(driverLatLng);
                                }

                                mMap.animateCamera(CameraUpdateFactory.newLatLng(driverLatLng));
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
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.mapFragment);
                if (mapFragment != null) {
                    mapFragment.getMapAsync(this);
                }
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

                // Send to backend if needed
            }
        }
    }
}
