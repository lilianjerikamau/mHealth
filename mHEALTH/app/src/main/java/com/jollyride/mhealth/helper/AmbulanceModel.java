package com.jollyride.mhealth.helper;


import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.GeoPoint;

public class AmbulanceModel implements Parcelable {
    private String driverId;
    private GeoPoint location;
    private double distanceKm;

    public AmbulanceModel(String driverId, GeoPoint location, double distanceKm) {
        this.driverId = driverId;
        this.location = location;
        this.distanceKm = distanceKm;
    }

    protected AmbulanceModel(Parcel in) {
        driverId = in.readString();
        double lat = in.readDouble();
        double lng = in.readDouble();
        location = new GeoPoint(lat, lng);
        distanceKm = in.readDouble();
    }

    public static final Creator<AmbulanceModel> CREATOR = new Creator<AmbulanceModel>() {
        @Override
        public AmbulanceModel createFromParcel(Parcel in) {
            return new AmbulanceModel(in);
        }

        @Override
        public AmbulanceModel[] newArray(int size) {
            return new AmbulanceModel[size];
        }
    };

    public String getDriverId() { return driverId; }
    public GeoPoint getLocation() { return location; }
    public double getDistanceKm() { return distanceKm; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(driverId);
        dest.writeDouble(location.getLatitude());
        dest.writeDouble(location.getLongitude());
        dest.writeDouble(distanceKm);
    }
}

