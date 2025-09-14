package com.jollyride.mhealth;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.jollyride.mhealth.helper.AmbulanceModel;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ArrivingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ImageView backButton, menuButton;
    private ImageView btnCall, btnChat, btnCancel;
    private TextView txtPickup, txtDestination, txtMinutesAway;
    private ListenerRegistration rideListener;

    private GoogleMap mMap;
    private Marker ambulanceMarker;

    private AmbulanceModel selectedAmbulance;
    private LatLng pickupLocation, destinationLocation, ambulanceLocation;

    private String rideId, customerId, pickupName, destinationName, destinationAddress, status;
    private double fare;

    private boolean tripEnded = false;
    private boolean isNavigated = false; // To prevent multiple navigations

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arriving);

        // Initialize UI
        backButton = findViewById(R.id.leftImage);
        menuButton = findViewById(R.id.rightImage);
        btnCall = findViewById(R.id.btnCall);
        btnChat = findViewById(R.id.btnChat);
        btnCancel = findViewById(R.id.btnCancel);
        txtPickup = findViewById(R.id.textPickup);
        txtDestination = findViewById(R.id.textDestination);
        txtMinutesAway = findViewById(R.id.timeText);

        // Get intent extras
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            rideId = extras.getString("rideId");
            customerId = extras.getString("customerId");
            pickupName = extras.getString("pickupLocationName");
            destinationName = extras.getString("destinationName");
            destinationAddress = extras.getString("destinationAddress");
            status = extras.getString("status", "arriving");
            fare = extras.getDouble("fare", 0);

            double pickupLat = extras.getDouble("pickupLat");
            double pickupLng = extras.getDouble("pickupLng");
            double destinationLat = extras.getDouble("destinationLat");
            double destinationLng = extras.getDouble("destinationLng");

            pickupLocation = new LatLng(pickupLat, pickupLng);
            destinationLocation = new LatLng(destinationLat, destinationLng);

            selectedAmbulance = extras.getParcelable("selectedAmbulance");
            if (selectedAmbulance != null && selectedAmbulance.getLocation() != null) {
                ambulanceLocation = new LatLng(
                        selectedAmbulance.getLocation().getLatitude(),
                        selectedAmbulance.getLocation().getLongitude()
                );

                txtPickup.setText(pickupName);
                txtDestination.setText(getAddressFromLatLng(this, ambulanceLocation));

                int minutes = calculateMinutesAway(ambulanceLocation, pickupLocation);
                txtMinutesAway.setText(minutes + " min");
            } else {
                Toast.makeText(this, "Ambulance location missing", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Missing ride data", Toast.LENGTH_SHORT).show();
            finish();
        }

        listenForDriverResponse();

        // Set listeners
        backButton.setOnClickListener(v -> finish());
        menuButton.setOnClickListener(v -> Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show());
        btnCall.setOnClickListener(v -> Toast.makeText(this, "Call clicked", Toast.LENGTH_SHORT).show());

        btnCancel.setOnClickListener(v -> {
            if (rideId != null && !tripEnded) {
                FirebaseFirestore.getInstance().collection("rides").document(rideId)
                        .update("status", "canceled", "canceledAt", FieldValue.serverTimestamp())
                        .addOnSuccessListener(unused -> {
                            tripEnded = true;
                            Toast.makeText(this, "Trip ended", Toast.LENGTH_SHORT).show();

                            if (rideListener != null) rideListener.remove();

                            Intent intent = new Intent(this, RiderHomeActivity.class);
                            intent.putExtra("rideId", rideId);
                            intent.putExtra("fare", fare);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Error ending trip: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            }
        });

        // Load map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.routeMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ambulanceLocation != null) {
            ambulanceMarker = mMap.addMarker(new MarkerOptions()
                    .position(ambulanceLocation)
                    .title("Ambulance")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_ambulance_moving)));

            mMap.addMarker(new MarkerOptions()
                    .position(pickupLocation)
                    .title("Pickup"));

            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ambulanceLocation, 14));

            simulateAmbulanceMovement();
        }
    }

    private void simulateAmbulanceMovement() {
        if (pickupLocation == null || ambulanceLocation == null) return;

        new Thread(() -> {
            try {
                for (int i = 0; i < 10 && !tripEnded; i++) {
                    Thread.sleep(2000);

                    double latStep = (pickupLocation.latitude - ambulanceLocation.latitude) / 10.0;
                    double lngStep = (pickupLocation.longitude - ambulanceLocation.longitude) / 10.0;

                    LatLng newLocation = new LatLng(
                            ambulanceLocation.latitude + latStep,
                            ambulanceLocation.longitude + lngStep
                    );

                    runOnUiThread(() -> {
                        updateAmbulanceLocation(newLocation);

                        int minutes = calculateMinutesAway(newLocation, pickupLocation);
                        txtMinutesAway.setText(minutes + " min");

                        float[] results = new float[1];
                        Location.distanceBetween(
                                newLocation.latitude, newLocation.longitude,
                                pickupLocation.latitude, pickupLocation.longitude,
                                results
                        );
                        if (results[0] < 50 && !tripEnded && !isNavigated) {
                            navigateToArrived();
                        }
                    });
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateAmbulanceLocation(LatLng newLocation) {
        ambulanceLocation = newLocation;
        if (ambulanceMarker != null) {
            ambulanceMarker.setPosition(newLocation);
            mMap.animateCamera(CameraUpdateFactory.newLatLng(newLocation));
        }
    }

    private void navigateToArrived() {
        if (isNavigated || tripEnded) return;

        isNavigated = true;
        Intent intent = new Intent(ArrivingActivity.this, ArrivedActivity.class);
        intent.putExtra("rideId", rideId);
        intent.putExtra("driverId", selectedAmbulance.getDriverId());
        intent.putExtra("driverPhone", "+254700137450"); // Optional: pass from model
        intent.putExtra("vehiclePlate", "KBQ 117Q");      // Optional: pass from model
        intent.putExtra("vehicleModel", "MAZDA DEMIO");   // Optional: pass from model
        intent.putExtra("pickupName", pickupName);
        intent.putExtra("destinationName", destinationName);
        intent.putExtra("fare", fare);
        startActivity(intent);
        finish();
    }

    private void listenForDriverResponse() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference rideRef = db.collection("rides").document(rideId);

        rideListener = rideRef.addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) return;

            String rideStatus = snapshot.getString("status");
            if (rideStatus == null) return;

            switch (rideStatus) {
                case "ended":
                    Toast.makeText(this, "Driver canceled your request!", Toast.LENGTH_SHORT).show();
                    if (rideListener != null) rideListener.remove();
                    startActivity(new Intent(this, RiderHomeActivity.class));
                    finish();
                    break;

                case "declined":
                    Toast.makeText(this, "Driver declined your request.", Toast.LENGTH_LONG).show();
                    if (rideListener != null) rideListener.remove();
                    finish();
                    break;
            }
        });
    }

    private int calculateMinutesAway(LatLng from, LatLng to) {
        float[] results = new float[1];
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results);
        double distanceKm = results[0] / 1000.0;
        double minutes = (distanceKm / 40.0) * 60.0;
        return (int) Math.max(1, Math.round(minutes));
    }

    public static String getAddressFromLatLng(Context context, LatLng latLng) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(
                    latLng.latitude,
                    latLng.longitude,
                    1
            );
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            } else {
                return "Unknown Location";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Location not found";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rideListener != null) {
            rideListener.remove();
        }
    }
}
