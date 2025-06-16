package com.jollyride.mhealth.search;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.jollyride.mhealth.R;
import com.jollyride.mhealth.adapter.RecentAdapter;
import com.jollyride.mhealth.adapter.RideItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecentFragment extends Fragment {

    private RecyclerView recentRecyclerView;
    private FirebaseFirestore db;
    private List<RideItem> rideList = new ArrayList<>();
    private RecentAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recent, container, false);
        recentRecyclerView = view.findViewById(R.id.recentRecyclerView);
        recentRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RecentAdapter(requireContext(),rideList);
        recentRecyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("rides")
                .whereEqualTo("customerId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    rideList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String customerId = doc.getString("customerId");
                        String name = doc.getString("destinationName");
                        String address = doc.getString("destinationAddress");

                        if (name != null && address != null) {
                            rideList.add(new RideItem(customerId, name, address));
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("RecentFragment", "Error fetching rides", e));

        return view;
    }
}


