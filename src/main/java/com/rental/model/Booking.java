package com.rental.model;

import java.time.LocalDate;

public class Booking {
    private transient long booking_id; // ไม่ serialize ลง JSON
    private int user_id;
    private String stall_id;
    private String product_type;
    private double total_price;
    private double deposit_price;
    private String start_date;
    private String end_date;
    private String full_name;
    private String email;
    private String phone;
    private String status;

    // Composition
    private transient Payment payment;

    // ===== Constructor =====
    public Booking(int userId,
                   String stallId,
                   String productType,
                   double totalPrice,
                   double depositPrice,
                   String fullName,
                   String email,
                   String phone,
                   LocalDate startDate,
                   LocalDate endDate) {

        this.user_id = userId;
        this.stall_id = stallId;
        this.product_type = productType != null ? productType : "";
        this.total_price = totalPrice;
        this.deposit_price = depositPrice;
        this.start_date = startDate.toString();
        this.end_date = endDate.toString();
        this.full_name = fullName;
        this.email = email;
        this.phone = phone;
        this.status = "pending";

        // Composition
        this.payment = new Payment(this);
    }

    // ===== Business Logic =====
    public boolean isPaid() { return "paid".equalsIgnoreCase(status); }
    public void markPaid() { this.status = "paid"; }

    // ===== Getters & Setters =====
    public long getBooking_id() { return booking_id; }
    public void setBooking_id(long booking_id) { this.booking_id = booking_id; }

    public int getUser_id() { return user_id; }
    public String getStall_id() { return stall_id; }
    public String getProduct_type() { return product_type; }
    public double getTotal_price() { return total_price; }
    public double getDeposit_price() { return deposit_price; }
    public String getStart_date() { return start_date; }
    public String getEnd_date() { return end_date; }
    public String getFull_name() { return full_name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getStatus() { return status; }
    public Payment getPayment() { return payment; }
}
