package com.rental.model;

public class Stall {

    private String zoneName;
    private String stallId;
    private String size;
    private String status;
    private double dailyRate;   
    private String amenities; 

    public Stall(String zoneName, String stallId, String size, String status,
             double dailyRate,
             String amenities) {
        this.zoneName = zoneName;
        this.stallId = stallId;
        this.size = size;
        this.status = status; 
        this.dailyRate = dailyRate;
        this.amenities = amenities; 
    }

    public String getZoneName() {
        return zoneName;
    }

    public String getStallId() {
        return stallId;
    }

    public String getSize() {
        return size;
    }

    public String getStatus() {
        return status;
    }

    public double getDailyRate() {
        return dailyRate;
    }

    public String getAmenities() {
        return amenities;
    }
}
