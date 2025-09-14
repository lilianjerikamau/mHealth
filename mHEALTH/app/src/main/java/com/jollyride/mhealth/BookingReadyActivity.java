package com.jollyride.mhealth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.GeoPoint;
import com.jollyride.mhealth.helper.AmbulanceModel;

import java.util.ArrayList;

public class BookingReadyActivity extends AppCompatActivity {

    private ImageView menuIcon;
    private Button nextButton;
    private TextView currentLocation;
    private TextView destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_ready);

        menuIcon = findViewById(R.id.menuIcon);
        nextButton = findViewById(R.id.nextButton);
        currentLocation = findViewById(R.id.currentLocation);
        destination = findViewById(R.id.destinationText);
        double pickupLat = getIntent().getDoubleExtra("pickupLat", 0);
        double pickupLng = getIntent().getDoubleExtra("pickupLng", 0);
        GeoPoint pickupLocation = new GeoPoint(pickupLat, pickupLng);
        // ✅ Get ambulances from Intent
        ArrayList<AmbulanceModel> ambulances =
                getIntent().getParcelableArrayListExtra("ambulances");

        if (ambulances != null && !ambulances.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (AmbulanceModel amb : ambulances) {
                builder.append("Driver: ").append(amb.getDriverId())
                        .append(" | Distance: ").append(String.format("%.2f km", amb.getDistanceKm()))
                        .append("\n");
            }
            destination.setText(builder.toString()); // show list in destination TextView for now
        } else {
            destination.setText("No ambulance data received.");
        }

        nextButton.setOnClickListener(v -> {
            Intent intent = new Intent(BookingReadyActivity.this, BookingDetailsActivity.class);
            intent.putParcelableArrayListExtra("ambulances", new ArrayList<>(ambulances));
            Bundle bundle = new Bundle();
            bundle.putDouble("pickupLat", pickupLocation.getLatitude());
            bundle.putDouble("pickupLng", pickupLocation.getLongitude());
            intent.putExtras(bundle);
            startActivity(intent);
            finish();
        });
    }
}
