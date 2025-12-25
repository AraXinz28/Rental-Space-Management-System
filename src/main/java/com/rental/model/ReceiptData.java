package com.rental.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReceiptData {
    public long bookingId;
    public String fullName;
    public String phone;
    public String stallId; // เช่น A14 / A-14
    public String zone; // ถอดจาก stallId
    public String lockNo; // ถอดจาก stallId
    public String productType;
    public String status;
    public LocalDate startDate;
    public LocalDate endDate;

    public BigDecimal deposit = BigDecimal.ZERO;
    public BigDecimal total = BigDecimal.ZERO;

    public String paymentMethod = "โอนผ่านธนาคาร"; // ถ้า DB ไม่มี ก็ใส่ค่า default
}
