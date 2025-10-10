package com.jollyride.mhealth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

public class SupportActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        // Toolbar setup
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // FAQ, Tickets, Contact clicks
        findViewById(R.id.tv_faq).setOnClickListener(v ->
                Toast.makeText(this, "Opening FAQ...", Toast.LENGTH_SHORT).show());
        findViewById(R.id.tv_tickets).setOnClickListener(v ->
                Toast.makeText(this, "Opening Support Tickets...", Toast.LENGTH_SHORT).show());
        findViewById(R.id.tv_contact).setOnClickListener(v ->
                Toast.makeText(this, "Contacting Support...", Toast.LENGTH_SHORT).show());

        // Option cards
        MaterialCardView cardOption1 = findViewById(R.id.card_option1);
        MaterialCardView cardOption2 = findViewById(R.id.card_option2);
        MaterialCardView cardOption3 = findViewById(R.id.card_option3);

        cardOption1.setOnClickListener(v ->{
                Toast.makeText(this, "Option 1 clicked", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, ActivityFAQ.class);
            startActivity(intent);
        });
        cardOption2.setOnClickListener(v ->
                Toast.makeText(this, "Send us an email (mhealth@email.com)", Toast.LENGTH_LONG).show());
        cardOption3.setOnClickListener(v ->
                Toast.makeText(this, "Send us an email (mhealth@email.com)", Toast.LENGTH_LONG).show());
    }
}
