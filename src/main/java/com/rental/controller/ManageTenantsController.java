package com.rental.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ManageTenantsController {

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> sortComboBox;

    @FXML private TableView<Tenant> tenantTable;
    @FXML private TableColumn<Tenant, String> colName;
    @FXML private TableColumn<Tenant, String> colEmail;
    @FXML private TableColumn<Tenant, String> colPhone;
    @FXML private TableColumn<Tenant, String> colZone;
    @FXML private TableColumn<Tenant, String> colCreatedAt;
    @FXML private TableColumn<Tenant, String> colRejectReason;
    @FXML private TableColumn<Tenant, String> colStatus;
    @FXML private TableColumn<Tenant, Void> colContact;

    @FXML private VBox menu;

    private final ObservableList<Tenant> masterData = FXCollections.observableArrayList();
    private final ObservableList<Tenant> filteredData = FXCollections.observableArrayList();

    // Supabase Config
    private static final String SUPABASE_URL = "https://sdmipxsxkquuyxvvqpho.supabase.co";
    private static final String SUPABASE_SERVICE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNkbWlweHN4a3F1dXl4dnZxcGhvIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc2NDc1MzU0NywiZXhwIjoyMDgwMzI5NTQ3fQ.IqSxzTLKHXlfGdH4RyzaYAIVXrlW7_LsrQEuJBlHJ8k";

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
       
        sortComboBox.setItems(FXCollections.observableArrayList(
                "ชื่อ (ก → ฮ)",
                "ชื่อ (ฮ → ก)"
        ));
        sortComboBox.getSelectionModel().select("ชื่อ (ก → ฮ)");

        // Cell Value Factory
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colZone.setCellValueFactory(new PropertyValueFactory<>("zone"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colRejectReason.setCellValueFactory(new PropertyValueFactory<>("rejectReason"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // สีสถานะ
        colStatus.setCellFactory(column -> new TableCell<Tenant, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    setStyle("-fx-text-fill: #c80000; -fx-font-weight: bold;");
                }
            }
        });

        // ปุ่มติดต่อ
        colContact.setCellFactory(col -> new TableCell<Tenant, Void>() {
            private final Button btn = new Button("ติดต่อ");
            private final VBox wrapper = new VBox(btn);

            {
                wrapper.setAlignment(Pos.CENTER);
                btn.setStyle("-fx-background-color: #7c7c7c; -fx-text-fill: white; -fx-border-radius: 20; -fx-background-radius: 20;");
                btn.setOnAction(e -> openGmail(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : wrapper);
            }
        });

        // โหลดข้อมูลจาก payments (เฉพาะ rejected)
        loadRejectedPayments();
        tenantTable.setItems(filteredData);

        onSortClick();

        nameField.textProperty().addListener((obs, o, n) -> applyFilters());
        phoneField.textProperty().addListener((obs, o, n) -> applyFilters());
        emailField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

  private void loadRejectedPayments() {
    Task<Void> task = new Task<>() {
        @Override
        protected Void call() throws Exception {
            // แก้ตรงนี้ให้ถูกต้องตามโครงสร้างจริง
            String url = SUPABASE_URL + "/rest/v1/payments"
                    + "?status=eq.rejected"
                    + "&select=id,payment_date,reject_reason,booking_id(*)"  // เปลี่ยนเป็น booking_id(*) เพื่อดึงข้อมูลจากตาราง bookings ทั้งหมดที่เกี่ยวข้อง
                    + "&order=created_at.desc";  // เรียงล่าสุดก่อน (optional แต่แนะนำ)

            Request request = new Request.Builder()
                    .url(url)
                    .header("apikey", SUPABASE_SERVICE_KEY)
                    .header("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP " + response.code() + ": " + response.body().string());
                }
                String body = response.body().string();
                System.out.println("Raw JSON response: " + body);  // debug ดูใน console

                JsonArray array = gson.fromJson(body, JsonArray.class);

                masterData.clear();
                for (int i = 0; i < array.size(); i++) {
                    JsonObject obj = array.get(i).getAsJsonObject();

                    // ดึงข้อมูลจากตาราง bookings ผ่าน relationship
                    JsonObject booking = obj.getAsJsonObject("booking_id");  // ชื่อ key จะเป็น booking_id เพราะเราใช้ booking_id(*)

                    String fullName = booking != null && booking.has("full_name") ? booking.get("full_name").getAsString() : "ไม่ระบุ";
                    String email = booking != null && booking.has("email") ? booking.get("email").getAsString() : "-";
                    String phone = booking != null && booking.has("phone") ? booking.get("phone").getAsString() : "-";
                    String stallId = booking != null && booking.has("stall_id") ? booking.get("stall_id").getAsString() : "-";

                    // payment_date อาจเป็น null ได้บ้าง ต้องเช็ค
                    String paymentDateStr = obj.has("payment_date") && !obj.get("payment_date").isJsonNull()
                            ? obj.get("payment_date").getAsString()
                            : obj.get("created_at").getAsString().substring(0, 10);  // fallback ใช้ created_at

                    String formattedDate = formatThaiDate(paymentDateStr);

                    String rejectReason = obj.has("reject_reason") && !obj.get("reject_reason").isJsonNull()
                            ? obj.get("reject_reason").getAsString()
                            : "-";

                    masterData.add(new Tenant(
                            obj.get("id").getAsInt(),
                            fullName,
                            email,
                            phone,
                            stallId,
                            formattedDate,
                            rejectReason
                    ));
                }
            }
            return null;
        }
    };

    task.setOnSucceeded(e -> {
        System.out.println("โหลดข้อมูลสำเร็จ: " + masterData.size() + " รายการ");
        filteredData.setAll(masterData);
        onSortClick();
        tenantTable.refresh();
    });

    task.setOnFailed(e -> {
        Throwable ex = task.getException();
        String msg = ex != null ? ex.getMessage() : "Unknown error";
        showAlert(Alert.AlertType.ERROR, "โหลดข้อมูลล้มเหลว", msg);
        ex.printStackTrace();
    });

    new Thread(task).start();  // อย่าลืม start task!
}

    @FXML
    private void onResetClick() {
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        sortComboBox.getSelectionModel().select("ชื่อ (ก → ฮ)");
        filteredData.setAll(masterData);
        onSortClick();
        tenantTable.refresh();
    }

    @FXML
    private void onSortClick() {
        String selected = sortComboBox.getValue();
        if (selected == null || filteredData.isEmpty()) return;

        ObservableList<Tenant> listToSort = FXCollections.observableArrayList(filteredData);

        if (selected.contains("ก → ฮ")) {
            listToSort.sort((t1, t2) -> t1.getName().compareToIgnoreCase(t2.getName()));
        } else {
            listToSort.sort((t1, t2) -> t2.getName().compareToIgnoreCase(t1.getName()));
        }

        filteredData.setAll(listToSort);
        tenantTable.refresh();
    }

    private void applyFilters() {
        String name = nameField.getText().trim().toLowerCase();
        String phone = phoneField.getText().trim().toLowerCase();
        String email = emailField.getText().trim().toLowerCase();

        filteredData.setAll(masterData.stream()
                .filter(t -> name.isEmpty() || t.getName().toLowerCase().contains(name))
                .filter(t -> phone.isEmpty() || t.getPhone().toLowerCase().contains(phone))
                .filter(t -> email.isEmpty() || t.getEmail().toLowerCase().contains(email))
                .toList());

        onSortClick();
        tenantTable.refresh();
    }

    private void openGmail(Tenant tenant) {
        String email = tenant.getEmail().trim();
        String name = tenant.getName().trim();
        String zone = tenant.getZone().trim();
        String reason = tenant.getRejectReason().trim();

        if (email.isEmpty() || email.equals("-")) {
            showAlert(Alert.AlertType.WARNING, "ไม่มีอีเมลล์", "ผู้เช่ารายนี้ไม่มีอีเมลล์ที่บันทึกไว้");
            return;
        }

        String subject = "[Rental System] แจ้งผลการปฏิเสธคำขอเช่าพื้นที่ - คุณ" + name + " (โซน " + zone + ")";
        String encodedSubject = URLEncoder.encode(subject, StandardCharsets.UTF_8);

        String body = "สวัสดีค่ะ/ครับ คุณ" + name + "\n\n" +
                      "เราต้องขออภัยที่ต้องแจ้งว่าคำขอเช่าพื้นที่โซน " + zone + " ของท่านไม่ผ่านการพิจารณา\n\n" +
                      "เหตุผล: " + (reason.equals("-") ? "ไม่ระบุ" : reason) + "\n\n" +
                      "หากท่านได้ชำระเงินมัดจำหรือค่าจองแล้ว เรายินดีดำเนินการคืนเงินให้เต็มจำนวน\n" +
                      "กรุณาติดต่อกลับเพื่อแจ้งช่องทางการคืนเงินที่สะดวก (เช่น เลขบัญชีธนาคาร พร้อมชื่อบัญชี)\n\n" +
                      "ขอบคุณที่สนใจบริการของเรา\n" +
                      "ทีมงาน Rental Space Management";

        String encodedBody = URLEncoder.encode(body, StandardCharsets.UTF_8);

        String mailtoLink = "https://mail.google.com/mail/?view=cm&fs=1&to=" + email +
                            "&su=" + encodedSubject +
                            "&body=" + encodedBody;

        try {
            Desktop.getDesktop().browse(new URI(mailtoLink));
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "เปิด Gmail ไม่ได้", "กรุณาคัดลอกอีเมลล์นี้: " + email);
        }
    }

    private String formatThaiDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("th-TH")));
        } catch (Exception e) {
            return dateStr;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class Tenant {
        private final SimpleStringProperty name = new SimpleStringProperty();
        private final SimpleStringProperty email = new SimpleStringProperty();
        private final SimpleStringProperty phone = new SimpleStringProperty();
        private final SimpleStringProperty zone = new SimpleStringProperty();
        private final SimpleStringProperty createdAt = new SimpleStringProperty();
        private final SimpleStringProperty rejectReason = new SimpleStringProperty();
        private final SimpleStringProperty status = new SimpleStringProperty();
        private final int id;

        public Tenant(int id, String name, String email, String phone, String zone, String createdAt, String rejectReason) {
            this.id = id;
            this.name.set(name);
            this.email.set(email);
            this.phone.set(phone);
            this.zone.set(zone);
            this.createdAt.set(createdAt);
            this.rejectReason.set(rejectReason);
            this.status.set("ไม่อนุมัติ");
        }

        public int getId() { return id; }
        public String getName() { return name.get(); }
        public String getEmail() { return email.get(); }
        public String getPhone() { return phone.get(); }
        public String getZone() { return zone.get(); }
        public String getCreatedAt() { return createdAt.get(); }
        public String getRejectReason() { return rejectReason.get(); }

        public SimpleStringProperty nameProperty() { return name; }
        public SimpleStringProperty emailProperty() { return email; }
        public SimpleStringProperty phoneProperty() { return phone; }
        public SimpleStringProperty zoneProperty() { return zone; }
        public SimpleStringProperty createdAtProperty() { return createdAt; }
        public SimpleStringProperty rejectReasonProperty() { return rejectReason; }
        public SimpleStringProperty statusProperty() { return status; }
    }
}