package com.jollyride.mhealth.adapter;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jollyride.mhealth.R;
import com.jollyride.mhealth.RideDetailsActivity;
import com.jollyride.mhealth.helper.RideHistoryItem;

import java.io.IOException;
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

        // Initially show placeholders
        holder.startAddressText.setText("Loading pickup address...");
        holder.endAddressText.setText("Loading destination address...");

        // Async task to fetch addresses via Geocoder
        new ReverseGeocodingTask(context, holder.startAddressText)
                .execute(item.getPickupLocation());

        new ReverseGeocodingTask(context, holder.endAddressText)
                .execute(item.getDestination());

        // Prepare extras for click intent
        String finalFormattedDate = formattedDate;

        // Note: We'll update pickup/destination in onClick from the TextViews directly (latest resolved addresses)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RideDetailsActivity.class);
            intent.putExtra("pickup", holder.startAddressText.getText().toString());
            intent.putExtra("destination", holder.endAddressText.getText().toString());
            intent.putExtra("name", "Suzzane Gideon");  // You may want to change this to dynamic data
            intent.putExtra("gender", "Fe-male");      // Same here
            intent.putExtra("sn", "SN: T728");          // And here
            intent.putExtra("amount", "SSP " + item.getFare());
            intent.putExtra("dateTime", finalFormattedDate);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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

    // AsyncTask to do reverse geocoding off the UI thread
    private static class ReverseGeocodingTask extends AsyncTask<com.google.firebase.firestore.GeoPoint, Void, String> {

        private final Context context;
        private final TextView targetTextView;

        ReverseGeocodingTask(Context context, TextView targetTextView) {
            this.context = context.getApplicationContext();
            this.targetTextView = targetTextView;
        }

        @Override
        protected String doInBackground(com.google.firebase.firestore.GeoPoint... geoPoints) {
            if (geoPoints == null || geoPoints.length == 0 || geoPoints[0] == null) {
                return "Location unknown";
            }

            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(
                        geoPoints[0].getLatitude(),
                        geoPoints[0].getLongitude(),
                        1);

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    // Return the full address line, or fallback to locality
                    String addressLine = address.getAddressLine(0);
                    if (addressLine == null || addressLine.isEmpty()) {
                        addressLine = address.getLocality();
                    }
                    return addressLine != null ? addressLine : "Unknown location";
                } else {
                    return "Unknown location";
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "Unable to fetch address";
            }
        }

        @Override
        protected void onPostExecute(String address) {
            targetTextView.setText(address);
        }
    }
}
