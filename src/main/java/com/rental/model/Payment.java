package com.rental.model;

import java.io.File;
import java.time.LocalDate;


public class Payment {
    private String areaCode;
    private String productType;
    private double amount;
    private String paymentType;
    private LocalDate paymentDate;
    private String note;
    private File proofFile;
    private String proofUrl;

    public boolean validatePayment() {
        return areaCode != null && !areaCode.isBlank()
            && paymentType != null && !paymentType.isBlank()
            && paymentDate != null
            && amount > 0
            && proofFile != null;
    }

    // Getters and setters...
    public String getAreaCode() { return areaCode; }
    public void setAreaCode(String areaCode) { this.areaCode = areaCode; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
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
}