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

        // สีสถานะ (รองรับทั้ง rejected และ cancelled)
        colStatus.setCellFactory(column -> new TableCell<Tenant, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("ไม่อนุมัติ".equals(status)) {
                        setStyle("-fx-text-fill: #c80000; -fx-font-weight: bold;"); // แดงเข้ม
                    } else if ("ยกเลิก".equals(status)) {
                        setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;"); // ส้ม
                    } else {
                        setStyle("");
                    }
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

        // โหลดข้อมูล
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
            String url = SUPABASE_URL + "/rest/v1/payments"
                    + "?or=(status.eq.rejected,status.eq.cancelled)"
                    + "&select=id,status,created_at,payment_date,reject_reason,booking_id(*)"
                    + "&order=created_at.desc";

            Request request = new Request.Builder()
                    .url(url)
                    .header("apikey", SUPABASE_SERVICE_KEY)
                    .header("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("HTTP error " + response.code() + ": " + response.body().string());
                }

                String body = response.body().string();
                System.out.println("Raw JSON → " + body);  // ช่วย debug มาก!

                JsonArray array = gson.fromJson(body, JsonArray.class);

                masterData.clear();

                for (int i = 0; i < array.size(); i++) {
                    JsonObject obj = array.get(i).getAsJsonObject();

                    JsonObject booking = obj.has("booking_id") && !obj.get("booking_id").isJsonNull()
                            ? obj.getAsJsonObject("booking_id")
                            : null;

                    String fullName = (booking != null && booking.has("full_name") && !booking.get("full_name").isJsonNull())
                            ? booking.get("full_name").getAsString()
                            : "ไม่พบข้อมูลผู้เช่า";

                    String email = (booking != null && booking.has("email") && !booking.get("email").isJsonNull())
                            ? booking.get("email").getAsString()
                            : "-";

                    String phone = (booking != null && booking.has("phone") && !booking.get("phone").isJsonNull())
                            ? booking.get("phone").getAsString()
                            : "-";

                    String stallId = (booking != null && booking.has("stall_id") && !booking.get("stall_id").isJsonNull())
                            ? booking.get("stall_id").getAsString()
                            : "-";
String paymentDateStr;
if (obj.has("payment_date") && !obj.get("payment_date").isJsonNull()) {
    paymentDateStr = obj.get("payment_date").getAsString();
} else if (obj.has("created_at") && !obj.get("created_at").isJsonNull()) {
    paymentDateStr = obj.get("created_at").getAsString().substring(0, 10);
} else {
    paymentDateStr = "";
}


                    String formattedDate = formatThaiDate(paymentDateStr);

                    String rejectReason = obj.has("reject_reason") && !obj.get("reject_reason").isJsonNull()
                            ? obj.get("reject_reason").getAsString()
                            : "-";

                 String dbStatus = obj.has("status") && !obj.get("status").isJsonNull()
        ? obj.get("status").getAsString()
        : "";
                    String displayStatus = "rejected".equals(dbStatus) ? "ไม่อนุมัติ" : "ยกเลิก";

                    masterData.add(new Tenant(
                            obj.get("id").getAsInt(),
                            fullName,
                            email,
                            phone,
                            stallId,
                            formattedDate,
                            rejectReason,
                            displayStatus
                    ));
                }
            }
            return null;
        }
    };

    // ส่วนที่เหลือเหมือนเดิม...
    task.setOnSucceeded(e -> {
        System.out.println("โหลดสำเร็จ → " + masterData.size() + " รายการ");
        filteredData.setAll(masterData);
        onSortClick();
        tenantTable.refresh();
    });

    task.setOnFailed(e -> {
        Throwable ex = task.getException();
        showAlert(Alert.AlertType.ERROR, "โหลดข้อมูลล้มเหลว", ex != null ? ex.getMessage() : "ไม่ทราบสาเหตุ");
        if (ex != null) ex.printStackTrace();
    });

    new Thread(task).start();
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
        String status = tenant.getStatus();

        if (email.isEmpty() || email.equals("-")) {
            showAlert(Alert.AlertType.WARNING, "ไม่มีอีเมลล์", "ผู้เช่ารายนี้ไม่มีอีเมลล์ที่บันทึกไว้");
            return;
        }

        String subject;
        String body;

        if ("ยกเลิก".equals(status)) {
            subject = "[Rental System] แจ้งการยกเลิกคำขอเช่าพื้นที่ - คุณ" + name + " (โซน " + zone + ")";
            body = "เรียน คุณ" + name + "\n\n" +
                   "เราขอแจ้งว่าคำขอเช่าพื้นที่โซน " + zone + " ของท่านได้ถูกยกเลิกเรียบร้อยแล้ว ตามที่ท่านแจ้งความประสงค์\n\n" +
                   "หากท่านได้ชำระเงินมัดจำหรือค่าจองไว้ก่อนหน้านี้ ทีมงานจะดำเนินการคืนเงินให้ท่านเต็มจำนวนโดยเร็วที่สุด\n" +
                   "กรุณาติดต่อกลับเพื่อแจ้งช่องทางการคืนเงินที่สะดวก (เช่น เลขบัญชีธนาคาร ชื่อบัญชี และชื่อธนาคาร)\n\n" +
                   "หากมีข้อสงสัยหรือต้องการความช่วยเหลือเพิ่มเติม ยินดีให้บริการทุกเมื่อค่ะ\n" +
                   "ขอบคุณที่ใช้บริการ\n\n" +
                   "ด้วยความเคารพ\nทีมงาน Rental Space Management";
        } else { // ไม่อนุมัติ (rejected)
            subject = "[Rental System] แจ้งผลการพิจารณาคำขอเช่าพื้นที่ - คุณ" + name + " (โซน " + zone + ")";
            body = "เรียน คุณ" + name + "\n\n" +
                   "เราต้องขออภัยเป็นอย่างยิ่งที่ต้องแจ้งให้ทราบว่า คำขอเช่าพื้นที่โซน " + zone + " ของท่านไม่ผ่านการพิจารณา\n\n" +
                   "เหตุผล: " + (reason.equals("-") ? "ไม่ระบุ" : reason) + "\n\n" +
                   "หากท่านได้ชำระเงินมัดจำหรือค่าจองไว้ก่อนหน้านี้ ทีมงานยินดีดำเนินการคืนเงินให้ท่านเต็มจำนวนโดยเร็วที่สุด\n" +
                   "กรุณาติดต่อกลับเพื่อแจ้งช่องทางการคืนเงินที่สะดวก (เช่น เลขบัญชีธนาคาร ชื่อบัญชี และชื่อธนาคาร)\n\n" +
                   "เราขอขอบคุณที่ท่านให้ความสนใจในบริการของเรา และหวังว่าจะมีโอกาสให้บริการท่านในโอกาสต่อไป\n\n" +
                   "ด้วยความเคารพ\nทีมงาน Rental Space Management";
        }

        String encodedSubject = URLEncoder.encode(subject, StandardCharsets.UTF_8);
        String encodedBody = URLEncoder.encode(body, StandardCharsets.UTF_8);

        String mailtoLink = "https://mail.google.com/mail/?view=cm&fs=1&to=" + email +
                            "&su=" + encodedSubject +
                            "&body=" + encodedBody;

        try {
            Desktop.getDesktop().browse(new URI(mailtoLink));
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "เปิด Gmail ไม่สำเร็จ", "กรุณาคัดลอกอีเมลล์นี้ด้วยตนเอง: " + email);
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

        public Tenant(int id, String name, String email, String phone, String zone, String createdAt, String rejectReason, String status) {
            this.id = id;
            this.name.set(name);
            this.email.set(email);
            this.phone.set(phone);
            this.zone.set(zone);
            this.createdAt.set(createdAt);
            this.rejectReason.set(rejectReason);
            this.status.set(status);
        }

        public int getId() { return id; }
        public String getName() { return name.get(); }
        public String getEmail() { return email.get(); }
        public String getPhone() { return phone.get(); }
        public String getZone() { return zone.get(); }
        public String getCreatedAt() { return createdAt.get(); }
        public String getRejectReason() { return rejectReason.get(); }
        public String getStatus() { return status.get(); }

        public SimpleStringProperty nameProperty() { return name; }
        public SimpleStringProperty emailProperty() { return email; }
        public SimpleStringProperty phoneProperty() { return phone; }
        public SimpleStringProperty zoneProperty() { return zone; }
        public SimpleStringProperty createdAtProperty() { return createdAt; }
        public SimpleStringProperty rejectReasonProperty() { return rejectReason; }
        public SimpleStringProperty statusProperty() { return status; }
    }
}