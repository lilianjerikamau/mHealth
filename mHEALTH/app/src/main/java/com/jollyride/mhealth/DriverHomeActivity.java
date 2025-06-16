package com.jollyride.mhealth;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
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
import com.google.firebase.firestore.FieldValue;
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
        placesClient = Places.createClient(this);
        db = FirebaseFirestore.getInstance();
        driverId = FirebaseAuth.getInstance().getUid();

        goOnlineButton.setOnClickListener(v -> {
            String destName = preferredDestinationInput.getText().toString();
            if (destName.isEmpty()) {
                Toast.makeText(this, "Enter destination", Toast.LENGTH_SHORT).show();
                return;
            }

            getLatLngFromPlaceName(destName, (latLng) -> {
                if (latLng != null) {
                    saveDriverAvailability(latLng);

                    Intent intent = new Intent(this, DriverAcceptRideActivity.class);
                    startActivity(intent);

                } else {
                    Toast.makeText(this, "Invalid destination", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void getLatLngFromPlaceName(String name, OnLatLngResolved callback) {
        FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                .setQuery(name)
                .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener(response -> {
                    if (!response.getAutocompletePredictions().isEmpty()) {
                        String placeId = response.getAutocompletePredictions().get(0).getPlaceId();
                        List<Place.Field> fields = Arrays.asList(Place.Field.LAT_LNG);
                        FetchPlaceRequest placeRequest = FetchPlaceRequest.newInstance(placeId, fields);

                        placesClient.fetchPlace(placeRequest)
                                .addOnSuccessListener(placeResponse -> {
                                    LatLng latLng = placeResponse.getPlace().getLatLng();
                                    callback.onResolved(latLng);
                                });
                    } else {
                        callback.onResolved(null);
                    }
                });
    }

    private void saveDriverAvailability(LatLng preferredLatLng) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        GeoPoint driverLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                        GeoPoint preferredDestination = new GeoPoint(preferredLatLng.latitude, preferredLatLng.longitude);

                        Map<String, Object> driver = new HashMap<>();
                        driver.put("driverId", driverId);
                        driver.put("location", driverLocation);
                        driver.put("preferredDestination", preferredDestination);
                        driver.put("available", true);

                        db.collection("availableDrivers").document(driverId).set(driver)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Now Online", Toast.LENGTH_SHORT).show();
                                    AutoAssignService.assignRideToDriver(driverId, driverLocation, preferredDestination);
                                });
                    }
                });
    }

    interface OnLatLngResolved {
        void onResolved(LatLng latLng);
    }

}