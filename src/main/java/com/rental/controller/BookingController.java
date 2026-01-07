package com.rental.controller;

import com.google.gson.*;
import com.rental.database.SupabaseClient;
import com.rental.model.Tenant;
import com.rental.util.Session;
import com.rental.model.Booking;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

public class BookingController implements Initializable {

    @FXML private Label spaceIdLabel, sizeLabel, categoryLabel, dailyPriceLabel, monthlyPriceLabel, depositPriceLabel, errorLabel;
    @FXML private Label summarySpaceId, summaryDeposit, summaryRent, summaryTotal;
    @FXML private DatePicker startDatePicker, endDatePicker;
    @FXML private TextField fullNameField, emailField, phoneField;
    @FXML private CheckBox agreeCheckBox;
    @FXML private ChoiceBox<String> productTypeChoice;
    @FXML private VBox bookingForm;
    @FXML private Label noStallMessage;

    private final SupabaseClient supabaseClient = new SupabaseClient();
    private final Gson gson = new Gson();

    private double dailyRate = 0;
    private double monthlyRate = 0;
    private double depositRate = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ChangeListener<Object> listener = (ObservableValue<?> obs, Object oldVal, Object newVal) -> calculateSummary();
        startDatePicker.valueProperty().addListener(listener);
        endDatePicker.valueProperty().addListener(listener);

        // เริ่มต้น summary
        summaryRent.setText("0 ฿");
        summaryTotal.setText("0 ฿");
        summaryDeposit.setText("0 ฿");

        // เติมข้อมูลผู้เช่าอัตโนมัติ
        if (Session.isLoggedIn() && Session.getCurrentUser() instanceof Tenant tenant) {
            fullNameField.setText(tenant.getFullName());
            emailField.setText(tenant.getEmail());
            phoneField.setText(tenant.getPhone());
        }

