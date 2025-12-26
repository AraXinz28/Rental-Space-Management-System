package com.rental.model;

import java.io.File;
import java.time.LocalDate;

public class Payment {

    // ===== Composition reference =====
    private Booking booking;

    private double amount;
    private String paymentType;
    private LocalDate paymentDate;
    private String note;
    private File proofFile;
    private String proofUrl;

    // ไม่ให้สร้าง Payment เดี่ยว ๆ
    public Payment(Booking booking) {
        this.booking = booking;
        this.amount = booking.getTotal_price() + booking.getDeposit_price();
    }

    // ===== Business Logic =====
    public boolean validatePayment() {
        return booking != null
            && paymentType != null && !paymentType.isBlank()
            && paymentDate != null
            && amount > 0
            && proofFile != null;
    }

    // ===== Getters / Setters =====
    public double getAmount() { return amount; }

    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public File getProofFile() { return proofFile; }
    public void setProofFile(File proofFile) { this.proofFile = proofFile; }

    public String getProofUrl() { return proofUrl; }
    public void setProofUrl(String proofUrl) { this.proofUrl = proofUrl; }

    public Booking getBooking() { return booking; }
}