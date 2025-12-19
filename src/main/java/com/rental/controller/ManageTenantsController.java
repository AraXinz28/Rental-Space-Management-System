package com.rental.controller;

import com.rental.database.SupabaseClient;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;
import java.awt.Desktop;
import java.net.URI;

public class ManageTenantsController {

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> pageSizeCombo;
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

    private final ObservableList<Tenant> masterData = FXCollections.observableArrayList();
    private final ObservableList<Tenant> filteredData = FXCollections.observableArrayList();

    private final SupabaseClient supabase = new SupabaseClient();

    @FXML
    public void initialize() {
        // ตั้งค่าแสดงต่อหน้า
        pageSizeCombo.setItems(FXCollections.observableArrayList("10", "20", "50", "100"));
        pageSizeCombo.getSelectionModel().select("20");

        // ตั้งค่าจัดเรียงตาม
        sortComboBox.setItems(FXCollections.observableArrayList(
                "ชื่อ (ก → ฮ)",
                "ชื่อ (ฮ → ก)"
        ));
        sortComboBox.getSelectionModel().select("จัดเรียงตาม");

        // Cell Value Factory
        colName.setCellValueFactory(data -> data.getValue().nameProperty());
        colEmail.setCellValueFactory(data -> data.getValue().emailProperty());
        colPhone.setCellValueFactory(data -> data.getValue().phoneProperty());
        colZone.setCellValueFactory(data -> data.getValue().zoneProperty());
        colCreatedAt.setCellValueFactory(data -> data.getValue().createdAtProperty());
        colRejectReason.setCellValueFactory(data -> data.getValue().rejectReasonProperty());
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());

