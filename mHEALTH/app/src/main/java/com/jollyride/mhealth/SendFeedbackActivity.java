package com.jollyride.mhealth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SendFeedbackActivity extends BaseActivity {

    private ImageView driverImage;
    private TextView driverName, driverSerial, ratingDescription;
    private RatingBar ratingBar;
    private EditText messageEditText;
    private Button rateButton;

    private String driverNameStr, driverSerialStr, rideId;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_feedback);

        db = FirebaseFirestore.getInstance();

        // Bind views
        driverImage = findViewById(R.id.driverImage);
        driverName = findViewById(R.id.driverName);
        driverSerial = findViewById(R.id.driverSerial);
        ratingBar = findViewById(R.id.ratingBar);
        ratingDescription = findViewById(R.id.ratingDescription);
        messageEditText = findViewById(R.id.messageEditText);
        rateButton = findViewById(R.id.rateButton);

        // Get data from intent
        driverNameStr = getIntent().getStringExtra("driverName");
        driverSerialStr = getIntent().getStringExtra("driverSerial");
        rideId = getIntent().getStringExtra("rideId");

        if (driverNameStr == null) driverNameStr = "Unknown Driver";
        if (driverSerialStr == null) driverSerialStr = "N/A";

        driverName.setText(driverNameStr);
        driverSerial.setText("Serial: " + driverSerialStr);

        // Rating bar change listener
        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            switch ((int) rating) {
                case 1: ratingDescription.setText("Poor"); break;
                case 2: ratingDescription.setText("Fair"); break;
                case 3: ratingDescription.setText("Good"); break;
                case 4: ratingDescription.setText("Very Good"); break;
                case 5: ratingDescription.setText("Excellent"); break;
                default: ratingDescription.setText("");
            }
        });

        // Rate button click
        rateButton.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String message = messageEditText.getText().toString().trim();

            if (rating == 0) {
                Toast.makeText(this, "Please provide a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            if (rideId == null) {
                Toast.makeText(this, "Ride ID missing", Toast.LENGTH_SHORT).show();
                return;
            }

            // Prepare feedback data
            Map<String, Object> feedback = new HashMap<>();
            feedback.put("rating", rating);
            feedback.put("feedbackMessage", message);
            feedback.put("submittedAt", FieldValue.serverTimestamp());

            // Update ride document in Firestore
            db.collection("rides").document(rideId)
                    .update(feedback)
                    .addOnSuccessListener(unused -> {
                        // Mark ride as completed
                        db.collection("rides").document(rideId)
                                .update("status", "completed")
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to mark ride as completed", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to submit feedback", Toast.LENGTH_SHORT).show();
                    });
            Intent receiptIntent = new Intent(SendFeedbackActivity.this, DriverReceiptActivity.class);
            receiptIntent.putExtra("rideId", rideId);
            startActivity(receiptIntent);
            finish();
            finish();
        });
    }
}
