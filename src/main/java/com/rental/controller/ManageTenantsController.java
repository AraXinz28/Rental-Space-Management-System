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

        nameField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        phoneField.textProperty().addListener((obs, oldV, newV) -> applyFilters());
        emailField.textProperty().addListener((obs, oldV, newV) -> applyFilters());

        
    }

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

            // ⭐ normalize
            String s = status.trim().toLowerCase();

            switch (s) {
                case "approved" -> setStyle("-fx-text-fill: #009e0bff;");   // เขียว
                case "pending" -> setStyle("-fx-text-fill: #e6a800;");     // เหลือง
                case "rejected" -> setStyle("-fx-text-fill: #c80000ff;");  // แดง
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

    private void setupEditStatusButton() {
        colEditStatus.setCellFactory(col -> new TableCell<>() {

            private final Button btn = new Button("แก้ไขสถานะ");
            private final VBox wrapper = new VBox(btn);

            {
                wrapper.setAlignment(javafx.geometry.Pos.CENTER);

                btn.setStyle("-fx-background-color: #ffa726; -fx-text-fill: white; -fx-background-radius: 5;");
                btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;"));
                btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #ffa726; -fx-text-fill: white;"));

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

 private void loadTenantsFromSupabase() {
    try {
        String bookingJson = supabase.selectAll("booking_demo_status");

        JSONArray bookingArr = new JSONArray(bookingJson);

        masterData.clear();

        for (int i = 0; i < bookingArr.length(); i++) {
            JSONObject booking = bookingArr.getJSONObject(i);

            int id = booking.optInt("id");
            String name = booking.optString("customer_name", "-");
            String zone = booking.optString("zone", "-");
            String status = booking.optString("status", "-");

            masterData.add(new Tenant(id, name, "-", "-", zone, status));
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
                masterData.filtered(tenant ->
                        (name.isEmpty() || tenant.getName().toLowerCase().contains(name)) &&
                        (phone.isEmpty() || tenant.getPhone().toLowerCase().contains(phone)) &&
                        (email.isEmpty() || tenant.getEmail().toLowerCase().contains(email)) &&
                        (status.equals("ทั้งหมด") || tenant.getStatus().equalsIgnoreCase(status))
                )
        );
    }

    private void showContactPopup(Tenant tenant) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("ติดต่อผู้เช่า");

        Label nameLabel = new Label("ชื่อลูกค้า: " + tenant.getName());
        Label phoneLabel = new Label("เบอร์โทร: " + tenant.getPhone());
        Label emailLabel = new Label("อีเมล์: " + tenant.getEmail());
        Label zoneLabel = new Label("โซน: " + tenant.getZone());
        Label statusLabel = new Label("สถานะ: " + tenant.getStatus());

        Button closeBtn = new Button("ปิด");
        closeBtn.setOnAction(e -> dialog.close());

        VBox vbox = new VBox(10, nameLabel, phoneLabel, emailLabel, zoneLabel, statusLabel, closeBtn);
        vbox.setStyle("-fx-padding: 20; -fx-background-color: #ffffff;");
        vbox.setPrefWidth(300);

        dialog.setScene(new Scene(vbox));
        dialog.showAndWait();
    }

   private void showEditStatusPopup(Tenant tenant) {
    Stage dialog = new Stage();
    dialog.initModality(Modality.APPLICATION_MODAL);
    dialog.setTitle("แก้ไขสถานะ");

    ComboBox<String> statusBox = new ComboBox<>();
    statusBox.getItems().addAll("pending", "approved", "rejected");
    statusBox.setValue(tenant.getStatus());

    Button saveBtn = new Button("บันทึก");
    saveBtn.setOnAction(e -> {
        try {
            // ⭐ อัปเดตสถานะใน Supabase
            supabase.updateStatusById("booking_demo_status", tenant.getId(), statusBox.getValue());

            // ⭐ อัปเดตใน UI
            tenant.setStatus(statusBox.getValue());
            applyFilters();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        dialog.close();
    });

    VBox vbox = new VBox(10, new Label("สถานะใหม่:"), statusBox, saveBtn);
    vbox.setStyle("-fx-padding: 20; -fx-background-color: #ffffff;");
    vbox.setPrefWidth(250);

    dialog.setScene(new Scene(vbox));
    dialog.showAndWait();
}

}

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
    public javafx.beans.property.IntegerProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public javafx.beans.property.StringProperty nameProperty() { return name; }

    public String getPhone() { return phone.get(); }
    public javafx.beans.property.StringProperty phoneProperty() { return phone; }

    public String getEmail() { return email.get(); }
    public javafx.beans.property.StringProperty emailProperty() { return email; }

    public String getZone() { return zone.get(); }
    public javafx.beans.property.StringProperty zoneProperty() { return zone; }

    public String getStatus() { return status.get(); }
    public void setStatus(String s) { status.set(s); }
    public javafx.beans.property.StringProperty statusProperty() { return status; }
}
