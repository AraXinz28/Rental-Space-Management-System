package com.rental.controller;

import com.rental.database.SupabaseClient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONObject;

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
    @FXML private TableColumn<Tenant, String> colZone;
    @FXML private TableColumn<Tenant, String> colStatus;
    @FXML private TableColumn<Tenant, Void> colContact;
    @FXML private TableColumn<Tenant, Void> colEditStatus;

    private final ObservableList<Tenant> masterData = FXCollections.observableArrayList();
    private final ObservableList<Tenant> filteredData = FXCollections.observableArrayList();

    private final SupabaseClient supabase = new SupabaseClient();

    /* ==================== FXML ACTIONS ==================== */

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

    /* ==================== INITIALIZE ==================== */

    @FXML
    public void initialize() {

        statusFilter.setItems(FXCollections.observableArrayList(
                "ทั้งหมด", "pending", "approved", "rejected"
        ));
        statusFilter.getSelectionModel().select("ทั้งหมด");
        statusFilter.setOnAction(e -> applyFilters());

        pageSizeCombo.setItems(FXCollections.observableArrayList("10", "20", "50"));
        pageSizeCombo.getSelectionModel().select("10");

        colName.setCellValueFactory(data -> data.getValue().nameProperty());
        colPhone.setCellValueFactory(data -> data.getValue().phoneProperty());
        colEmail.setCellValueFactory(data -> data.getValue().emailProperty());
        colZone.setCellValueFactory(data -> data.getValue().zoneProperty());
        colStatus.setCellValueFactory(data -> data.getValue().statusProperty());

        setupStatusColor();
        setupContactButton();
        setupEditStatusButton();

        loadTenantsFromSupabase();

        tenantTable.setItems(filteredData);
        tenantTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        nameField.textProperty().addListener((obs, o, n) -> applyFilters());
        phoneField.textProperty().addListener((obs, o, n) -> applyFilters());
        emailField.textProperty().addListener((obs, o, n) -> applyFilters());
    }

    /* ==================== TABLE SETUP ==================== */

    private void setupStatusColor() {
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

                switch (status.trim().toLowerCase()) {
                    case "approved" -> setStyle("-fx-text-fill: #009e0b;");
                    case "pending" -> setStyle("-fx-text-fill: #e6a800;");
                    case "rejected" -> setStyle("-fx-text-fill: #c80000;");
                    default -> setStyle("");
                }
            }
        });
    }

    private void setupContactButton() {
        colContact.setCellFactory(col -> new TableCell<>() {

            private final Button btn = new Button("ติดต่อ");
            private final VBox wrapper = new VBox(btn);

            {
                wrapper.setAlignment(javafx.geometry.Pos.CENTER);
                btn.setStyle("-fx-background-color: #7c7c7c; -fx-text-fill: white;");
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

    private void setupEditStatusButton() {
        colEditStatus.setCellFactory(col -> new TableCell<>() {

            private final Button btn = new Button("แก้ไขสถานะ");
            private final VBox wrapper = new VBox(btn);

            {
                wrapper.setAlignment(javafx.geometry.Pos.CENTER);
                btn.setStyle("-fx-background-color: #ffa726; -fx-text-fill: white;");
                btn.setOnAction(e -> {
                    Tenant tenant = getTableView().getItems().get(getIndex());
                    showEditStatusPopup(tenant);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : wrapper);
            }
        });
    }

    /* ==================== DATA ==================== */

    private void loadTenantsFromSupabase() {
        try {
            String json = supabase.selectAll("booking_demo_status");
            JSONArray arr = new JSONArray(json);

            masterData.clear();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                masterData.add(new Tenant(
                        o.optInt("id"),
                        o.optString("customer_name", "-"),
                        "-",
                        "-",
                        o.optString("zone", "-"),
                        o.optString("status", "-")
                ));
            }

            filteredData.setAll(masterData);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void applyFilters() {
        String name = nameField.getText().trim().toLowerCase();
        String phone = phoneField.getText().trim().toLowerCase();
        String email = emailField.getText().trim().toLowerCase();
        String status = statusFilter.getValue();

        filteredData.setAll(
                masterData.filtered(t ->
                        (name.isEmpty() || t.getName().toLowerCase().contains(name)) &&
                        (phone.isEmpty() || t.getPhone().toLowerCase().contains(phone)) &&
                        (email.isEmpty() || t.getEmail().toLowerCase().contains(email)) &&
                        (status.equals("ทั้งหมด") || t.getStatus().equalsIgnoreCase(status))
                )
        );
    }

    /* ==================== POPUPS ==================== */

    private void showContactPopup(Tenant t) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox vbox = new VBox(10,
                new Label("ชื่อ: " + t.getName()),
                new Label("โทร: " + t.getPhone()),
                new Label("อีเมล: " + t.getEmail()),
                new Label("โซน: " + t.getZone()),
                new Label("สถานะ: " + t.getStatus()),
                new Button("ปิด")
        );

        vbox.setStyle("-fx-padding: 20;");
        dialog.setScene(new Scene(vbox));
        dialog.showAndWait();
    }

    private void showEditStatusPopup(Tenant t) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);

        ComboBox<String> box = new ComboBox<>();
        box.getItems().addAll("pending", "approved", "rejected");
        box.setValue(t.getStatus());

        Button save = new Button("บันทึก");
        save.setOnAction(e -> {
         
tenantTable.getColumns().setAll(Arrays.asList(colName, colPhone, colEmail, colContact));
tenantTable.setColumnResizePolicy(param -> true);
            t.setStatus(box.getValue());
            applyFilters();
            dialog.close();
        });

        VBox vbox = new VBox(10, new Label("สถานะใหม่"), box, save);
        vbox.setStyle("-fx-padding: 20;");
        dialog.setScene(new Scene(vbox));
        dialog.showAndWait();
    }
}

/* ==================== MODEL ==================== */

class Tenant {

    private final javafx.beans.property.IntegerProperty id =
            new javafx.beans.property.SimpleIntegerProperty();
    private final javafx.beans.property.StringProperty name =
            new javafx.beans.property.SimpleStringProperty();
    private final javafx.beans.property.StringProperty phone =
            new javafx.beans.property.SimpleStringProperty();
    private final javafx.beans.property.StringProperty email =
            new javafx.beans.property.SimpleStringProperty();
    private final javafx.beans.property.StringProperty zone =
            new javafx.beans.property.SimpleStringProperty();
    private final javafx.beans.property.StringProperty status =
            new javafx.beans.property.SimpleStringProperty();

    public Tenant(int id, String name, String phone, String email, String zone, String status) {
        this.id.set(id);
        this.name.set(name);
        this.phone.set(phone);
        this.email.set(email);
        this.zone.set(zone);
        this.status.set(status);
    }

    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getPhone() { return phone.get(); }
    public String getEmail() { return email.get(); }
    public String getZone() { return zone.get(); }
    public String getStatus() { return status.get(); }

    public void setStatus(String s) { status.set(s); }

    public javafx.beans.property.StringProperty nameProperty() { return name; }
    public javafx.beans.property.StringProperty phoneProperty() { return phone; }
    public javafx.beans.property.StringProperty emailProperty() { return email; }
    public javafx.beans.property.StringProperty zoneProperty() { return zone; }
    public javafx.beans.property.StringProperty statusProperty() { return status; }
}
