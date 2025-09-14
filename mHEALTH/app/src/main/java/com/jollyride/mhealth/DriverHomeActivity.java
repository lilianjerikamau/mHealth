package com.jollyride.mhealth;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.jollyride.mhealth.helper.AutoAssignService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DriverHomeActivity extends BaseActivity {

    private AutoCompleteTextView preferredDestinationInput;
    private Button goOnlineButton;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private PlacesClient placesClient;
    private FirebaseFirestore db;
    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_home);

        preferredDestinationInput = findViewById(R.id.preferred_destination);
        goOnlineButton = findViewById(R.id.go_online_button);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        placesClient = Places.createClient(this);
        db = FirebaseFirestore.getInstance();
        driverId = FirebaseAuth.getInstance().getUid();

        goOnlineButton.setOnClickListener(v -> {
            String destName = preferredDestinationInput.getText().toString().trim();
            Log.d("DRIVER_HOME", "Go Online button clicked. Destination input: '" + destName + "'");

            if (destName.isEmpty()) {
                Toast.makeText(this, "Please enter a destination", Toast.LENGTH_SHORT).show();
                return;
            }

            getLatLngFromPlaceName(destName, (latLng, name, address) -> {
                if (latLng != null) {
                    String finalName = name != null ? name : destName;
                    saveDriverAvailability(latLng, finalName);

                    Toast.makeText(this, "You're now online. Listening for ride requests...", Toast.LENGTH_SHORT).show();

                    listenForRideRequests();

                } else {
                    Toast.makeText(this, "Could not resolve destination", Toast.LENGTH_SHORT).show();
                }
            });
        });

        listenForRideRequests();

    }

    private void listenForRideRequests() {
        db.collection("drivers")
                .document(driverId)
                .collection("rideRequests")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e("DRIVER_HOME", "Error listening for ride requests: ", error);
                        return;
                    }

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);

                        String rideId = doc.getString("rideId");
                        String pickupLocationName = doc.getString("pickupLocationName");
                        String destinationName = doc.getString("destinationName");
                        String destinationAddress = doc.getString("destinationAddress");
                        Double fare = doc.getDouble("fare");

                        Log.d("DRIVER_HOME", "New ride request detected: " + rideId);
                        Double pickupLat = doc.getDouble("pickupLat");
                        Double pickupLng = doc.getDouble("pickupLng");

                        Intent intent = new Intent(this, DriverAcceptRideActivity.class);
                        intent.putExtra("rideId", rideId);
                        intent.putExtra("pickupLocationName", pickupLocationName);
                        intent.putExtra("destinationName", destinationName);
                        intent.putExtra("destinationAddress", destinationAddress);
                        intent.putExtra("fare", fare);
                        intent.putExtra("pickupLat", pickupLat);
                        intent.putExtra("pickupLng", pickupLng);
                        startActivity(intent);
                    }
                });
    }


    /**
     * Resolves a destination name into LatLng and a readable place name/address.
     */
    private void getLatLngFromPlaceName(String name, OnPlaceResolved callback) {
        FindAutocompletePredictionsRequest request =
                FindAutocompletePredictionsRequest.builder().setQuery(name).build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    if (!response.getAutocompletePredictions().isEmpty()) {
                        String placeId = response.getAutocompletePredictions().get(0).getPlaceId();
                        List<Place.Field> fields = Arrays.asList(
                                Place.Field.LAT_LNG,
                                Place.Field.NAME,
                                Place.Field.ADDRESS
                        );
                        FetchPlaceRequest placeRequest = FetchPlaceRequest.newInstance(placeId, fields);

                        placesClient.fetchPlace(placeRequest)
                                .addOnSuccessListener(placeResponse -> {
                                    Place place = placeResponse.getPlace();
                                    callback.onResolved(
                                            place.getLatLng(),
                                            place.getName(),
                                            place.getAddress()
                                    );
                                })
                                .addOnFailureListener(e -> {
                                    e.printStackTrace();
                                    callback.onResolved(null, null, null);
                                });
                    } else {
                        callback.onResolved(null, null, null);
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.onResolved(null, null, null);
                });
    }

    /**
     * Saves the driver's availability with their current location and preferred destination.
     */
    private void saveDriverAvailability(LatLng preferredLatLng, String destinationName) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        GeoPoint driverLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                        GeoPoint preferredDestination = new GeoPoint(preferredLatLng.latitude, preferredLatLng.longitude);

                        Map<String, Object> driverData = new HashMap<>();
                        driverData.put("driverId", driverId);
                        driverData.put("location", driverLocation);
                        driverData.put("preferredDestination", preferredDestination);
                        driverData.put("preferredDestinationName", destinationName);
                        driverData.put("available", true);

                        db.collection("availableDrivers")
                                .document(driverId)
                                .set(driverData)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "You are now online", Toast.LENGTH_SHORT).show();
                                    AutoAssignService.assignRideToDriver(driverId, driverLocation, preferredDestination);
                                })
                                .addOnFailureListener(e -> {
                                    e.printStackTrace();
                                    Toast.makeText(this, "Error going online: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this, "Current location unavailable", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    interface OnPlaceResolved {
        void onResolved(LatLng latLng, String name, String address);
    }
}
