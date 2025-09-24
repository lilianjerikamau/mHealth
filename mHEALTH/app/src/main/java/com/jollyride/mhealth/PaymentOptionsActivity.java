package com.jollyride.mhealth;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.jollyride.mhealth.widget.CustomRouteView;

public class PaymentOptionsActivity extends BaseActivity {

    private ImageButton backButton, menuButton;
    private MaterialCardView cardPaymentMethod1, cardPaymentMethod2;
    private CustomRouteView customRouteView; // ✅ Your custom view for route drawing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_options);

        backButton = findViewById(R.id.backButton);
        menuButton = findViewById(R.id.menuButton);
        cardPaymentMethod1 = findViewById(R.id.cardPaymentMethod1);
        cardPaymentMethod2 = findViewById(R.id.cardPaymentMethod2);
        customRouteView = findViewById(R.id.customRouteView); // ✅ Reference your view

        backButton.setOnClickListener(v -> finish());
        menuButton.setOnClickListener(v -> {
            toggleDrawer();
        });

        cardPaymentMethod1.setOnClickListener(v ->
                Toast.makeText(this, "Credit card selected", Toast.LENGTH_SHORT).show());

        cardPaymentMethod2.setOnClickListener(v ->
                Toast.makeText(this, "Cash selected", Toast.LENGTH_SHORT).show());

        // ✅ Example: draw a simple route between two points
        double startLat = 37.7749;  // San Francisco
        double startLng = -122.4194;
        double endLat = 37.7849;
        double endLng = -122.4094;

        customRouteView.setRoute(startLat, startLng, endLat, endLng, R.color.main1);
    }
}
