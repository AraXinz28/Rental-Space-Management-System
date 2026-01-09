package com.rental.model;

import java.time.LocalDateTime;

public class Stall {

    private String zoneName;
    private String stallId;
    private String size;
    private String status;
    private double dailyRate;

    
    private LocalDateTime bookingDate;

    // constructor เดิม 
    public Stall(String zoneName, String stallId, String size,
                 String status, double dailyRate) {
        this(zoneName, stallId, size, status, dailyRate, null);
    }

    // constructor ใหม่ (มีวันที่)
    public Stall(String zoneName, String stallId, String size,
                 String status, double dailyRate,
                 LocalDateTime bookingDate) {

        this.zoneName = zoneName;
        this.stallId = stallId;
        this.size = size;
        this.status = status;
        this.dailyRate = dailyRate;
        this.bookingDate = bookingDate;
    }

    // ===== getters =====
    public String getZoneName() { return zoneName; }
    public String getStallId() { return stallId; }
    public String getSize() { return size; }
    public String getStatus() { return status; }
    public double getDailyRate() { return dailyRate; }
    public LocalDateTime getBookingDate() { return bookingDate; }
}
