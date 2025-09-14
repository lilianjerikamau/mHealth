package com.jollyride.mhealth;



import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class DriverArrivedActivity extends BaseActivity {

    private Button buttonAccept;
    private TextView textPickup, textDestination;
    private ListenerRegistration rideListener;
    private FirebaseFirestore db;
    private String rideId;
    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_accept_ride);

        buttonAccept = findViewById(R.id.buttonAccept);
        textPickup = findViewById(R.id.textPickup);
        textDestination = findViewById(R.id.textDestination);

        db = FirebaseFirestore.getInstance();
        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        fetchAssignedRide();

        buttonAccept.setOnClickListener(v -> acceptRide());
    }

    private void fetchAssignedRide() {
        db.collection("rides")
                .whereEqualTo("driverId", driverId)
                .whereEqualTo("status", "assigned")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        rideId = doc.getId();

                        String pickup = doc.getString("pickupLocationName");
                        String destination = doc.getString("destinationName");
                        String address = doc.getString("destinationAddress");

                        // Update UI
                        textPickup.setText("Pickup: " + (pickup != null ? pickup : "Unknown"));
                        textDestination.setText("Destination: " + (destination != null ? destination : "Unknown")
                                + "\nAddress: " + (address != null ? address : "N/A"));

                        buttonAccept.setEnabled(true);
                    } else {
                        textPickup.setText("No assigned ride.");
                        textDestination.setText("");
                        buttonAccept.setEnabled(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Failed to fetch ride: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    buttonAccept.setEnabled(false);
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

    private void acceptRide() {
        if (rideId != null) {
            db.collection("rides").document(rideId)
                    .update("status", "accepted",
                            "acceptedAt", FieldValue.serverTimestamp(),
                            "driverId", driverId)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Ride accepted!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(DriverArrivedActivity.this, DriverOnTripActivity.class);
                        intent.putExtra("rideId", rideId);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this,
                                "Failed to accept ride: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "No ride to accept.", Toast.LENGTH_SHORT).show();
        }
    }
}

