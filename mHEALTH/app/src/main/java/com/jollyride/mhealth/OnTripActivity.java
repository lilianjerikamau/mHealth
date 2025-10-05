package com.jollyride.mhealth;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.jollyride.mhealth.widget.CustomRouteView;

public class OnTripActivity extends BaseActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private ImageButton menuButton;
    private ImageView driverImage;
    private TextView driverName, driverSerial, vehiclePlate, vehicleModel;
    private Button panicButton;
    private CustomRouteView routeView;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;

    private String rideId;
    private String driverId;
    private String driverSerialNum;
    private String vehiclePlateNo;
    private String vehicleModelName;

    private double pickupLat = Double.NaN;
    private double pickupLng = Double.NaN;
    private double destLat = Double.NaN;
    private double destLng = Double.NaN;
    private boolean hasNavigatedToFeedback = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ontrip);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Bind UI
        menuButton = findViewById(R.id.menuButton);
        driverImage = findViewById(R.id.driverImage);
        driverName = findViewById(R.id.driverName);
        driverSerial = findViewById(R.id.driverSerial);
        vehiclePlate = findViewById(R.id.vehiclePlate);
        vehicleModel = findViewById(R.id.vehicleModel);
        panicButton = findViewById(R.id.panicButton);
        routeView = findViewById(R.id.customRouteView);

        // Intent extras
        Intent intent = getIntent();
        rideId = intent.getStringExtra("rideId");
        driverId = intent.getStringExtra("driverId");
        driverSerialNum = intent.getStringExtra("driverSerial");
        vehiclePlateNo = intent.getStringExtra("vehiclePlate");
        vehicleModelName = intent.getStringExtra("vehicleModel");

        driverName.setText(driverId != null ? driverId : "Unknown");
        driverSerial.setText("Serial: " + (driverSerialNum != null ? driverSerialNum : "N/A"));
        vehiclePlate.setText(vehiclePlateNo != null ? vehiclePlateNo : "N/A");
        vehicleModel.setText(vehicleModelName != null ? vehicleModelName : "N/A");

        // Set custom icons for pickup/destination/driver
        Drawable pickupIcon = ContextCompat.getDrawable(this, R.drawable.ambulance);
        Drawable destinationIcon = ContextCompat.getDrawable(this, R.drawable.ic_hospital_marker);
        Drawable driverIcon = ContextCompat.getDrawable(this, R.drawable.ambulance);
        routeView.setPickupDrawable(pickupIcon);
        routeView.setDestinationDrawable(destinationIcon);
        routeView.setDriverDrawable(driverIcon);
        menuButton.setOnClickListener(v -> {
            toggleDrawer();
        });
        // Load pickup & destination from Firestore
        if (rideId != null) {
            db.collection("rides").document(rideId).get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            GeoPoint pickup = snapshot.getGeoPoint("pickup");
                            GeoPoint dest = snapshot.getGeoPoint("destination");
                            if (pickup != null) {
                                pickupLat = pickup.getLatitude();
                                pickupLng = pickup.getLongitude();
                            }
                            if (dest != null) {
                                destLat = dest.getLatitude();
                                destLng = dest.getLongitude();
                            }

                            if (!Double.isNaN(pickupLat) && !Double.isNaN(destLat)) {
                                routeView.setLocations(
                                        new LatLng(pickupLat, pickupLng),
                                        new LatLng(destLat, destLng)
                                );
                            }
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load trip info", Toast.LENGTH_SHORT).show());
        }

        panicButton.setOnClickListener(v -> {
            startActivity(new Intent(this, PanicReasonActivity.class));
            Toast.makeText(this, "Panic alert sent!", Toast.LENGTH_SHORT).show();
        });

        menuButton.setOnClickListener(v -> {
            toggleDrawer();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private void startLocationUpdates() {
        LocationRequest request;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            request = new LocationRequest.Builder(2000)
                    .setMinUpdateIntervalMillis(1000)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .build();
        } else {
            request = LocationRequest.create()
                    .setInterval(2000)
                    .setFastestInterval(1000)
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }

        if (locationCallback == null) {
            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult result) {
                    for (Location loc : result.getLocations()) {
                        // update driver icon
                        routeView.updateDriverLocation(new LatLng(loc.getLatitude(), loc.getLongitude()));
                        checkArrival(loc);
                    }
                }
            };
        }

        fusedLocationClient.requestLocationUpdates(request, locationCallback, getMainLooper());
    }

    private void checkArrival(Location current) {
        if (!Double.isNaN(destLat) && !hasNavigatedToFeedback) {
            float[] results = new float[1];
            Location.distanceBetween(
                    current.getLatitude(), current.getLongitude(),
                    destLat, destLng,
                    results
            );
            if (results[0] < 50) {
                hasNavigatedToFeedback = true;
                Intent feedbackIntent = new Intent(this, SendFeedbackActivity.class);
                feedbackIntent.putExtra("rideId", rideId);
                feedbackIntent.putExtra("driverId", driverId);
                feedbackIntent.putExtra("driverSerial", driverSerialNum);
                startActivity(feedbackIntent);
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission is required.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
