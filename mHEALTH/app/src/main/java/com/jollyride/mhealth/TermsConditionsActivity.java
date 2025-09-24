package com.jollyride.mhealth;



import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class TermsConditionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_conditions);

        // Set up the "Terms & Conditions" text
        TextView termsText = findViewById(R.id.termsText);
        String termsContent = "IMPORTANT:\n\n" +
                "THESE TERMS AND CONDITIONS (\"Conditions\") DEFINE THE BASIS UPON WHICH GETT WILL PROVIDE YOU WITH ACCESS TO THE GETT MOBILE APPLICATION PLATFORM...\n" +
                "..." // Continue with the full text of the terms
                ;
        termsText.setText(termsContent);

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

