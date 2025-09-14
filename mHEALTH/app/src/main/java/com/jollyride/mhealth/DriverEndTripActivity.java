package com.jollyride.mhealth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.jollyride.mhealth.R;

public class DriverEndTripActivity extends AppCompatActivity {

    private ImageView leftImage, rightImage;
    private TextView centerText;

    private MaterialButton confirmButton;

    private FirebaseFirestore db;

    private String rideId;
    private double fare;
    private String pickupLocationName;
    private String destinationName;

    private TextView ambulanceTypeText, ambulanceFareText, ambulanceEtaText;
    private TextView estimatedTimeText, paymentMethodText;

    private SupportMapFragment mapFragment;
    private GoogleMap googleMap;

    private GeoPoint pickupLocationGeoPoint;
    private GeoPoint destinationGeoPoint;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.driver_end_trip);

        // Bind Views
        leftImage = findViewById(R.id.leftImage);
        rightImage = findViewById(R.id.rightImage);
        centerText = findViewById(R.id.centerText);
        confirmButton = findViewById(R.id.confirm_button);

        ambulanceTypeText = findViewById(R.id.ambulance_type_text);
        ambulanceFareText = findViewById(R.id.ambulance_fare_text);
        ambulanceEtaText = findViewById(R.id.ambulance_eta_text);

        estimatedTimeText = findViewById(R.id.estimated_time_text);
        paymentMethodText = findViewById(R.id.payment_method_text);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get rideId from Intent extras safely
        rideId = getIntent().getStringExtra("rideId");
        if (rideId == null || rideId.isEmpty()) {
            Toast.makeText(this, "Ride ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        centerText.setText("End Trip");

        leftImage.setImageResource(R.drawable.icon_back_btn);
        leftImage.setOnClickListener(v -> onBackPressed());

        rightImage.setOnClickListener(v -> Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show());

        confirmButton.setOnClickListener(v -> {
            Intent intent = new Intent(DriverEndTripActivity.this, DriverReceiptActivity.class);
            intent.putExtra("rideId", rideId);
            intent.putExtra("fare", fare);
            intent.putExtra("pickupLocationName", pickupLocationName);
            intent.putExtra("destinationName", destinationName);
            startActivity(intent);
            finish();
        });

        // Initialize map fragment and async load Google Map
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(map -> {
                googleMap = map;
                fetchRideDetails();
            });
        } else {
            Toast.makeText(this, "Map fragment not found", Toast.LENGTH_SHORT).show();
            fetchRideDetails();
        }
    }

    private void fetchRideDetails() {
        db.collection("rides")
                .document(rideId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        fare = documentSnapshot.getDouble("fare") != null ? documentSnapshot.getDouble("fare") : 0.0;
                        pickupLocationName = documentSnapshot.getString("pickupLocationName");
                        destinationName = documentSnapshot.getString("destinationName");
                        pickupLocationGeoPoint = documentSnapshot.getGeoPoint("pickupLocation");
                        destinationGeoPoint = documentSnapshot.getGeoPoint("destination");
                        String status = documentSnapshot.getString("status");

                        updateBottomSheetUI(fare, pickupLocationName, destinationName, status);

                        if (googleMap != null) {
                            showMarkersOnMap();
                        }

                    } else {
                        Toast.makeText(this, "Ride data not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load ride details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void updateBottomSheetUI(double fare, String pickupName, String destName, String status) {
        if (ambulanceTypeText != null) {
            ambulanceTypeText.setText("Private");
        }

        if (ambulanceFareText != null) {
            ambulanceFareText.setText(String.format("SSP %.2f", fare));
        }

        if (ambulanceEtaText != null) {
            ambulanceEtaText.setText("3 min");
        }

        if (estimatedTimeText != null) {
            estimatedTimeText.setText("Estimated trip time\n24 min");
        }

        if (paymentMethodText != null) {
            paymentMethodText.setText("Cash");
        }
    }

    private void showMarkersOnMap() {
        if (pickupLocationGeoPoint != null && googleMap != null) {
            LatLng pickupLatLng = new LatLng(pickupLocationGeoPoint.getLatitude(), pickupLocationGeoPoint.getLongitude());
            googleMap.addMarker(new MarkerOptions().position(pickupLatLng).title("Pickup: " + (pickupLocationName != null ? pickupLocationName : "")));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pickupLatLng, 12f));
        }

        if (destinationGeoPoint != null && googleMap != null) {
            LatLng destinationLatLng = new LatLng(destinationGeoPoint.getLatitude(), destinationGeoPoint.getLongitude());
            googleMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination: " + (destinationName != null ? destinationName : "")));
        }
    }
}
