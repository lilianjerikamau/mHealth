package com.jollyride.mhealth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

public class OnTripActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private ImageButton menuButton;
    private ImageView driverImage;
    private TextView driverName, driverSerial, vehiclePlate, vehicleModel;
    private Button panicButton;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Marker ambulanceMarker;

    private FirebaseFirestore db;

    private String rideId;
    private String driverId;
    private String driverSerialNum;
    private String vehiclePlateNo;
    private String vehicleModelName;

    private LatLng destinationLatLng;
    private boolean hasNavigatedToFeedback = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ontrip);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Bind views
        menuButton = findViewById(R.id.menuButton);
        driverImage = findViewById(R.id.driverImage);
        driverName = findViewById(R.id.driverName);
        driverSerial = findViewById(R.id.driverSerial);
        vehiclePlate = findViewById(R.id.vehiclePlate);
        vehicleModel = findViewById(R.id.vehicleModel);
        panicButton = findViewById(R.id.panicButton);

        // Get intent extras
        Intent intent = getIntent();
        rideId = intent.getStringExtra("rideId");
        driverId = intent.getStringExtra("driverId");
        driverSerialNum = intent.getStringExtra("driverSerial");
        vehiclePlateNo = intent.getStringExtra("vehiclePlate");
        vehicleModelName = intent.getStringExtra("vehicleModel");

        // Set UI
        driverName.setText(driverId != null ? driverId : "Unknown");
        driverSerial.setText("Serial: " + (driverSerialNum != null ? driverSerialNum : "N/A"));
        vehiclePlate.setText(vehiclePlateNo != null ? vehiclePlateNo : "N/A");
        vehicleModel.setText(vehicleModelName != null ? vehicleModelName : "N/A");

        // Load destination from Firestore
        if (rideId != null) {
            db.collection("rides").document(rideId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            GeoPoint destPoint = documentSnapshot.getGeoPoint("destination");
                            if (destPoint != null) {
                                destinationLatLng = new LatLng(destPoint.getLatitude(), destPoint.getLongitude());
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load destination", Toast.LENGTH_SHORT).show());
        }

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Panic button
        panicButton.setOnClickListener(v -> {
            startActivity(new Intent(this, PanicReasonActivity.class));
            Toast.makeText(this, "Panic alert sent!", Toast.LENGTH_SHORT).show();
        });

        // Menu button
        menuButton.setOnClickListener(v ->
                Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(2000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    LatLng ambulancePos = new LatLng(location.getLatitude(), location.getLongitude());

                    if (ambulanceMarker == null) {
                        ambulanceMarker = mMap.addMarker(new MarkerOptions()
                                .position(ambulancePos)
                                .title("Ambulance")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_ambulance_moving)));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(ambulancePos, 15));
                    } else {
                        ambulanceMarker.setPosition(ambulancePos);
                        mMap.animateCamera(CameraUpdateFactory.newLatLng(ambulancePos));
                    }

                    // Check if near destination
                    if (destinationLatLng != null && !hasNavigatedToFeedback) {
                        float[] results = new float[1];
                        Location.distanceBetween(
                                location.getLatitude(), location.getLongitude(),
                                destinationLatLng.latitude, destinationLatLng.longitude,
                                results
                        );

                        float distanceInMeters = results[0];
                        if (distanceInMeters < 50) {
                            hasNavigatedToFeedback = true;

                            Intent feedbackIntent = new Intent(OnTripActivity.this, SendFeedbackActivity.class);
                            feedbackIntent.putExtra("rideId", rideId);
                            feedbackIntent.putExtra("driverId", driverId);
                            feedbackIntent.putExtra("driverSerial", driverSerialNum);
                            startActivity(feedbackIntent);
                            finish();
                        }
                    }
                }
            }
        }, getMainLooper());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission is required to show ambulance movement.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
