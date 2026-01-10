package com.rental.model;

import java.time.LocalDate;

public class Stall {

    private String zoneName;
    private String stallId;
    private String size;
    private String status;
    private double dailyRate;

    // ===== booking range =====
    private LocalDate bookingStart;
    private LocalDate bookingEnd;

    // constructor เดิม (ไม่มี booking)
    public Stall(String zoneName, String stallId, String size,
                 String status, double dailyRate) {
        this(zoneName, stallId, size, status, dailyRate, null, null);
    }

    // constructor ใหม่ (รองรับช่วงวัน)
    public Stall(String zoneName, String stallId, String size,
                 String status, double dailyRate,
                 LocalDate bookingStart,
                 LocalDate bookingEnd) {

        this.zoneName = zoneName;
        this.stallId = stallId;
        this.size = size;
        this.status = status;
        this.dailyRate = dailyRate;
        this.bookingStart = bookingStart;
        this.bookingEnd = bookingEnd;
    }

    // ===== getters =====
    public String getZoneName() { return zoneName; }
    public String getStallId() { return stallId; }
    public String getSize() { return size; }
    public String getStatus() { return status; }
    public double getDailyRate() { return dailyRate; }

    public LocalDate getBookingStart() { return bookingStart; }
    public LocalDate getBookingEnd() { return bookingEnd; }
}
