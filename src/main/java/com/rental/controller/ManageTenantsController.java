package com.rental.controller;

import java.util.Arrays;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class ManageTenantsController {

    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private ComboBox<String> pageSizeCombo;
    @FXML private ComboBox<String> statusFilter;

    @FXML private TableView<Tenant> tenantTable;
    @FXML private TableColumn<Tenant, String> colName;
    @FXML private TableColumn<Tenant, String> colPhone;
    @FXML private TableColumn<Tenant, String> colEmail;
    @FXML private TableColumn<Tenant, String> colStatus;
    @FXML private TableColumn<Tenant, Void> colContact;

    private final ObservableList<Tenant> masterData = FXCollections.observableArrayList();
    private final ObservableList<Tenant> filteredData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        // ✅ Dropdown สถานะ
        statusFilter.setItems(FXCollections.observableArrayList(
                "ทั้งหมด", "ปกติ", "ลูกค้ายกเลิก", "แอดมินยกเลิก"
        ));
        statusFilter.getSelectionModel().select("ทั้งหมด");

        // ✅ ทำงานทันทีเมื่อเลือกสถานะ
        statusFilter.setOnAction(e -> applyFilters());

        // ✅ จำนวนต่อหน้า
        pageSizeCombo.setItems(FXCollections.observableArrayList("10", "20", "50"));
        pageSizeCombo.getSelectionModel().select("10");

        // ✅ Map คอลัมน์
        colName.setCellValueFactory(data -> data.getValue().nameProperty());
        colPhone.setCellValueFactory(data -> data.getValue().phoneProperty());
        colEmail.setCellValueFactory(data -> data.getValue().emailProperty());
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());

        // ✅ ใส่สีให้สถานะ
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(status);

                switch (status) {
                    case "ปกติ" -> setStyle("-fx-text-fill: #009e0bff;");
                    case "ลูกค้ายกเลิก" -> setStyle("-fx-text-fill: #c80000ff;");
                    case "แอดมินยกเลิก" -> setStyle("-fx-text-fill: #c80000ff;");
                }
            }
        });

        setupContactButton();
        loadMockData();

        tenantTable.setItems(filteredData);
tenantTable.getColumns().setAll(Arrays.asList(colName, colPhone, colEmail, colContact));
tenantTable.setColumnResizePolicy(param -> true);

        // ✅ ค้นหาแบบ real-time
        nameField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        phoneField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        emailField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
    }

    // ✅ ฟิลเตอร์หลัก (ชื่อ + เบอร์ + อีเมล + สถานะ)
    private void applyFilters() {
        String name = nameField.getText().trim().toLowerCase();
        String phone = phoneField.getText().trim().toLowerCase();
        String email = emailField.getText().trim().toLowerCase();
        String status = statusFilter.getValue();

        filteredData.setAll(
                masterData.filtered(tenant ->
                        (name.isEmpty() || tenant.getName().toLowerCase().contains(name)) &&
                        (phone.isEmpty() || tenant.getPhone().toLowerCase().contains(phone)) &&
                        (email.isEmpty() || tenant.getEmail().toLowerCase().contains(email)) &&
                        (status.equals("ทั้งหมด") || tenant.getStatus().equals(status))
                )
        );
    }

    private void setupContactButton() {
        colContact.setCellFactory(col -> new TableCell<>() {

            private final Button btn = new Button("ติดต่อ");
            private final VBox wrapper = new VBox(btn);

            {
                wrapper.setAlignment(javafx.geometry.Pos.CENTER);

                btn.setStyle("-fx-background-color: #7c7c7cff; -fx-text-fill: white; -fx-background-radius: 5;");
                btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #5a5a5aff; -fx-text-fill: white;"));
                btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #7c7c7cff; -fx-text-fill: white;"));

                btn.setOnAction(e -> {
                    Tenant tenant = getTableView().getItems().get(getIndex());
                    showContactPopup(tenant);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : wrapper);
            }
        });
    }

    private void loadMockData() {
        masterData.setAll(
                new Tenant("นายสมชาย ใจดี", "081-2345678", "somchai@gmail.com", "ปกติ"),
                new Tenant("นางอัมพร สองใจ", "081-2345678", "amporn@gmail.com", "ลูกค้ายกเลิก"),
                new Tenant("นางสาวร้อนตัว เย็นใจ", "081-2345678", "yenchai@gmail.com", "ปกติ"),
                new Tenant("นายสิงสา ราสัตว์", "081-2345678", "singsa@gmail.com", "แอดมินยกเลิก"),
                new Tenant("นายดำ คั่วพริกเกลือ", "081-2345678", "dam@gmail.com", "ปกติ")
        );

        filteredData.setAll(masterData);
    }

    @FXML
    private void onFilterClick() {
        applyFilters();
    }

    @FXML
    private void onResetClick() {
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        statusFilter.getSelectionModel().select("ทั้งหมด");
        pageSizeCombo.getSelectionModel().select("10");

        applyFilters();
    }

    private void showContactPopup(Tenant tenant) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("ติดต่อผู้เช่า");

        Label nameLabel = new Label("ชื่อลูกค้า: " + tenant.getName());
        Label phoneLabel = new Label("เบอร์โทร: " + tenant.getPhone());
        Label emailLabel = new Label("อีเมล์: " + tenant.getEmail());
        Label statusLabel = new Label("สถานะ: " + tenant.getStatus());

        Button closeBtn = new Button("ปิด");
        closeBtn.setOnAction(e -> dialog.close());

        VBox vbox = new VBox(10, nameLabel, phoneLabel, emailLabel, statusLabel, closeBtn);
        vbox.setStyle("-fx-padding: 20; -fx-background-color: #ffffff;");
        vbox.setPrefWidth(300);

        dialog.setScene(new Scene(vbox));
        dialog.showAndWait();
    }
}

/* ✅ Tenant class */
class Tenant {

    private final javafx.beans.property.StringProperty name =
            new javafx.beans.property.SimpleStringProperty();
    private final javafx.beans.property.StringProperty phone =
            new javafx.beans.property.SimpleStringProperty();
    private final javafx.beans.property.StringProperty email =
            new javafx.beans.property.SimpleStringProperty();
    private final javafx.beans.property.StringProperty status =
            new javafx.beans.property.SimpleStringProperty();

    public Tenant(String name, String phone, String email, String status) {
        this.name.set(name);
        this.phone.set(phone);
        this.email.set(email);
        this.status.set(status);
    }

    public String getName() { return name.get(); }
    public javafx.beans.property.StringProperty nameProperty() { return name; }

    public String getPhone() { return phone.get(); }
    public javafx.beans.property.StringProperty phoneProperty() { return phone; }

    public String getEmail() { return email.get(); }
    public javafx.beans.property.StringProperty emailProperty() { return email; }

    public String getStatus() { return status.get(); }
    public javafx.beans.property.StringProperty statusProperty() { return status; }
}
