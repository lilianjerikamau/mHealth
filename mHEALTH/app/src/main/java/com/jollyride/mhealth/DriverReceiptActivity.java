package com.jollyride.mhealth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DriverReceiptActivity extends AppCompatActivity {

    private TextView textPickup, textDestination, timeText, fareAmount, paymentMethod;
    private MaterialButton buttonOk;

    private FirebaseFirestore db;
    private String rideId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_end_trip); // Make sure this layout exists

        // Bind views
        textPickup = findViewById(R.id.textPickup);
        textDestination = findViewById(R.id.textDestination);
        timeText = findViewById(R.id.timeText);
        fareAmount = findViewById(R.id.fareAmount);
        paymentMethod = findViewById(R.id.paymentMethod);
        buttonOk = findViewById(R.id.buttonOk);

        // Get rideId from Intent
        rideId = getIntent().getStringExtra("rideId");

        if (rideId == null || rideId.isEmpty()) {
            Toast.makeText(this, "Ride ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Fetch ride details from Firestore
        fetchRideDetails(rideId);

        // OK button listener
        buttonOk.setOnClickListener(v -> {
            Intent intent = new Intent(this, DriverHomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void fetchRideDetails(String rideId) {
        db.collection("rides").document(rideId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String pickupName = documentSnapshot.getString("pickupLocationName");
                        String destinationName = documentSnapshot.getString("destinationName");
                        Double fare = documentSnapshot.getDouble("fare");

                        textPickup.setText(pickupName != null ? pickupName : "Unknown pickup");
                        textDestination.setText(destinationName != null ? destinationName : "Unknown destination");

                        if (fare != null) {
                            fareAmount.setText(String.format(Locale.getDefault(), "SSP %.2f", fare));
                        } else {
                            fareAmount.setText("SSP 0.00");
                        }

                        // Set time
                        String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                        timeText.setText(currentTime);

                        // Payment method is hardcoded for now
                        paymentMethod.setText("Paid in Cash");

                    } else {
                        Toast.makeText(this, "Ride not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DriverReceiptActivity", "Error fetching ride", e);
                    Toast.makeText(this, "Error fetching ride details", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}
