package com.jollyride.mhealth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RideDetailsActivity extends BaseActivity {

    private TextView fromAddress, toAddress, userName, gender, sn, amount, dateTime;
    private LinearLayout driverDetail;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_details);

        fromAddress = findViewById(R.id.pickupAddressText);
        toAddress = findViewById(R.id.destinationAddressText);
        userName = findViewById(R.id.userNameText);
        gender = findViewById(R.id.userGenderText);
        sn = findViewById(R.id.userSerialNumberText);
        amount = findViewById(R.id.paymentAmountText);
        dateTime = findViewById(R.id.dateTimeText);
        driverDetail= findViewById(R.id.driverDetail);
        // Get intent data safely
        String from = getIntent().getStringExtra("pickup");
        String to = getIntent().getStringExtra("destination");
        String name = getIntent().getStringExtra("name");
        String sex = getIntent().getStringExtra("gender");
        String serial = getIntent().getStringExtra("sn");
        String pay = getIntent().getStringExtra("amount");
        String time = getIntent().getStringExtra("dateTime");

        // Set texts with default fallback values
        fromAddress.setText(from != null ? from : "Pickup address unknown");
        toAddress.setText(to != null ? to : "Destination unknown");
        userName.setText(name != null ? name : "Unknown User");
        gender.setText(sex != null ? sex : "-");
        sn.setText(serial != null ? "SN: " + serial : "SN: -");

        driverDetail.setOnClickListener(v -> {
            startActivity(new Intent(this, UserDetailsActivity.class));
        });

        // Format amount as "SSP 500.00"
        amount.setText(formatAmount(pay));

        // Format date time like "8 APRIL 2022, 18:39"
        dateTime.setText(formatDateTime(time));
    }

    private String formatAmount(String pay) {
        if (pay == null || pay.isEmpty()) return "SSP 0.00";
        try {
            double value = Double.parseDouble(pay);
            return String.format(Locale.getDefault(), "SSP %.2f", value);
        } catch (NumberFormatException e) {
            return "SSP " + pay;
        }
    }

    private String formatDateTime(String rawDate) {
        if (rawDate == null || rawDate.isEmpty()) return "Unknown date";
        // Assume rawDate comes in ISO or known format, try parse and reformat
        try {
            // Example input: "2022-04-08T18:39:00" or other ISO-like format
            SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date date = parser.parse(rawDate);
            if (date == null) return rawDate;
            SimpleDateFormat formatter = new SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault());
            return formatter.format(date).toUpperCase(Locale.getDefault());
        } catch (ParseException e) {
            // fallback, show raw string uppercase
            return rawDate.toUpperCase(Locale.getDefault());
        }
    }
}
