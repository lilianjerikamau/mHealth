package com.jollyride.mhealth;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RiderHomeActivity extends BaseActivity implements OnMapReadyCallback, DirectionsTask.DirectionsCallback {

    private static final String RIDER_TAG = "====|RIDER|====>";
    private GestureDetector gestureDetector;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    //MaterialButton requestRideBtn;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private String userId;
    private boolean locationPermissionGranted = false;
    private GeoPoint selectedDestination;
    private TextInputLayout destinationInputLayout;
    private AutoCompleteTextView destinationInput;
    private PlaceAutocompleteAdapter adapter;
    private DirectionsHelper directionsHelper;
    private DirectionsTask directionsTask;
    private Polyline currentRoutePolyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_home);

        Log.e(RIDER_TAG,"INITIALIZING MAP...");

        // Verify Play Services
        checkPlayServices();

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        // Initialize DirectionsHelper with your API key
        directionsHelper = new DirectionsHelper(this, getString(R.string.google_maps_key));
        // Initialize DirectionsTask with callback
        directionsTask = new DirectionsTask(this, getString(R.string.google_maps_key), this);




        // Setup destination input
        destinationInputLayout = findViewById(R.id.destination_input_layout);
        destinationInput = findViewById(R.id.destination_input);

        adapter = new PlaceAutocompleteAdapter(this);
        destinationInput.setAdapter(adapter);
        destinationInput.setThreshold(1); // Start showing suggestions after 1 character

        destinationInput.setOnItemClickListener((parent, view, position, id) -> {
            AutocompletePrediction prediction = adapter.getItem(position);
            if (prediction != null) {
                fetchPlaceDetails(prediction.getPlaceId());
            }
        });


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

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check for location permissions
        checkLocationPermission();

        // Add this to your activity's onItemClickListener
        destinationInput.setOnItemClickListener((parent, view, position, id) -> {
            AutocompletePrediction prediction = adapter.getItem(position);
            if (prediction != null) {
                // Display only the primary text in the input field
                destinationInput.setText(prediction.getPrimaryText(null));
                fetchPlaceDetails(prediction.getPlaceId());
            }
        });

        // Set up request ride button
        /*requestRideBtn = findViewById(R.id.requestRideBtn);
        requestRideBtn.setOnClickListener(v -> {
            if (locationPermissionGranted) {
                requestRide();
            } else {
                checkLocationPermission();
            }
        });*/
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    public void onDirectionsReady(List<LatLng> points) {
        runOnUiThread(() -> {
            if (points == null || points.isEmpty()) {
                Toast.makeText(RiderHomeActivity.this,
                        "No route points received", Toast.LENGTH_SHORT).show();
                return;
            }

            // Remove previous polyline if exists
            if (currentRoutePolyline != null) {
                currentRoutePolyline.remove();
            }

            // Draw new route with more visible parameters
            currentRoutePolyline = mMap.addPolyline(new PolylineOptions()
                    .addAll(points)
                    .width(15f) // Increased width for better visibility
                    .color(Color.parseColor("#1a73e8")) // Google Maps blue color
                    .zIndex(1) // Ensure it appears above other map elements
                    .geodesic(true));

            // Add start and end markers for reference
            mMap.addMarker(new MarkerOptions()
                    .position(points.get(0))
                    .title("Start")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

            mMap.addMarker(new MarkerOptions()
                    .position(points.get(points.size() - 1))
                    .title("End")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

            // Zoom to show entire route
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng point : points) {
                builder.include(point);
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
        });
    }

    @Override
    public void onFailure(Exception e) {
        runOnUiThread(() -> {
            if (e.getMessage().contains("REQUEST_DENIED")) {
                // More specific error message for API key issues
                Toast.makeText(RiderHomeActivity.this,
                        "API key error. Check console.cloud.google.com",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(RiderHomeActivity.this,
                        "Directions failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
            Log.e("Directions", "Error: " + e.getMessage());
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (directionsTask != null) {
            directionsTask.shutdown();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                getCurrentLocation();
            } else {
                //Toast.makeText(this, "Location permission is required to use this feature", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (locationPermissionGranted) {
            getCurrentLocation();
        }
    }

    private class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float diffX = e2.getX() - e1.getX();
            float diffY = e2.getY() - e1.getY();

            if (Math.abs(diffX) > Math.abs(diffY)) {
                // Horizontal swipe
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onSwipeRight();
                    } else {
                        onSwipeLeft();
                    }
                    return true;
                }
            } else {
                // Vertical swipe
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        //onSwipeDown();
                    } else {
                        //onSwipeUp();
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

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
            getCurrentLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }



    private void checkPlayServices() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int code = api.isGooglePlayServicesAvailable(this);
        if (code != ConnectionResult.SUCCESS) {
            api.showErrorNotification(this, code);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                        mMap.addMarker(new MarkerOptions().position(currentLatLng).title("Your Location"));

                        // Update user's location in Firestore
                        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        db.collection("users").document(userId)
                                .update("currentLocation", geoPoint);
                    }
                });
    }

    private void fetchPlaceDetails(String placeId) {
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.LAT_LNG,
                Place.Field.NAME,
                Place.Field.ADDRESS
        );

        FetchPlaceRequest request = FetchPlaceRequest.newInstance(placeId, placeFields);
        PlacesClient placesClient = Places.createClient(this);

        placesClient.fetchPlace(request)
                .addOnSuccessListener(response -> {
                    Place place = response.getPlace();
                    LatLng destinationLatLng = place.getLatLng();

                    if (destinationLatLng != null) {
                        // 1. Update UI
                        destinationInput.setText(place.getName());
                        selectedDestination = new GeoPoint(destinationLatLng.latitude, destinationLatLng.longitude);

                        // 2. Clear previous map markers and polylines
                        mMap.clear();

                        // 3. Add destination marker
                        mMap.addMarker(new MarkerOptions()
                                .position(destinationLatLng)
                                .title(place.getName())
                                .snippet(place.getAddress()));

                        // 4. Get current location and draw route
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {

                            fusedLocationClient.getLastLocation()
                                    .addOnSuccessListener(location -> {
                                        if (location != null) {
                                            LatLng origin = new LatLng(location.getLatitude(), location.getLongitude());

                                            // Add origin marker
                                            mMap.addMarker(new MarkerOptions()
                                                    .position(origin)
                                                    .title("Your Location")
                                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                                            // Draw the route
                                            drawRoute(origin, destinationLatLng);

                                            // Zoom to show both markers
                                            LatLngBounds bounds = new LatLngBounds.Builder()
                                                    .include(origin)
                                                    .include(destinationLatLng)
                                                    .build();
                                            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                                        }
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to get place details", Toast.LENGTH_SHORT).show();
                    Log.e("Places", "Error: " + e.getMessage());
                });
    }
    private void drawRoute(LatLng origin, LatLng destination) {
        // Initialize DirectionsTask with proper callback
        DirectionsTask task = new DirectionsTask(this, getString(R.string.google_maps_key),
                new DirectionsTask.DirectionsCallback() {
                    @Override
                    public void onDirectionsReady(List<LatLng> points) {
                        runOnUiThread(() -> {
                            if (points != null && !points.isEmpty()) {
                                // Remove previous polyline if exists
                                if (currentRoutePolyline != null) {
                                    currentRoutePolyline.remove();
                                }

                                // Draw new route
                                currentRoutePolyline = mMap.addPolyline(new PolylineOptions()
                                        .addAll(points)
                                        .width(12f)
                                        .color(Color.BLUE) // Or use resource color
                                        .geodesic(true));

                                // Zoom to show entire route (optional)
                                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                for (LatLng point : points) {
                                    builder.include(point);
                                }
                                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50));
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> {
                            Toast.makeText(RiderHomeActivity.this,
                                    "Directions failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });

        task.execute(origin, destination);
    }

    private void requestRide() {
        if (selectedDestination == null) {
            Toast.makeText(this, "Please select a destination", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!locationPermissionGranted) {
            checkLocationPermission();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        GeoPoint pickupLocation = new GeoPoint(location.getLatitude(), location.getLongitude());

                        // Create ride request
                        Map<String, Object> ride = new HashMap<>();
                        ride.put("customerId", userId);
                        ride.put("pickupLocation", pickupLocation);
                        ride.put("destination", selectedDestination);
                        ride.put("status", "requested");
                        ride.put("timestamp", FieldValue.serverTimestamp());
                        ride.put("fare", calculateFare(pickupLocation, selectedDestination));

                        db.collection("rides").add(ride)
                                .addOnSuccessListener(documentReference -> {
                                    listenForDriverAssignment(documentReference.getId());
                                });
                    }
                });
    }

    // Places Autocomplete Adapter
    class PlacesAutocompleteAdapter extends ArrayAdapter<AutocompletePrediction> {
        public PlacesAutocompleteAdapter(Context context) {
            super(context, android.R.layout.simple_dropdown_item_1line);
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    if (constraint != null) {
                        List<AutocompletePrediction> predictions = getPredictions(constraint);
                        results.values = predictions;
                        results.count = predictions.size();
                    }
                    return results;
                }

                private List<AutocompletePrediction> getPredictions(CharSequence constraint) {
                    PlacesClient placesClient = Places.createClient(RiderHomeActivity.this);
                    AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

                    FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                            .setQuery(constraint.toString())
                            .setSessionToken(token)
                            .build();

                    try {
                        Task<FindAutocompletePredictionsResponse> response = placesClient.findAutocompletePredictions(request);
                        return Tasks.await(response).getAutocompletePredictions();
                    } catch (Exception e) {
                        return new ArrayList<>();
                    }
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        setNotifyOnChange(false);
                        clear();
                        addAll((List<AutocompletePrediction>) results.values);
                        notifyDataSetChanged();
                    }
                }
            };
        }
    }

    private void listenForDriverAssignment(String rideId) {
        db.collection("rides").document(rideId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("RideRequest", "Listen failed", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        String driverId = documentSnapshot.getString("driverId");

                        if (status != null && status.equals("accepted") && driverId != null) {
                            // Driver found, show driver info
                            showDriverInfo(driverId);

                            // Optional: You might want to remove the listener here
                            // if you only need to be notified once about driver assignment
                        }
                    }
                });
    }

    private void showDriverInfo(String driverId) {
        // Fetch driver details from Firestore
        db.collection("users").document(driverId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String driverName = documentSnapshot.getString("name");
                        String driverPhone = documentSnapshot.getString("phone");
                        GeoPoint driverLocation = documentSnapshot.getGeoPoint("currentLocation");

                        // Create and show a dialog with driver info
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Driver Assigned");
                        builder.setMessage("Driver: " + driverName + "\nPhone: " + driverPhone);

                        // Add a map button to view driver location
                        builder.setPositiveButton("View on Map", (dialog, which) -> {
                            showDriverOnMap(driverLocation);
                        });

                        builder.setNegativeButton("OK", null);
                        builder.show();
                    }
                });
    }

    private void showDriverOnMap(GeoPoint driverLocation) {
        if (driverLocation == null) return;

        LatLng driverLatLng = new LatLng(driverLocation.getLatitude(), driverLocation.getLongitude());
        mMap.clear();

        // Add driver marker
        mMap.addMarker(new MarkerOptions()
                .position(driverLatLng)
                .title("Your Driver")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        // Add pickup location marker (if available)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            LatLng pickupLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                            mMap.addMarker(new MarkerOptions()
                                    .position(pickupLatLng)
                                    .title("Pickup Location"));

                            // Zoom to show both locations
                            LatLngBounds.Builder builder = new LatLngBounds.Builder();
                            builder.include(driverLatLng);
                            builder.include(pickupLatLng);
                            LatLngBounds bounds = builder.build();

                            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
                        }
                    });
        }
    }

    private double calculateFare(GeoPoint pickup, GeoPoint destination) {
        // Implement fare calculation logic
        return 10.0; // example fare
    }

}