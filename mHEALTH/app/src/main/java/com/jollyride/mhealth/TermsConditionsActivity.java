package com.jollyride.mhealth;



import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class TermsConditionsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_conditions);

        // Set up the "Terms & Conditions" text
        TextView termsText = findViewById(R.id.termsText);
        String termsContent = "THESE TERMS AND CONDITIONS (“Conditions”) DEFINE THE BASIS UPON WHICH "
                + "GETT WILL PROVIDE YOU WITH ACCESS TO THE GETT MOBILE APPLICATION PLATFORM, PURSUANT "
                + "TO WHICH YOU WILL BE ABLE TO REQUEST CERTAIN TRANSPORTATION SERVICES FROM THIRD PARTY "
                + "DRIVERS BY PLACING ORDERS THROUGH GETT’S MOBILE APPLICATION PLATFORM. THESE CONDITIONS "
                + "(TOGETHER WITH THE DOCUMENTS REFERRED TO HEREIN) SET OUT THE TERMS OF USE ON WHICH YOU MAY, "
                + "AS A CUSTOMER, USE THE APP AND REQUEST TRANSPORTATION SERVICES. BY USING THE APP AND TICKING "
                + "THE ACCEPTANCE BOX, YOU INDICATE THAT YOU ACCEPT THESE TERMS OF USE WHICH APPLY, AMONG OTHER "
                + "THINGS, TO ALL SERVICES HEREINUNDER TO BE RENDERED TO OR BY YOU VIA THE APP WITHIN THE UK AND "
                + "THAT YOU AGREE TO ABIDE BY THEM. USE THE APP AND REQUEST TRANSPORTATION SERVICES. BY USING "
                + "THE APP AND TICKING THE ACCEPTANCE BOX, YOU INDICATE THAT YOU ACCEPT THESE TERMS OF USE WHICH "
                + "APPLY, AMONG OTHER THINGS, TO ALL SERVICES HEREINUNDER TO BE RENDERED TO OR BY YOU VIA THE APP "
                + "WITHIN THE UK AND THAT YOU AGREE TO ABIDE BY THEM.";
        termsText.setText(termsContent);
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        // Set up the Accept button
        MaterialButton acceptButton = findViewById(R.id.acceptButton);
        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();
            }
        });
    }
}

