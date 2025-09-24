package com.jollyride.mhealth;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import com.jollyride.mhealth.adapter.RideHistoryAdapter;
import com.jollyride.mhealth.helper.RideHistoryItem;

import java.util.ArrayList;
import java.util.List;

public class RideHistoryActivity extends AppCompatActivity {

    private static final String TAG = "RideHistoryActivity";

    private RecyclerView recyclerView;
    private RideHistoryAdapter adapter;
    private List<RideHistoryItem> rideHistoryItems;

    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ride_history);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.rideHistoryRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        rideHistoryItems = new ArrayList<>();
        adapter = new RideHistoryAdapter(rideHistoryItems);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        fetchRideHistory();
    }

    private void fetchRideHistory() {
        db.collection("rides")
                .whereEqualTo("customerId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(this::onHistoryFetchSuccess)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching ride history", e);
                    // maybe show a Toast or empty view
                });
    }

    private void onHistoryFetchSuccess(QuerySnapshot querySnapshot) {
        rideHistoryItems.clear();

        for (QueryDocumentSnapshot doc : querySnapshot) {
            String rideId = doc.getId();
            String status = doc.getString("status");
            GeoPoint pickup = doc.getGeoPoint("pickupLocation");
            GeoPoint destination = doc.getGeoPoint("destination");
            double fare = 0.0;
            if (doc.contains("fare")) {
                Object fareObj = doc.get("fare");
                if (fareObj instanceof Number) {
                    fare = ((Number) fareObj).doubleValue();
                }
            }
            com.google.firebase.Timestamp timestamp = doc.getTimestamp("timestamp");

            RideHistoryItem item = new RideHistoryItem(
                    rideId,
                    pickup,
                    destination,
                    timestamp,
                    status,
                    fare
            );

            rideHistoryItems.add(item);
        }

        adapter.notifyDataSetChanged();
    }
}
