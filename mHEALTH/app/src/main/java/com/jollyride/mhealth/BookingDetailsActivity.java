package com.jollyride.mhealth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.jollyride.mhealth.adapter.AmbulanceOptionAdapter;
import com.jollyride.mhealth.helper.AmbulanceModel;
import com.jollyride.mhealth.helper.DistanceUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BookingDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private Button bookRideButton;
    private ImageView backButton, menuButton;
    private RecyclerView ambulanceRecycler;
    private GoogleMap mMap;
    private ArrayList<AmbulanceModel> ambulances;
    private AmbulanceModel selectedAmbulance = null;
    private TextView etaText;
    private String rideId, customerId, pickupLocationName, destinationName, destinationAddress, status;
    private double pickupLat, pickupLng, destinationLat, destinationLng, fare;

    private ListenerRegistration rideListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_details);

        ambulances = getIntent().getParcelableArrayListExtra("ambulances");
        if (ambulances == null) ambulances = new ArrayList<>();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            rideId = extras.getString("rideId");
            customerId = extras.getString("customerId");
            pickupLat = extras.getDouble("pickupLat", 0);
            pickupLng = extras.getDouble("pickupLng", 0);
            destinationLat = extras.getDouble("destinationLat", 0);
            destinationLng = extras.getDouble("destinationLng", 0);
            pickupLocationName = extras.getString("pickupLocationName");
            destinationName = extras.getString("destinationName");
            destinationAddress = extras.getString("destinationAddress");
            status = extras.getString("status", "requested");
            fare = extras.getDouble("fare", 0);
        }

        bookRideButton = findViewById(R.id.bookRideButton);
        backButton = findViewById(R.id.backButton);
        menuButton = findViewById(R.id.menuButton);
        ambulanceRecycler = findViewById(R.id.ambulanceRecycler);
        etaText=findViewById(R.id.etaText);
        LatLng pickupLocation = new LatLng(pickupLat, pickupLng);
        LatLng destinationLocation = new LatLng(destinationLat, destinationLng);

        int etaMinutes = DistanceUtils.calculateMinutesAway(pickupLocation, destinationLocation);
        etaText.setText("Estimated trip time "+etaMinutes + " min");
        ambulanceRecycler.setLayoutManager(new LinearLayoutManager(this));
        AmbulanceOptionAdapter adapter = new AmbulanceOptionAdapter(
                ambulances,
                fare,
                pickupLat,
                pickupLng,
                ambulance -> {
                    selectedAmbulance = ambulance;
                    Toast.makeText(this, "Selected ambulance: " + ambulance.getDriverId(), Toast.LENGTH_SHORT).show();
                }
        );
        ambulanceRecycler.setAdapter(adapter);

        ambulanceRecycler.setAdapter(adapter);

        bookRideButton.setOnClickListener(v -> {
            if (selectedAmbulance == null) {
                Toast.makeText(this, "Please select an ambulance first", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // 1. Update ride document with selected driver
            db.collection("rides").document(rideId)
                    .update(
                            "driverId", selectedAmbulance.getDriverId(),
                            "status", "waitingForDriver",
                            "manuallyAssigned", true
                    )
                    .addOnSuccessListener(aVoid -> {
                        // 2. Send the ride request to the driver
                        sendRideRequestToDriver(db, selectedAmbulance.getDriverId());

                        // 3. Start listening for driver response
                        listenForDriverResponse();

                        Toast.makeText(this, "Waiting for driver to accept...", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to assign ambulance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        backButton.setOnClickListener(v -> finish());

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.routeMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void sendRideRequestToDriver(FirebaseFirestore db, String driverId) {
        Map<String, Object> request = new HashMap<>();
        request.put("rideId", rideId);
        request.put("customerId", customerId);
        request.put("pickupLat", pickupLat);
        request.put("pickupLng", pickupLng);
        request.put("destinationLat", destinationLat);
        request.put("destinationLng", destinationLng);
        request.put("pickupLocationName", pickupLocationName);
        request.put("destinationName", destinationName);
        request.put("destinationAddress", destinationAddress);
        request.put("fare", fare);
        request.put("status", "pending");
        request.put("timestamp", System.currentTimeMillis());

        db.collection("drivers")
                .document(driverId)
                .collection("rideRequests")
                .document(rideId)
                .set(request)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Ride request sent to driver", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to notify driver: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void listenForDriverResponse() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference rideRef = db.collection("rides").document(rideId);

        rideListener = rideRef.addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) return;

            String status = snapshot.getString("status");
            if (status == null) return;

            switch (status) {
                case "accepted":
                    Toast.makeText(this, "Driver accepted your request!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, ArrivingActivity.class);
                    intent.putExtra("rideId", rideId);
                    intent.putExtra("customerId", customerId);
                    intent.putExtra("pickupLat", pickupLat);
                    intent.putExtra("pickupLng", pickupLng);
                    intent.putExtra("destinationLat", destinationLat);
                    intent.putExtra("destinationLng", destinationLng);
                    intent.putExtra("pickupLocationName", pickupLocationName);
                    intent.putExtra("destinationName", destinationName);
                    intent.putExtra("destinationAddress", destinationAddress);
                    intent.putExtra("status", "arriving");
                    intent.putExtra("fare", fare);
                    intent.putExtra("selectedAmbulance", selectedAmbulance);

                    if (rideListener != null) rideListener.remove();
                    startActivity(intent);
                    finish();
                    break;

                case "declined":
                    Toast.makeText(this, "Driver declined your request.", Toast.LENGTH_LONG).show();
                    if (rideListener != null) rideListener.remove();
                    break;
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rideListener != null) {
            rideListener.remove();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (pickupLat != 0 && pickupLng != 0 && destinationLat != 0 && destinationLng != 0) {
            showPickupAndDropoffOnMap();
        }
    }

    private void showPickupAndDropoffOnMap() {
        LatLng pickup = new LatLng(pickupLat, pickupLng);
        LatLng dropoff = new LatLng(destinationLat, destinationLng);

        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(pickup).title("Pickup: " + pickupLocationName));
        mMap.addMarker(new MarkerOptions().position(dropoff).title("Drop-off: " + destinationName));

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(pickup);
        builder.include(dropoff);

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
    }
}
