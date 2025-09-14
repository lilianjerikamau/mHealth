package com.jollyride.mhealth.helper;

import android.location.Location;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

public class AutoAssignService {

    public static void assignRideToDriver(String driverId, GeoPoint driverLocation, GeoPoint preferredDestination) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("rides")
                .whereEqualTo("status", "requested")
                .get()
                .addOnSuccessListener(rides -> {
                    for (DocumentSnapshot ride : rides) {
                        GeoPoint riderDest = ride.getGeoPoint("destination");
                        String rideDriverId = ride.contains("driverId") ? ride.getString("driverId") : null;
                        Boolean manuallyAssigned = ride.contains("manuallyAssigned") ? ride.getBoolean("manuallyAssigned") : false;

                        // ✅ Skip manually assigned rides
                        if (Boolean.TRUE.equals(manuallyAssigned)) {
                            continue;
                        }

                        if (riderDest != null && preferredDestination != null) {
                            float[] results = new float[1];
                            Location.distanceBetween(
                                    riderDest.getLatitude(), riderDest.getLongitude(),
                                    preferredDestination.getLatitude(), preferredDestination.getLongitude(),
                                    results
                            );

                            boolean isDestinationNearby = results[0] < 1000; // 1km
                            boolean isUnassigned = (rideDriverId == null || rideDriverId.isEmpty());

                            if (isDestinationNearby && isUnassigned) {
                                db.collection("rides").document(ride.getId())
                                        .update(
                                                "status", "assigned",
                                                "driverId", driverId
                                        )
                                        .addOnSuccessListener(aVoid -> {
                                            db.collection("availableDrivers").document(driverId)
                                                    .update("available", false);
                                            Log.d("AutoAssignService", "Ride auto-assigned to driver: " + driverId);
                                        })
                                        .addOnFailureListener(e ->
                                                Log.e("AutoAssignService", "Failed to assign ride: " + e.getMessage()));
                                break; // assign only one ride
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("AutoAssignService", "Error fetching rides: " + e.getMessage()));
    }
}
