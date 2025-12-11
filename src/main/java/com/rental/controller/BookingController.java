package com.rental.controller;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

public class BookingController {

    @FXML
    private Label spaceIdLabel, zoneLabel, priceLabel, sizeLabel, errorLabel;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private TextField daysField;

    @FXML
    public void handleUserButton(ActionEvent event) {
        System.out.println("User button clicked!");
    }

    @FXML
    public void handleBooking(ActionEvent event) {
        System.out.println("Booking confirmed!");
    }

    // 🔥 สำคัญ — method ที่ FXML ต้องการ แต่เดิมไม่มี
    @FXML
    public void handleCancel(ActionEvent event) {
        System.out.println("Cancel clicked!");

        // ใส่อะไรก็ได้ เช่น clear field
        if (startDatePicker != null) startDatePicker.setValue(null);
        if (daysField != null) daysField.clear();
        if (errorLabel != null) errorLabel.setText("");
    }
}
