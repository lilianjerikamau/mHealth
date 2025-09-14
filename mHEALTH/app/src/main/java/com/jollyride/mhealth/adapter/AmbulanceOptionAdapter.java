package com.jollyride.mhealth.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.jollyride.mhealth.R;
import com.jollyride.mhealth.helper.AmbulanceModel;
import com.jollyride.mhealth.helper.DistanceUtils;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmbulanceOptionAdapter extends RecyclerView.Adapter<AmbulanceOptionAdapter.ViewHolder> {

    private final List<AmbulanceModel> ambulances;
    private final double fare;
    private final OnAmbulanceClickListener listener;
    private final LatLng pickupLocation;

    // Cache of driver locations to avoid repeated queries
    private final Map<String, LatLng> driverLocationCache = new HashMap<>();

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public interface OnAmbulanceClickListener {
        void onAmbulanceClick(AmbulanceModel ambulance);
    }

    public AmbulanceOptionAdapter(List<AmbulanceModel> ambulances, double fare, double pickupLat, double pickupLng, OnAmbulanceClickListener listener) {
        this.ambulances = ambulances;
        this.fare = fare;
        this.listener = listener;
        this.pickupLocation = new LatLng(pickupLat, pickupLng);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ambulance, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        AmbulanceModel ambulance = ambulances.get(position);
        holder.ambulanceType.setText("Private");
        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        formatter.setMaximumFractionDigits(0);
        formatter.setCurrency(Currency.getInstance("KES"));
        holder.ambulancePrice.setText(formatter.format(fare));


        String driverId = ambulance.getDriverId();
        Log.d("DRIVER ID",driverId);
        // Check cache first
        if (driverLocationCache.containsKey(driverId)) {
            LatLng driverLoc = driverLocationCache.get(driverId);
            updateEta(holder, driverLoc);
        } else {
            holder.ambulanceEta.setText("...");
            firestore.collection("availableDrivers").document(driverId)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot != null && snapshot.exists()) {
                            Log.d("SNAPSHOT", String.valueOf(snapshot));

                            GeoPoint geoPoint = snapshot.getGeoPoint("location");

                            if (geoPoint != null) {
                                LatLng driverLoc = new LatLng(geoPoint.getLatitude(), geoPoint.getLongitude());
                                driverLocationCache.put(driverId, driverLoc);
                                updateEta(holder, driverLoc);
                            } else {
                                holder.ambulanceEta.setText("N/A");
                            }
                        } else {
                            holder.ambulanceEta.setText("N/A");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AmbulanceAdapter", "Failed to get driver location", e);
                        holder.ambulanceEta.setText("N/A");
                    });

        }

        holder.itemView.setOnClickListener(v -> listener.onAmbulanceClick(ambulance));
    }

    private void updateEta(ViewHolder holder, LatLng driverLocation) {
        int etaMinutes = DistanceUtils.calculateMinutesAway(driverLocation, pickupLocation);
        holder.ambulanceEta.setText(etaMinutes + " min");
    }

    @Override
    public int getItemCount() {
        return ambulances.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ambulanceIcon;
        TextView ambulanceEta, ambulancePrice, ambulanceType;

        ViewHolder(View itemView) {
            super(itemView);
            ambulanceIcon = itemView.findViewById(R.id.ambulanceIcon);
            ambulanceEta = itemView.findViewById(R.id.ambulanceEta);
            ambulancePrice = itemView.findViewById(R.id.ambulancePrice);
            ambulanceType = itemView.findViewById(R.id.ambulanceType);
        }
    }
}
