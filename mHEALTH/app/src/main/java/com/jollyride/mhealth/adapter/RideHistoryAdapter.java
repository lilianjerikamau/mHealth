package com.jollyride.mhealth.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jollyride.mhealth.R;
import com.jollyride.mhealth.RideDetailsActivity;
import com.jollyride.mhealth.helper.RideHistoryItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RideHistoryAdapter extends RecyclerView.Adapter<RideHistoryAdapter.ViewHolder> {

    private final List<RideHistoryItem> items;
    private final Context context;

    public RideHistoryAdapter(Context context, List<RideHistoryItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public RideHistoryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ride_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RideHistoryAdapter.ViewHolder holder, int position) {
        RideHistoryItem item = items.get(position);

        // Format date/time
        String formattedDate = "Unknown date";
        if (item.getTimestamp() != null) {
            Date date = item.getTimestamp().toDate();
            SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault());
            formattedDate = sdf.format(date);
            holder.dateTimeText.setText(formattedDate);
        } else {
            holder.dateTimeText.setText(formattedDate);
        }

        // Status
        String status = item.getStatus() != null ? item.getStatus().toUpperCase(Locale.getDefault()) : "UNKNOWN";
        holder.statusText.setText(status);

        // Optionally change color depending on status
        if ("COMPLETED".equalsIgnoreCase(status)) {
            holder.statusText.setTextColor(holder.statusText.getResources().getColor(android.R.color.holo_green_dark));
        } else if ("CANCELLED".equalsIgnoreCase(status)) {
            holder.statusText.setTextColor(holder.statusText.getResources().getColor(android.R.color.holo_red_dark));
        } else {
            holder.statusText.setTextColor(holder.statusText.getResources().getColor(android.R.color.black));
        }

        // Address details
        String pickupAddress = "Pickup location unknown";
        String destinationAddress = "Destination unknown";

        if (item.getPickupLocation() != null) {
            pickupAddress = String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f",
                    item.getPickupLocation().getLatitude(),
                    item.getPickupLocation().getLongitude());
            holder.startAddressText.setText(pickupAddress);
        } else {
            holder.startAddressText.setText(pickupAddress);
        }

        if (item.getDestination() != null) {
            destinationAddress = String.format(Locale.getDefault(), "Lat: %.5f, Lng: %.5f",
                    item.getDestination().getLatitude(),
                    item.getDestination().getLongitude());
            holder.endAddressText.setText(destinationAddress);
        } else {
            holder.endAddressText.setText(destinationAddress);
        }

        // Handle click to navigate to RideDetailsActivity
        String finalPickupAddress = pickupAddress;
        String finalDestinationAddress = destinationAddress;
        String finalFormattedDate = formattedDate;

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RideDetailsActivity.class);
            intent.putExtra("pickup", finalPickupAddress);
            intent.putExtra("destination", finalDestinationAddress);
            intent.putExtra("name", "Suzzane Gideon");
            intent.putExtra("gender", "Fe-male");
            intent.putExtra("sn", "SN: T728");
            intent.putExtra("amount", "SSP " + item.getFare());
            intent.putExtra("dateTime", finalFormattedDate);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateTimeText, statusText, startAddressText, endAddressText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTimeText = itemView.findViewById(R.id.dateTimeText);
            statusText = itemView.findViewById(R.id.statusText);
            startAddressText = itemView.findViewById(R.id.startAddressText);
            endAddressText = itemView.findViewById(R.id.endAddressText);
        }
    }
}
