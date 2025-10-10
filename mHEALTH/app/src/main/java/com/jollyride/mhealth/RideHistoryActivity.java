package com.jollyride.mhealth;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import com.jollyride.mhealth.adapter.RideHistoryAdapter;
import com.jollyride.mhealth.helper.RideHistoryItem;

import java.util.ArrayList;
import java.util.List;

public class RideHistoryActivity extends BaseActivity {

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
        adapter = new RideHistoryAdapter(getApplicationContext(),rideHistoryItems);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        fetchRideHistory();
    }

    private void fetchRideHistory() {
        db.collection("rides")
                .whereEqualTo("customerId", userId)
                .get()
                .addOnSuccessListener(this::onHistoryFetchSuccess)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching ride history", e);

                    if (e instanceof FirebaseFirestoreException) {
                        FirebaseFirestoreException ffe = (FirebaseFirestoreException) e;
                        if (ffe.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                            Toast.makeText(this,
                                    "Data is being indexed. Please try again in a moment.",
                                    Toast.LENGTH_LONG).show();
                            String indexLink = ffe.getMessage();
                            Log.w(TAG, "Create index here: " + indexLink);
                        } else {
                            Toast.makeText(this,
                                    "Failed to load ride history. Please try again later.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this,
                                "Unexpected error occurred.",
                                Toast.LENGTH_SHORT).show();
                    }
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

        TextView emptyStateText = findViewById(R.id.tv_empty_state);
        if (rideHistoryItems.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

}
