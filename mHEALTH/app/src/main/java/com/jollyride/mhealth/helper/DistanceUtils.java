package com.jollyride.mhealth.helper;


import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

public class DistanceUtils {

    /**
     * Estimates travel time in minutes using straight-line distance and average speed.
     * Assumes average speed of 40 km/h.
     */
    public static int calculateMinutesAway(LatLng from, LatLng to) {
        float[] results = new float[1];
        Location.distanceBetween(
                from.latitude, from.longitude,
                to.latitude, to.longitude,
                results
        );
        double distanceKm = results[0] / 1000.0;
        double minutes = (distanceKm / 40.0) * 60.0;
        return (int) Math.max(1, Math.round(minutes));
    }
}

