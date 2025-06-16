package com.jollyride.mhealth;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

public class RideNavigationActivity extends BaseActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private String rideId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_navigation);

//        rideId = getIntent().getStringExtra("rideId");
//        db = FirebaseFirestore.getInstance();
//
//        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
//                .findFragmentById(R.id.map);
//        mapFragment.getMapAsync(this);
//
//        listenForRideUpdates();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Configure map as needed
    }

    private void listenForRideUpdates() {
        db.collection("rides").document(rideId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        // Update map with pickup and destination
                        GeoPoint pickup = documentSnapshot.getGeoPoint("pickupLocation");
                        GeoPoint destination = documentSnapshot.getGeoPoint("destination");

                        mMap.clear();
                        mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(pickup.getLatitude(), pickup.getLongitude()))
                                .title("Pickup"));

                        mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(destination.getLatitude(), destination.getLongitude()))
                                .title("Destination"));

                        // Draw route between current location and pickup
                        // You would use Directions API here in a real app
                    }
                });
    }
}