        // ตรวจสอบว่ามี stallId หรือไม่
        if (spaceIdLabel.getText() == null || spaceIdLabel.getText().isEmpty()) {
            bookingForm.setVisible(false);
            noStallMessage.setVisible(true);
        }
    }

    // ==================== SET DATA STALL ====================
    public void setStallData(String stallId) {
        try {
            spaceIdLabel.setText(stallId);
            summarySpaceId.setText(stallId);

            String response = supabaseClient.selectWhere("stalls", "stall_id", stallId);
            JsonArray jsonArray = gson.fromJson(response, JsonArray.class);

            if (jsonArray.size() > 0) {
                JsonObject stall = jsonArray.get(0).getAsJsonObject();
                String size = stall.has("size") && !stall.get("size").isJsonNull() ? stall.get("size").getAsString() : "-";
                String category = stall.has("category_allowed") && !stall.get("category_allowed").isJsonNull() ? stall.get("category_allowed").getAsString() : "-";
                dailyRate = stall.has("daily_rate") && !stall.get("daily_rate").isJsonNull() ? stall.get("daily_rate").getAsDouble() : 0;
                monthlyRate = stall.has("monthly_rate") && !stall.get("monthly_rate").isJsonNull() ? stall.get("monthly_rate").getAsDouble() : 0;
                depositRate = stall.has("deposit_rate") && !stall.get("deposit_rate").isJsonNull() ? stall.get("deposit_rate").getAsDouble() : 0;

                sizeLabel.setText(size + " เมตร");
                categoryLabel.setText(category);
                dailyPriceLabel.setText(String.format("%.0f ฿/วัน", dailyRate));
                monthlyPriceLabel.setText(String.format("%.0f ฿/เดือน", monthlyRate));
                depositPriceLabel.setText(String.format("%.0f ฿", depositRate));

                bookingForm.setVisible(true);
                noStallMessage.setVisible(false);
            } else {
                if (errorLabel != null) errorLabel.setText("ไม่พบข้อมูลพื้นที่ " + stallId);
            }
        } catch (Exception e) {
            if (errorLabel != null) errorLabel.setText("เกิดข้อผิดพลาด: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== CALCULATE SUMMARY ====================
    private void calculateSummary() {
        LocalDate start = startDatePicker.getValue();
        LocalDate end = endDatePicker.getValue();
        if (start != null && end != null && !end.isBefore(start)) {
            long days = ChronoUnit.DAYS.between(start, end) + 1;
            double totalRent = (days >= 30) ? monthlyRate * (days / 30.0) : dailyRate * days;
            double deposit = (days >= 30) ? depositRate : 0;

            summaryRent.setText(String.format("%.0f ฿", totalRent));
            summaryDeposit.setText(String.format("%.0f ฿", deposit));
            summaryTotal.setText(String.format("%.0f ฿", totalRent + deposit));
        } else {
            summaryRent.setText("0 ฿");
            summaryDeposit.setText("0 ฿");
            summaryTotal.setText("0 ฿");
        }
    }

    // ==================== HANDLE BOOKING ====================
    @FXML
    public void handleBooking(ActionEvent event) {
        if (!agreeCheckBox.isSelected()) { errorLabel.setText("กรุณายินยอมข้อกำหนดก่อนยืนยันการจอง"); return; }
        if (!Session.isLoggedIn()) { errorLabel.setText("คุณต้องลงชื่อเข้าใช้ก่อนทำการจอง"); return; }

        String fullName = fullNameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        LocalDate startD = startDatePicker.getValue();
        LocalDate endD = endDatePicker.getValue();
        String spaceId = spaceIdLabel.getText();
        double totalRent = summaryRent.getText().isEmpty() ? 0 : Double.parseDouble(summaryRent.getText().replace(" ฿", ""));
        double deposit = summaryDeposit.getText().isEmpty() ? 0 : Double.parseDouble(summaryDeposit.getText().replace(" ฿", ""));
        int userId = Session.getCurrentUser().getId();

        String productType = productTypeChoice.getValue();
        if (productType == null || productType.isEmpty()) { errorLabel.setText("กรุณาเลือกประเภทสินค้า"); return; }
        if (startD == null || endD == null || endD.isBefore(startD)) { errorLabel.setText("กรุณาเลือกวันที่เริ่มต้นและสิ้นสุดให้ถูกต้อง"); return; }

        JsonObject bookingJson = new JsonObject();
        bookingJson.addProperty("user_id", userId);
        bookingJson.addProperty("stall_id", spaceId);
        bookingJson.addProperty("product_type", productType);
        bookingJson.addProperty("total_price", totalRent);
        bookingJson.addProperty("deposit_price", deposit);
        bookingJson.addProperty("full_name", fullName);
        bookingJson.addProperty("email", email);
        bookingJson.addProperty("phone", phone);
        bookingJson.addProperty("start_date", startD.toString());
        bookingJson.addProperty("end_date", endD.toString());
        bookingJson.addProperty("status", "pending");

        try {
            String result = supabaseClient.insert("bookings", bookingJson.toString());
            if (result == null) {
                showAlert(Alert.AlertType.ERROR, "ไม่สามารถบันทึกการจองได้ โปรดลองใหม่");
                return;
            }

            System.out.println("Raw result from Supabase: " + result);

            JsonElement jsonElement = JsonParser.parseString(result);

            // ==================== HANDLE RESPONSE ====================
            if (jsonElement.isJsonArray()) {
                JsonArray arr = jsonElement.getAsJsonArray();
                if (arr.size() > 0 && arr.get(0).getAsJsonObject().has("booking_id")) {
                    long generatedId = arr.get(0).getAsJsonObject().get("booking_id").getAsLong();
                    Booking booking = new Booking(userId, spaceId, productType, totalRent, deposit, fullName, email, phone, startD, endD);
                    booking.setBooking_id(generatedId);
                    showAlert(Alert.AlertType.INFORMATION, "จองพื้นที่สำเร็จ กรุณาชำระเงิน");
                    handleCancel(event);
                    return;
                }
                showAlert(Alert.AlertType.INFORMATION, "จองพื้นที่สำเร็จ (ไม่ได้คืน booking_id จาก Supabase)");
            } else if (jsonElement.isJsonObject()) {
                JsonObject obj = jsonElement.getAsJsonObject();
                if (obj.has("booking_id")) {
                    long generatedId = obj.get("booking_id").getAsLong();
                    Booking booking = new Booking(userId, spaceId, productType, totalRent, deposit, fullName, email, phone, startD, endD);
                    booking.setBooking_id(generatedId);
                    showAlert(Alert.AlertType.INFORMATION, "จองพื้นที่สำเร็จ กรุณาชำระเงิน");
                    handleCancel(event);
                } else if (obj.has("message")) {
                    String errorMessage = obj.get("message").getAsString();
                    showAlert(Alert.AlertType.ERROR, "ไม่สามารถบันทึกการจองได้: " + errorMessage);
                } else {
                    showAlert(Alert.AlertType.INFORMATION, "จองพื้นที่สำเร็จ กรุณาชำระเงิน");
                }
            } else {
                showAlert(Alert.AlertType.INFORMATION, "จองพื้นที่สำเร็จ กรุณาชำระเงิน");
            }

        } catch (JsonParseException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "เกิดข้อผิดพลาดในข้อมูล (ไม่ใช่ JSON ที่ถูกต้อง): " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            if (errorLabel != null) errorLabel.setText("เกิดข้อผิดพลาด: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "เกิดข้อผิดพลาดในการบันทึกการจอง: " + e.getMessage());
        }
    }

    // ==================== CANCEL / CLEAR UI ====================
    @FXML
    public void handleCancel(ActionEvent event) {
        if (startDatePicker != null) startDatePicker.setValue(null);
        if (endDatePicker != null) endDatePicker.setValue(null);
        if (fullNameField != null) fullNameField.clear();
        if (emailField != null) emailField.clear();
        if (phoneField != null) phoneField.clear();
        if (productTypeChoice != null) productTypeChoice.getSelectionModel().clearSelection();
        if (errorLabel != null) errorLabel.setText("");

        // รีเซ็ต summary
        summaryRent.setText("0 ฿");
        summaryDeposit.setText("0 ฿");
        summaryTotal.setText("0 ฿");
    }

    private void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(null);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