        // สีสถานะ
        colStatus.setCellFactory(column -> new TableCell<>() {
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
// ปุ่มติดต่อ → เปิด Gmail พร้อมกรอกอีเมลล์ + หัวข้อ + เนื้อเมลเริ่มต้น
colContact.setCellFactory(col -> new TableCell<>() {
    private final Button btn = new Button("ติดต่อ");
    private final VBox wrapper = new VBox(btn);

    {
        wrapper.setAlignment(Pos.CENTER);
        btn.setStyle("-fx-background-color: #7c7c7c; -fx-text-fill: white; -fx-border-radius: 20; -fx-background-radius: 20;");
        btn.setOnAction(e -> {
            Tenant tenant = getTableView().getItems().get(getIndex());
            String email = tenant.getEmail().trim();
            String name = tenant.getName().trim();
            String zone = tenant.getZone().trim();
            String reason = tenant.getRejectReason().trim();

            if (email.isEmpty() || email.equals("-")) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("ไม่มีอีเมลล์");
                alert.setContentText("ผู้เช่ารายนี้ไม่มีอีเมลล์ที่บันทึกไว้");
                alert.show();
                return;
            }

            // หัวข้ออีเมล
            String subject = "[Rental System] แจ้งผลการปฏิเสธคำขอเช่าพื้นที่ - คุณ" + name + " (โซน " + zone + ")";
            String encodedSubject = java.net.URLEncoder.encode(subject, java.nio.charset.StandardCharsets.UTF_8);

            // เนื้อเมลเริ่มต้น (body) - มีคำเชิญชวนให้ติดต่อกลับเรื่องคืนเงิน
            String body = "สวัสดีค่ะ/ครับ คุณ" + name  +
                          "เราต้องขออภัยที่ต้องแจ้งว่าคำขอเช่าพื้นที่โซน " + zone + " ของท่านไม่ผ่านการพิจารณา" +
                          "เหตุผล: " + (reason.equals("-") ? "ไม่ระบุ" : reason)  +
                          "หากท่านได้ชำระเงินมัดจำหรือค่าจองแล้ว เรายินดีดำเนินการคืนเงินให้เต็มจำนวน" +
                          "กรุณาติดกลับเพื่อแจ้งช่องทางการคืนเงินที่สะดวก (เช่น เลขบัญชีธนาคาร พร้อมชื่อบัญชี)" +
                          "ขอบคุณที่สนใจบริการของเรา" +
                          "ทีมงาน Rental Space Management";

            String encodedBody = java.net.URLEncoder.encode(body, java.nio.charset.StandardCharsets.UTF_8);

            // ลิงก์ Gmail พร้อม To + Subject + Body
            String mailtoLink = "https://mail.google.com/mail/?view=cm&fs=1&to=" + email +
                                "&su=" + encodedSubject +
                                "&body=" + encodedBody;

            try {
                Desktop.getDesktop().browse(new URI(mailtoLink));
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("เปิด Gmail ไม่ได้");
                alert.setContentText("ไม่สามารถเปิด Gmail ได้อัตโนมัติ\nกรุณาคัดลอกอีเมลล์นี้: " + email);
                alert.show();
            }
        });
    }

    @Override
    protected void updateItem(Void item, boolean empty) {
        super.updateItem(item, empty);
        setGraphic(empty ? null : wrapper);
    }
});

        // โหลดข้อมูล
        loadRejectedTenants();
        tenantTable.setItems(filteredData);

        // จัดเรียงตามค่าเริ่มต้น
        onSortClick();

        // ค้นหาแบบ real-time
        nameField.textProperty().addListener((obs, o, n) -> applyFilters());
        phoneField.textProperty().addListener((obs, o, n) -> applyFilters());
        emailField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    @FXML
    private void onResetClick() {
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        sortComboBox.getSelectionModel().select("วันที่ทำรายการ (ใหม่ → เก่า)");
        pageSizeCombo.getSelectionModel().select("20");
        filteredData.setAll(masterData);
        onSortClick(); // จัดเรียงใหม่หลังรีเซ็ต
        tenantTable.refresh();
    }

    @FXML
    private void onSortClick() {
        String selected = sortComboBox.getValue();
        if (selected == null || masterData.isEmpty()) {
            return;
        }

        // ใช้ข้อมูลจาก filteredData ถ้ามีการกรองอยู่ มิฉะนั้นใช้ masterData
        ObservableList<Tenant> listToSort = FXCollections.observableArrayList(
                filteredData.isEmpty() ? masterData : filteredData
        );

        switch (selected) {
            case "ชื่อ (ก → ฮ)":
                FXCollections.sort(listToSort, (t1, t2) -> t1.getName().compareToIgnoreCase(t2.getName()));
                break;
            case "ชื่อ (ฮ → ก)":
                FXCollections.sort(listToSort, (t1, t2) -> t2.getName().compareToIgnoreCase(t1.getName()));
                break;
           
        }

        filteredData.setAll(listToSort);
        tenantTable.refresh();
    }

    private void loadRejectedTenants() {
        try {
            String json = supabase.selectAll("booking_demo_status");
            JSONArray arr = new JSONArray(json);

            masterData.clear();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                String status = o.optString("status", "").trim();
                if (!status.equalsIgnoreCase("rejected")) {
                    continue;
                }

                String bookingDate = o.optString("booking_date", "-");
                String displayDate = "-";
                if (!bookingDate.equals("-") && bookingDate.length() == 10) {
                    displayDate = bookingDate.substring(8, 10) + "/" +
                                  bookingDate.substring(5, 7) + "/" +
                                  bookingDate.substring(0, 4);
                }

                masterData.add(new Tenant(
                        o.optInt("id"),
                        o.optString("customer_name", "-"),
                        o.optString("email", "-"),
                        o.optString("phone", "-"),
                        o.optString("zone", "-"),
                        displayDate,
                        o.optString("reject_reason", "-")
                ));
            }

            filteredData.setAll(masterData);
            onSortClick(); // จัดเรียงหลังโหลดเสร็จ

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("โหลดข้อมูลล้มเหลว");
            alert.setContentText("ข้อผิดพลาด: " + e.getMessage());
            alert.show();
        }
    }

    private void applyFilters() {
        String name = nameField.getText().trim().toLowerCase();
        String phone = phoneField.getText().trim().toLowerCase();
        String email = emailField.getText().trim().toLowerCase();

        ObservableList<Tenant> filtered = FXCollections.observableArrayList();

        for (Tenant t : masterData) {
            if ((name.isEmpty() || t.getName().toLowerCase().contains(name)) &&
                (phone.isEmpty() || t.getPhone().toLowerCase().contains(phone)) &&
                (email.isEmpty() || t.getEmail().toLowerCase().contains(email))) {
                filtered.add(t);
            }
        }

        filteredData.setAll(filtered);
        onSortClick(); // รักษาการเรียงลำดับหลังกรอง
        tenantTable.refresh();
    }
}

class Tenant {
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
        this.status.set("rejected");
    }

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

    public int getId() { return id; }
}
