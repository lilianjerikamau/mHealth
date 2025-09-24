package com.jollyride.mhealth;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AddPromoCodeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_promo_code);

        final EditText promoCodeInput = findViewById(R.id.promoCodeInput);
        Button submitButton = findViewById(R.id.submitButton);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String promoCode = promoCodeInput.getText().toString();
                if (!promoCode.isEmpty()) {
                    Toast.makeText(AddPromoCodeActivity.this, "Promo code applied: " + promoCode, Toast.LENGTH_SHORT).show();
                    // Handle promo code logic here
                } else {
                    Toast.makeText(AddPromoCodeActivity.this, "Please enter a valid promo code", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}

