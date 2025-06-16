package com.jollyride.mhealth.helper;

import android.location.Location;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

public class AutoAssignService {

    public static void assignRideToDriver(String driverId, GeoPoint driverLoc, GeoPoint preferredDest) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("rides")
                .whereEqualTo("status", "requested")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        GeoPoint pickup = doc.getGeoPoint("pickupLocation");
                        GeoPoint destination = doc.getGeoPoint("destination");

                        if (pickup == null || destination == null) continue;

                        if (isNearby(driverLoc, pickup) && isRouteMatch(preferredDest, destination)) {
                            String rideId = doc.getId();

                            Map<String, Object> update = new HashMap<>();
                            update.put("driverId", driverId);
                            update.put("status", "assigned");

                            db.collection("rides").document(rideId).update(update)
                                    .addOnSuccessListener(aVoid ->
                                            Log.d("AutoAssign", "Ride assigned to driver " + driverId));
                            break; // assign only one
                        }
                    }
                });
    }

    private static boolean isNearby(GeoPoint a, GeoPoint b) {
        float[] results = new float[1];
        Location.distanceBetween(
                a.getLatitude(), a.getLongitude(),
                b.getLatitude(), b.getLongitude(),
                results
        );
        return results[0] < 3000; // 3km
    }

    private static boolean isRouteMatch(GeoPoint a, GeoPoint b) {
        float[] results = new float[1];
        Location.distanceBetween(
                a.getLatitude(), a.getLongitude(),
                b.getLatitude(), b.getLongitude(),
                results
        );
        return results[0] < 5000; // 5km
    }
}

