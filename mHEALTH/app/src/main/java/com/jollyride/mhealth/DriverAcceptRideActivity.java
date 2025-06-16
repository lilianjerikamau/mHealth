package com.jollyride.mhealth;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

public class DriverAcceptRideActivity extends BaseActivity {

    private Button buttonAccept;
    private FirebaseFirestore db;
    private String rideId;
    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_accept_ride);

        buttonAccept = findViewById(R.id.buttonAccept);
        db = FirebaseFirestore.getInstance();

        // Get current driver ID
        driverId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get assigned ride
        fetchAssignedRide();

        buttonAccept.setOnClickListener(v -> acceptRide());

    }

    private void fetchAssignedRide() {
        db.collection("rides")
                .whereEqualTo("assignedDriverId", driverId)
                .whereEqualTo("status", "assigned")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        rideId = doc.getId();
                        String destination = doc.getString("destinationName");
                        String address = doc.getString("destinationAddress");

                        Toast.makeText(this, "Destination: " + destination + "\nAddress: " + address, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this,"No assigned ride at the moment.", Toast.LENGTH_SHORT).show();
                        buttonAccept.setEnabled(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch ride", Toast.LENGTH_SHORT).show();
                });
    }

    private void acceptRide() {
        if (rideId != null) {
            db.collection("rides").document(rideId)
                    .update("status", "accepted", "acceptedAt", FieldValue.serverTimestamp())
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Ride accepted!", Toast.LENGTH_SHORT).show();
                        // Optionally start navigation or another screen
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to accept ride", Toast.LENGTH_SHORT).show();
                    });
        }
    }
}