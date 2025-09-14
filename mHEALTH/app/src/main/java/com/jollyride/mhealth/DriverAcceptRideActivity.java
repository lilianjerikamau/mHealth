package com.jollyride.mhealth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class DriverAcceptRideActivity extends BaseActivity {

    private Button buttonAccept;
    private ImageView buttonDecline;
    private TextView textPickup, textDestination;

    private FirebaseFirestore db;
    private String rideId, driverId;
    private String pickup, destination, address;
    private double pickupLat, pickupLng, fare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_accept_ride);

        buttonAccept = findViewById(R.id.buttonAccept);
        buttonDecline = findViewById(R.id.buttonDecline);
        textPickup = findViewById(R.id.textPickup);
        textDestination = findViewById(R.id.textDestination);

        db = FirebaseFirestore.getInstance();
        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 🔄 Get data from Intent
        rideId = getIntent().getStringExtra("rideId");
        pickup = getIntent().getStringExtra("pickupLocationName");
        destination = getIntent().getStringExtra("destinationName");
        address = getIntent().getStringExtra("destinationAddress");
        pickupLat = getIntent().getDoubleExtra("pickupLat", 0);
        pickupLng = getIntent().getDoubleExtra("pickupLng", 0);
        fare = getIntent().getDoubleExtra("fare", 0);

        if (rideId != null) {
            textPickup.setText("Pickup: " + (pickup != null ? pickup : "Unknown"));
            textDestination.setText("Destination: " + (destination != null ? destination : "Unknown")
                    + "\nAddress: " + (address != null ? address : "N/A"));

            buttonAccept.setEnabled(true);
            buttonDecline.setEnabled(true);
        } else {
            textPickup.setText("No ride assigned.");
            textDestination.setText("");
            buttonAccept.setEnabled(false);
            buttonDecline.setEnabled(false);
        }

        buttonAccept.setOnClickListener(v -> acceptRide());
        buttonDecline.setOnClickListener(v -> declineRide());
    }

    private void acceptRide() {
        if (rideId != null) {
            db.collection("rides").document(rideId)
                    .update(
                            "status", "accepted",
                            "acceptedAt", FieldValue.serverTimestamp(),
                            "driverId", driverId
                    )
                    .addOnSuccessListener(unused -> {
                        db.collection("drivers")
                                .document(driverId)
                                .collection("rideRequests")
                                .document(rideId)
                                .delete();

                        Toast.makeText(this, "Ride accepted!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(this, DriverContinueActivity.class);
                        intent.putExtra("rideId", rideId);
                        intent.putExtra("pickupLat", pickupLat);
                        intent.putExtra("pickupLng", pickupLng);
                        intent.putExtra("fare", fare);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to accept ride: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "No ride to accept.", Toast.LENGTH_SHORT).show();
        }
    }

    private void declineRide() {
        if (rideId != null) {
            db.collection("rides").document(rideId)
                    .update(
                            "status", "declined",
                            "declinedAt", FieldValue.serverTimestamp(),
                            "driverId", driverId
                    )
                    .addOnSuccessListener(unused -> {
                        db.collection("drivers")
                                .document(driverId)
                                .collection("rideRequests")
                                .document(rideId)
                                .delete();

                        Toast.makeText(this, "Ride declined.", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to decline ride: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "No ride to decline.", Toast.LENGTH_SHORT).show();
        }
    }
}

