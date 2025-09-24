package com.jollyride.mhealth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.jollyride.mhealth.adapter.AmbulanceOptionAdapter;
import com.jollyride.mhealth.helper.AmbulanceModel;
import com.jollyride.mhealth.helper.DistanceUtils;
import com.jollyride.mhealth.widget.CustomRouteView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class BookingDetailsActivity extends BaseActivity {

    private Button bookRideButton;
    private ImageView backButton, menuButton;
    private RecyclerView ambulanceRecycler;
    private ArrayList<AmbulanceModel> ambulances;
    private AmbulanceModel selectedAmbulance = null;
    private TextView etaText,etaBubble;
    private String rideId, customerId, pickupLocationName, destinationName, destinationAddress, status;
    private double pickupLat, pickupLng, destinationLat, destinationLng, fare;

    private ListenerRegistration rideListener;

    private CustomRouteView customRouteView;

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

        // UI setup
        bookRideButton = findViewById(R.id.bookRideButton);
        backButton = findViewById(R.id.backButton);
        menuButton = findViewById(R.id.menuButton);
        ambulanceRecycler = findViewById(R.id.ambulanceRecycler);
        etaText = findViewById(R.id.etaText);
        etaBubble=findViewById(R.id.etaBubble);
        customRouteView = findViewById(R.id.customRouteView);

        // ETA
        int etaMinutes = DistanceUtils.calculateMinutesAway(
                new com.google.android.gms.maps.model.LatLng(pickupLat, pickupLng),
                new com.google.android.gms.maps.model.LatLng(destinationLat, destinationLng)
        );
        etaText.setText("Estimated trip time " + etaMinutes + " min");
        etaBubble.setText(etaMinutes + " min");
        menuButton.setOnClickListener(v -> {
          toggleDrawer();
        });

        // Ambulance list
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

        // Book ride
        bookRideButton.setOnClickListener(v -> {
            if (selectedAmbulance == null) {
                Toast.makeText(this, "Please select an ambulance first", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("rides").document(rideId)
                    .update(
                            "driverId", selectedAmbulance.getDriverId(),
                            "status", "waitingForDriver",
                            "manuallyAssigned", true
                    )
                    .addOnSuccessListener(aVoid -> {
                        sendRideRequestToDriver(db, selectedAmbulance.getDriverId());
                        listenForDriverResponse();
                        Toast.makeText(this, "Waiting for driver to accept...", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to assign ambulance: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        backButton.setOnClickListener(v -> finish());

        // ✅ Set pickup & drop-off points in CustomRouteView
        if (customRouteView != null) {
            customRouteView.setRoutePoints(pickupLat, pickupLng, destinationLat, destinationLng);
            customRouteView.setPickupDrawable(ContextCompat.getDrawable(this, R.drawable.pickup_pin));
            customRouteView.setDestinationDrawable(ContextCompat.getDrawable(this, R.drawable.dest_pin));
            customRouteView.setDriverDrawable(ContextCompat.getDrawable(this, R.drawable.pickup_pin));
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
}
