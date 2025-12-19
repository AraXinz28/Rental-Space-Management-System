package com.rental.controller;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rental.database.SupabaseClient;
import com.rental.model.Tenant;
import com.rental.util.Session;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;
import javafx.scene.layout.VBox;
public class BookingController implements Initializable {
    @FXML
    private Label spaceIdLabel, sizeLabel, categoryLabel, dailyPriceLabel, monthlyPriceLabel, depositPriceLabel, errorLabel;
    @FXML
    private Label summarySpaceId, summaryDeposit, summaryRent, summaryTotal;
    @FXML
    private DatePicker startDatePicker, endDatePicker;
    @FXML
    private TextField fullNameField, emailField, phoneField;
    @FXML
    private CheckBox agreeCheckBox;
    @FXML
    private ChoiceBox<String> productTypeChoice;
    @FXML
    private VBox bookingForm;
    @FXML
    private Label noStallMessage;
    private final SupabaseClient supabaseClient = new SupabaseClient();
    private final Gson gson = new Gson();
    private double dailyRate = 0;
    private double monthlyRate = 0;
    private double depositRate = 0;
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Listener สำหรับคำนวณค่าเช่าตามวันที่
        ChangeListener<Object> listener = (ObservableValue<?> obs, Object oldVal, Object newVal) -> calculateSummary();
        startDatePicker.valueProperty().addListener(listener);
        endDatePicker.valueProperty().addListener(listener);
        // เริ่มต้นสรุปค่าเป็น 0
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
                // Set Label
                sizeLabel.setText(size + " เมตร");
                categoryLabel.setText(category);
                dailyPriceLabel.setText(String.format("%.0f ฿/วัน", dailyRate));
                monthlyPriceLabel.setText(String.format("%.0f ฿/เดือน", monthlyRate));
                depositPriceLabel.setText(String.format("%.0f ฿", depositRate));
                // แสดงฟอร์มหลังจากตั้งค่าข้อมูล
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
            if (days < 1) days = 1;
            double totalRent;
            if (days >= 30) {
                double months = days / 30.0; // เดือนทศนิยม
                totalRent = monthlyRate * months;
                summaryDeposit.setText(String.format("%.0f ฿", depositRate)); // แสดงเฉพาะรายเดือน
            } else {
                totalRent = dailyRate * days;
                summaryDeposit.setText("0 ฿"); // ยังไม่ถึงเดือน ไม่แสดงมัดจำ
            }
            summaryRent.setText(String.format("%.0f ฿", totalRent));
            summaryTotal.setText(String.format("%.0f ฿", totalRent));
        } else {
            summaryRent.setText("0 ฿");
            summaryDeposit.setText("0 ฿");
            summaryTotal.setText("0 ฿");
        }
    }
    // ==================== BUTTON ACTION ====================
    @FXML
    public void handleBooking(ActionEvent event) {
        if (!agreeCheckBox.isSelected()) {
            if (errorLabel != null) errorLabel.setText("กรุณายินยอมข้อกำหนดก่อนยืนยันการจอง");
            return;
        }
        if (!Session.isLoggedIn()) {
            if (errorLabel != null) errorLabel.setText("คุณต้องลงชื่อเข้าใช้ก่อนทำการจอง");
            return;
        }
        String fullName = fullNameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String startDate = startDatePicker.getValue() != null ? startDatePicker.getValue().toString() : "";
        String endDate = endDatePicker.getValue() != null ? endDatePicker.getValue().toString() : "";
        String spaceId = spaceIdLabel.getText();
        double total = summaryTotal.getText().isEmpty() ? 0 : Double.parseDouble(summaryTotal.getText().replace(" ฿", ""));
        double deposit = summaryDeposit.getText().isEmpty() ? 0 : Double.parseDouble(summaryDeposit.getText().replace(" ฿", ""));
        int userId = Session.getCurrentUser().getId();
        // เพิ่มการตรวจสอบและดึงค่าจาก ChoiceBox
        String productType = productTypeChoice.getValue();
        if (productType == null || productType.isEmpty()) {
            if (errorLabel != null) errorLabel.setText("กรุณาเลือกประเภทสินค้า");
            return;
        }
        // สร้าง JSON สำหรับ insert
        JsonObject booking = new JsonObject();
        booking.addProperty("user_id", userId);
        booking.addProperty("stall_id", spaceId);
        booking.addProperty("start_date", startDate);
        booking.addProperty("end_date", endDate);
        booking.addProperty("total_price", total);
        booking.addProperty("deposit_price", deposit);
        booking.addProperty("full_name", fullName);
        booking.addProperty("email", email);
        booking.addProperty("phone", phone);
        booking.addProperty("status", "pending");
        booking.addProperty("product_type", productType); // <-- เพิ่มตรงนี้
        try {
            String result = supabaseClient.insert("bookings", booking.toString());
            if (result != null) {
                showAlert(Alert.AlertType.INFORMATION, "จองพื้นที่สำเร็จ!");
                handleCancel(event); // ล้างฟอร์มหลังบันทึก
            } else {
                showAlert(Alert.AlertType.ERROR, "ไม่สามารถบันทึกการจองได้ โปรดลองใหม่");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "เกิดข้อผิดพลาดในการบันทึกการจอง");
        }
    }
    @FXML
    public void handleCancel(ActionEvent event) {
        if (startDatePicker != null) startDatePicker.setValue(null);
        if (endDatePicker != null) endDatePicker.setValue(null);
        if (fullNameField != null) fullNameField.clear();
        if (emailField != null) emailField.clear();
        if (phoneField != null) phoneField.clear();
        if (productTypeChoice != null) productTypeChoice.setValue(null);
        if (errorLabel != null) errorLabel.setText("");
        calculateSummary();
    }
    private void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(null);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}