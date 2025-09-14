package com.jollyride.mhealth;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.jollyride.mhealth.helper.AmbulanceModel;

import java.util.ArrayList;
import java.util.List;

public class SearchAmbulanceActivity extends AppCompatActivity {

    private static final String TAG = "GET_SEARCH";

    private ImageView closeButton;
    private ImageView ambulanceIcon;
    private TextView statusText;

    private FirebaseFirestore db;
    private List<AmbulanceModel> nearbyAmbulances = new ArrayList<>();

    // Ride details
    private String rideId;
    private String customerId;
    private double pickupLat, pickupLng, destinationLat, destinationLng, fare;
    private String pickupLocationName, destinationName, destinationAddress, status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_ambulance);

        Log.d(TAG, "Activity started");

        db = FirebaseFirestore.getInstance();

        closeButton = findViewById(R.id.closeButton);
        ambulanceIcon = findViewById(R.id.ambulanceIcon);
        statusText = findViewById(R.id.statusText);

        closeButton.setOnClickListener(v -> {
            Log.d(TAG, "Close button clicked");
            finish();
        });

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            rideId = extras.getString("rideId");
            customerId = extras.getString("customerId");
            pickupLat = extras.getDouble("pickupLat", 0);
            pickupLng = extras.getDouble("pickupLng", 0);
            destinationLat = extras.getDouble("destinationLat", 0);
            destinationLng = extras.getDouble("destinationLng", 0);
            pickupLocationName = extras.getString("pickupLocationName");
            destinationName = extras.getString("destinationName");
            destinationAddress = extras.getString("destinationAddress");
            status = extras.getString("status", "requested");
            fare = extras.getDouble("fare", 0);

            Log.d(TAG, "Ride data extracted: rideId=" + rideId + ", customerId=" + customerId +
                    ", pickup=(" + pickupLat + "," + pickupLng + "), destination=(" + destinationLat + "," + destinationLng + "), destinationName=" + destinationName);
        } else {
            Toast.makeText(this, "Missing ride details.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Missing ride details in intent");
            finish();
            return;
        }

        GeoPoint rideDestination = new GeoPoint(destinationLat, destinationLng);
        statusText.setText("Searching for nearby ambulances...");
        Log.d(TAG, "Starting ambulance search based on destination: " + rideDestination.getLatitude() + ", " + rideDestination.getLongitude());

        findNearbyAmbulances(rideDestination);
    }

    private void findNearbyAmbulances(GeoPoint rideDestination) {
        db.collection("availableDrivers")
                .whereEqualTo("available", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    nearbyAmbulances.clear();
                    Log.d(TAG, "Total available drivers fetched: " + queryDocumentSnapshots.size());

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String driverId = doc.getString("driverId");
                        GeoPoint driverDestination = doc.getGeoPoint("preferredDestination");
                        GeoPoint driverLocation = doc.getGeoPoint("location");

                        if (driverId == null) {
                            Log.w(TAG, "Skipping driver without driverId. Doc ID: " + doc.getId());
                            continue;
                        }

                        boolean matchesDirection = false;

                        if (driverLocation != null && driverDestination != null) {
                            // Compute bearing from driver location to driver's preferred destination
                            float bearingDriverToPreferredDest = bearingBetweenGeoPoints(driverLocation, driverDestination);

                            // Compute bearing from pickup location to ride destination
                            GeoPoint pickupLocation = new GeoPoint(pickupLat, pickupLng);
                            float bearingPickupToRideDest = bearingBetweenGeoPoints(pickupLocation, rideDestination);

                            // Calculate difference between bearings
                            float bearingDifference = Math.abs(bearingDriverToPreferredDest - bearingPickupToRideDest);
                            // Normalize difference to [0, 180]
                            if (bearingDifference > 180) {
                                bearingDifference = 360 - bearingDifference;
                            }

                            Log.d(TAG, "Driver " + driverId + " bearings: driverToPrefDest=" + bearingDriverToPreferredDest
                                    + ", pickupToRideDest=" + bearingPickupToRideDest + ", difference=" + bearingDifference);

                            // Check if difference is within threshold (e.g., 30 degrees)
                            matchesDirection = bearingDifference <= 30;
                        } else {
                            Log.w(TAG, "Driver " + driverId + " missing location or preferredDestination.");
                        }

                        if (matchesDirection) {
                            double distanceKm = (driverLocation != null)
                                    ? distanceBetween(driverLocation, rideDestination) / 1000.0
                                    : -1;

                            AmbulanceModel ambulance = new AmbulanceModel(driverId, driverLocation, distanceKm);
                            nearbyAmbulances.add(ambulance);
                            Log.d(TAG, "Driver " + driverId + " matched by direction and added.");
                        } else {
                            Log.d(TAG, "Driver " + driverId + " did not match direction and skipped.");
                        }
                    }

                    if (nearbyAmbulances.isEmpty()) {
                        statusText.setText("No available drivers found going in your direction.");
                        Log.d(TAG, "No ambulances to show.");
                    } else {
                        statusText.setText("Found ambulances going in your direction.");

                        Intent intent = new Intent(SearchAmbulanceActivity.this, BookingDetailsActivity.class);
                        intent.putParcelableArrayListExtra("ambulances", new ArrayList<>(nearbyAmbulances));

                        Bundle bundle = new Bundle();
                        bundle.putString("rideId", rideId);
                        bundle.putString("customerId", customerId);
                        bundle.putDouble("pickupLat", pickupLat);
                        bundle.putDouble("pickupLng", pickupLng);
                        bundle.putDouble("destinationLat", destinationLat);
                        bundle.putDouble("destinationLng", destinationLng);
                        bundle.putString("pickupLocationName", pickupLocationName);
                        bundle.putString("destinationName", destinationName);
                        bundle.putString("destinationAddress", destinationAddress);
                        bundle.putString("status", status);
                        bundle.putDouble("fare", fare);

                        intent.putExtras(bundle);
                        Log.d(TAG, "Passing matched drivers to BookingDetailsActivity");
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching available drivers: " + e.getMessage());
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    statusText.setText("Failed to find ambulances.");
                });
    }

    /**
     * Calculate bearing in degrees between two GeoPoints.
     * Result is in [0,360) degrees.
     */
    private float bearingBetweenGeoPoints(GeoPoint from, GeoPoint to) {
        float[] results = new float[1];
        Location.distanceBetween(
                from.getLatitude(), from.getLongitude(),
                to.getLatitude(), to.getLongitude(),
                results
        );
        // Android Location API also calculates bearing:
        // Use Location API to get bearing
        android.location.Location locFrom = new android.location.Location("");
        locFrom.setLatitude(from.getLatitude());
        locFrom.setLongitude(from.getLongitude());

        android.location.Location locTo = new android.location.Location("");
        locTo.setLatitude(to.getLatitude());
        locTo.setLongitude(to.getLongitude());

        return locFrom.bearingTo(locTo);
    }


    /**
     * Check if two GeoPoints are within a certain distance threshold (in meters)
     */
    private boolean isCloseEnough(GeoPoint a, GeoPoint b, float thresholdMeters) {
        float[] results = new float[1];
        Location.distanceBetween(
                a.getLatitude(), a.getLongitude(),
                b.getLatitude(), b.getLongitude(),
                results
        );
        return results[0] <= thresholdMeters;
    }

    private double distanceBetween(GeoPoint a, GeoPoint b) {
        float[] results = new float[1];
        Location.distanceBetween(
                a.getLatitude(), a.getLongitude(),
                b.getLatitude(), b.getLongitude(),
                results
        );
        return results[0];
    }
}
