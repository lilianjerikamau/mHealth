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
import com.jollyride.mhealth.SearchingAmbulanceActivity;

import java.util.List;

public class RecentAdapter extends RecyclerView.Adapter<RecentAdapter.RecentViewHolder> {

    private Context context;
    private List<RideItem> recentItems;

    public RecentAdapter(Context context, List<RideItem> recentItems) {
        this.context = context;
        this.recentItems = recentItems;
    }

    @NonNull
    @Override
    public RecentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent, parent, false);
        return new RecentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentViewHolder holder, int position) {
        RideItem ride = recentItems.get(position);
        holder.destinationName.setText(ride.getDestinationName());
        holder.destinationAddress.setText(ride.getDestinationAddress());

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, SearchingAmbulanceActivity.class);
            intent.putExtra("customerId", ride.getCustomerId());
            intent.putExtra("destinationName", ride.getDestinationName());
            intent.putExtra("destinationAddress", ride.getDestinationAddress());
            context.startActivity(intent);
        });

    }

    @Override
    public int getItemCount() {
        return recentItems.size();
    }

    static class RecentViewHolder extends RecyclerView.ViewHolder {
        TextView destinationName, destinationAddress;

        RecentViewHolder(View itemView) {
            super(itemView);
            destinationName = itemView.findViewById(R.id.destinationName);
            destinationAddress = itemView.findViewById(R.id.destinationAddress);
        }
    }
}

