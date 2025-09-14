package com.jollyride.mhealth;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RiderHomeActivity extends BaseActivity {

    private GestureDetector gestureDetector;
    FusedLocationProviderClient fusedLocationProviderClient;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_home);
        Log.d("RIDER_HOME", "onCreate: Layout set.");

        this.getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        Intent intent = new Intent(RiderHomeActivity.this, SignInActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                        finish();
                    }
                });

        gestureDetector = new GestureDetector(this, new RiderHomeActivity.SwipeGestureListener());

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        PlacesClient placesClient = Places.createClient(this);
        db = FirebaseFirestore.getInstance();

        AutoCompleteTextView destinationInput = findViewById(R.id.destination_input);
        PlacesAutoCompleteAdapter adapter = new PlacesAutoCompleteAdapter(this);
        destinationInput.setAdapter(adapter);
        destinationInput.setThreshold(1);

        destinationInput.setOnItemClickListener((parent, view, position, id) -> {
            AutocompletePrediction item = adapter.getItem(position);

            if (item != null) {
                destinationInput.setText(item.getFullText(null));
                String placeId = item.getPlaceId();

                List<Place.Field> placeFields = Arrays.asList(
                        Place.Field.LAT_LNG,
                        Place.Field.NAME,
                        Place.Field.ADDRESS
                );
                FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();

                placesClient.fetchPlace(request).addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    LatLng destinationLatLng = place.getLatLng();

                    fusedLocationProviderClient.getLastLocation()
                            .addOnSuccessListener(location -> {
                                if (location != null && destinationLatLng != null) {
                                    GeoPoint pickupLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                                    GeoPoint selectedDestination = new GeoPoint(destinationLatLng.latitude, destinationLatLng.longitude);

                                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                    Map<String, Object> ride = new HashMap<>();
                                    ride.put("customerId", userId);
                                    ride.put("pickupLocation", pickupLocation);
                                    ride.put("destination", selectedDestination);
                                    ride.put("pickupLocationName", getPickupLocationName(pickupLocation));
                                    ride.put("destinationName", place.getName());
                                    ride.put("destinationAddress", place.getAddress());
                                    ride.put("status", "requested");
                                    ride.put("timestamp", FieldValue.serverTimestamp());
                                    ride.put("fare", calculateFare(pickupLocation, selectedDestination));

                                    db.collection("rides").add(ride)
                                            .addOnSuccessListener(documentReference -> {
                                                String rideId = documentReference.getId();
                                                Log.d("RIDER_HOME", "Ride saved successfully. ID: " + rideId);

                                                // ✅ Navigate to SearchAmbulanceActivity
                                                Intent intent = new Intent(RiderHomeActivity.this, SearchAmbulanceActivity.class);

                                                // ✅ Pass ALL ride details forward
                                                Bundle bundle = new Bundle();
                                                bundle.putString("rideId", rideId);
                                                bundle.putString("customerId", userId);
                                                bundle.putDouble("pickupLat", pickupLocation.getLatitude());
                                                bundle.putDouble("pickupLng", pickupLocation.getLongitude());
                                                bundle.putDouble("destinationLat", selectedDestination.getLatitude());
                                                bundle.putDouble("destinationLng", selectedDestination.getLongitude());
                                                bundle.putString("pickupLocationName", getPickupLocationName(pickupLocation));
                                                bundle.putString("destinationName", place.getName());
                                                bundle.putString("destinationAddress", place.getAddress());
                                                bundle.putString("status", "requested");
                                                bundle.putDouble("fare", calculateFare(pickupLocation, selectedDestination));

                                                intent.putExtras(bundle);

                                                startActivity(intent);
                                                overridePendingTransition(R.anim.slide_out_right, R.anim.slide_in_left);
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.d("RIDER_HOME", "Failed to save ride: " + e.getMessage());
                                                Toast.makeText(this, "Failed to save ride: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    Toast.makeText(this, "Could not get current location or destination.", Toast.LENGTH_SHORT).show();
                                }
                            });
                });
            }
        });

    }


    private String getPickupLocationName(GeoPoint location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            } else {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private double calculateFare(GeoPoint pickup, GeoPoint destination) {
        float[] result = new float[1];
        Location.distanceBetween(
                pickup.getLatitude(), pickup.getLongitude(),
                destination.getLatitude(), destination.getLongitude(),
                result
        );
        float distanceInMeters = result[0];
        double baseFare = 2.0;
        double perKm = 1.5;

        return baseFare + (distanceInMeters / 1000) * perKm;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onSwipeRight();
                    } else {
                        onSwipeLeft();
                    }
                    return true;
                }
            }
            return false;
        }
    }

    private void onSwipeLeft() {
        Intent intent = new Intent(RiderHomeActivity.this, RiderHomeActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void onSwipeRight() {
        Intent intent = new Intent(RiderHomeActivity.this, SignInActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}
