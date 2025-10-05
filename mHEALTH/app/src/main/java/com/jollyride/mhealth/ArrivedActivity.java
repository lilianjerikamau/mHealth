package com.jollyride.mhealth;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class ArrivedActivity extends BaseActivity {

    private ImageView leftImage, rightImage, callButton, cancelButton;
    private TextView centerText;
    private MaterialButton imComingButton;
    private ProgressBar progressBar;
    private ListenerRegistration rideListener;
    private String rideId, driverId, driverPhone, vehiclePlateNo, vehicleModelName, pickupName, destinationName;
    private double fare;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arrived);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Bind UI components using IDs
        leftImage = findViewById(R.id.leftImage);
        rightImage = findViewById(R.id.rightImage);
        centerText = findViewById(R.id.centerText);
        callButton = findViewById(R.id.btnCall);
        cancelButton = findViewById(R.id.buttonDecline);
        imComingButton = findViewById(R.id.buttonAccept);
        progressBar = findViewById(R.id.progressBar);
        // Extract data from Intent extras
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            rideId = extras.getString("rideId", "");
            driverId = extras.getString("driverId", "Unknown Driver");
            driverPhone = extras.getString("driverPhone", "+1234567890");
            vehiclePlateNo = extras.getString("vehiclePlate", "N/A");
            vehicleModelName = extras.getString("vehicleModel", "Ambulance");
            pickupName = extras.getString("pickupName", "Pickup");
            destinationName = extras.getString("destinationName", "Destination");
            fare = extras.getDouble("fare", 0);
        }

        // Click listeners
        leftImage.setOnClickListener(v -> finish());
        
        rightImage.setOnClickListener(v -> {
            toggleDrawer();
        });

        imComingButton.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            Toast.makeText(this, "You confirmed: I'm coming", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(ArrivedActivity.this, OnTripActivity.class);
            intent.putExtra("rideId", rideId);
            intent.putExtra("driverId", driverId);

            // If you have driverSerial, add here, else remove this line or pass empty string
            // For now, removing or you can add a driverSerial variable to extract from extras
            // intent.putExtra("driverSerial", driverSerial);

            intent.putExtra("vehiclePlate", vehiclePlateNo);
            intent.putExtra("vehicleModel", vehicleModelName);
            startActivity(intent);

            progressBar.setVisibility(View.GONE);
        });

        cancelButton.setOnClickListener(v -> {
            declineRide();
        });

        if (callButton != null) {
            callButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + driverPhone));
                startActivity(intent);
            });
        }
        listenForDriverResponse();
    }
    private void listenForDriverResponse() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference rideRef = db.collection("rides").document(rideId);

        rideListener = rideRef.addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) return;

            String status = snapshot.getString("status");
            if (status == null) return;

            switch (status) {
                case "ended":
                    Toast.makeText(this, "Driver canceled your request!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(this, RiderHomeActivity.class);


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

    private void declineRide() {
        if (rideId != null && !rideId.isEmpty()) {
            db.collection("rides").document(rideId)
                    .update("status", "canceled", "canceledAt", FieldValue.serverTimestamp())
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
