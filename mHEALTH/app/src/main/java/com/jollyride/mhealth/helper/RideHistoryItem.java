package com.jollyride.mhealth.helper;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

public class RideHistoryItem {
    private String rideId;
    private GeoPoint pickupLocation;
    private GeoPoint destination;
    private Timestamp timestamp;
    private String status;
    private double fare;

    public RideHistoryItem() {
        // Firestore needs no-arg constructor
    }

    public RideHistoryItem(String rideId, GeoPoint pickupLocation, GeoPoint destination,
                           Timestamp timestamp, String status, double fare) {
        this.rideId = rideId;
        this.pickupLocation = pickupLocation;
        this.destination = destination;
        this.timestamp = timestamp;
        this.status = status;
        this.fare = fare;
    }

    // Getters

    public String getRideId() {
        return rideId;
    }

    public GeoPoint getPickupLocation() {
        return pickupLocation;
    }

    public GeoPoint getDestination() {
        return destination;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }

    public double getFare() {
        return fare;
    }
}
