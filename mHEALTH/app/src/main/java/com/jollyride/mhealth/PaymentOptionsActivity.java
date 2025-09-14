package com.jollyride.mhealth;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.card.MaterialCardView;

public class PaymentOptionsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ImageButton backButton, menuButton;
    private MaterialCardView cardPaymentMethod1, cardPaymentMethod2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_options);

        backButton = findViewById(R.id.backButton);
        menuButton = findViewById(R.id.menuButton);
        cardPaymentMethod1 = findViewById(R.id.cardPaymentMethod1);
        cardPaymentMethod2 = findViewById(R.id.cardPaymentMethod2);

        backButton.setOnClickListener(v -> finish());
        menuButton.setOnClickListener(v -> {
            Toast.makeText(this, "Menu clicked", Toast.LENGTH_SHORT).show();
            // TODO: Open menu drawer or action here
        });

        cardPaymentMethod1.setOnClickListener(v ->
                Toast.makeText(this, "Credit card selected", Toast.LENGTH_SHORT).show());

        cardPaymentMethod2.setOnClickListener(v ->
                Toast.makeText(this, "Cash selected", Toast.LENGTH_SHORT).show());

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Example start and end locations
        LatLng start = new LatLng(37.7749, -122.4194);  // San Francisco
        LatLng end = new LatLng(37.7849, -122.4094);

        // Add markers on map
        mMap.addMarker(new MarkerOptions().position(start).title("Start"));
        mMap.addMarker(new MarkerOptions().position(end).title("End"));

        // Draw polyline between start and end
        mMap.addPolyline(new PolylineOptions()
                .add(start, end)
                .width(8)
                .color(Color.parseColor("#3A4352"))
                .geodesic(true));

        // Move camera to show entire route area nicely
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(start, 13));
    }
}
