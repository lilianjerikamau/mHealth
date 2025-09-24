package com.jollyride.mhealth;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class PromoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promo);

        // Add promo code button click listener
        Button addPromoButton = findViewById(R.id.addPromoButton);
        addPromoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(PromoActivity.this, "Redirecting to Add Promo code", Toast.LENGTH_SHORT).show();
                 Intent intent = new Intent(PromoActivity.this, AddPromoCodeActivity.class);
                 startActivity(intent);
            }
        });
    }
}

