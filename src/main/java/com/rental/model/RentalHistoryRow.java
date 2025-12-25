package com.rental.model;

import javafx.beans.property.*;
import java.time.LocalDate;

public class RentalHistoryRow {

    private final LongProperty bookingId = new SimpleLongProperty(); // ⭐ เพิ่ม id
    private final StringProperty customerName = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> date = new SimpleObjectProperty<>();
    private final StringProperty status = new SimpleStringProperty();

    // constructor หลัก (ใช้กับ Supabase)
    public RentalHistoryRow(long bookingId, String customerName, LocalDate date, String status) {
        this.bookingId.set(bookingId);
        this.customerName.set(customerName);
        this.date.set(date);
        this.status.set(status);
    }

    // constructor เก่า (กันโค้ดเก่าพัง)
    public RentalHistoryRow(String customerName, LocalDate date, String status) {
        this(0, customerName, date, status);
    }

    // getters
    public long getBookingId() {
        return bookingId.get();
    }

    public String getCustomerName() {
        return customerName.get();
    }

    public LocalDate getDate() {
        return date.get();
    }

    public String getStatus() {
        return status.get();
    }

    // properties
    public LongProperty bookingIdProperty() {
        return bookingId;
    }

    public StringProperty customerNameProperty() {
        return customerName;
    }

    public ObjectProperty<LocalDate> dateProperty() {
        return date;
    }

    public StringProperty statusProperty() {
        return status;
    }
}
