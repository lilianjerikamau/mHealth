package com.jollyride.mhealth.adapter;

public class RideItem {

    private String customerId;
    private String destinationName;
    private String destinationAddress;

    public RideItem(String customerId, String destinationName, String destinationAddress) {
        this.customerId = customerId;
        this.destinationName = destinationName;
        this.destinationAddress = destinationAddress;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }
}

