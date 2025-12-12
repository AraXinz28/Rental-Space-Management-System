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

    @FXML private TableView<Tenant> tenantTable;
    @FXML private TableColumn<Tenant, String> colName;
    @FXML private TableColumn<Tenant, String> colPhone;
    @FXML private TableColumn<Tenant, String> colEmail;
    @FXML private TableColumn<Tenant, Void> colContact;

    private final ObservableList<Tenant> masterData = FXCollections.observableArrayList();
    private final ObservableList<Tenant> filteredData = FXCollections.observableArrayList();

   @FXML
public void initialize() {

    // ✅ กำหนดรายการ ComboBox ที่นี่
    pageSizeCombo.setItems(FXCollections.observableArrayList("10", "20", "50"));
    pageSizeCombo.getSelectionModel().select("10");

    colName.setCellValueFactory(data -> data.getValue().nameProperty());
    colPhone.setCellValueFactory(data -> data.getValue().phoneProperty());
    colEmail.setCellValueFactory(data -> data.getValue().emailProperty());

    setupContactButton();
    loadMockData();

    tenantTable.setItems(filteredData);

    // ✅ ล็อกคอลัมน์ไว้แค่ 4 ตัว ไม่ให้ JavaFX สร้างเพิ่ม
    tenantTable.getColumns().setAll(Arrays.asList(colName, colPhone, colEmail, colContact));
tenantTable.setColumnResizePolicy(param -> true);
}


    private void setupContactButton() {
        colContact.setCellFactory(col -> new TableCell<>() {

            private final Button btn = new Button("ติดต่อ");
            private final VBox wrapper = new VBox(btn); // ✅ จัดปุ่มให้อยู่ตรงกลาง

            {
                wrapper.setAlignment(javafx.geometry.Pos.CENTER);

                btn.setStyle("-fx-background-color: #7c7c7cff; -fx-text-fill: white; -fx-background-radius: 5;");

                btn.setOnMouseEntered(e ->
                        btn.setStyle("-fx-background-color: #5a5a5aff; -fx-text-fill: white; -fx-background-radius: 5;")
                );
                btn.setOnMouseExited(e ->
                        btn.setStyle("-fx-background-color: #7c7c7cff; -fx-text-fill: white; -fx-background-radius: 5;")
                );

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
                new Tenant("นายสมชาย ใจดี", "081-2345678", "somchai@gmail.com"),
                new Tenant("นางอัมพร สองใจ", "081-2345678", "amporn@gmail.com"),
                new Tenant("นางสาวร้อนตัว เย็นใจ", "081-2345678", "yenchai@gmail.com"),
                new Tenant("นายสิงสา ราสัตว์", "081-2345678", "singsa@gmail.com"),
                new Tenant("นายดำ คั่วพริกเกลือ", "081-2345678", "dam@gmail.com")
        );

        filteredData.setAll(masterData);
    }

    @FXML
    private void onFilterClick() {
        String name = nameField.getText() != null ? nameField.getText().trim() : "";
        String phone = phoneField.getText() != null ? phoneField.getText().trim() : "";
        String email = emailField.getText() != null ? emailField.getText().trim() : "";

        filteredData.setAll(
                masterData.filtered(tenant ->
                        (name.isEmpty()  || tenant.getName().contains(name)) &&
                        (phone.isEmpty() || tenant.getPhone().contains(phone)) &&
                        (email.isEmpty() || tenant.getEmail().contains(email))
                )
        );
    }

    @FXML
    private void onResetClick() {
        nameField.clear();
        phoneField.clear();
        emailField.clear();
        pageSizeCombo.getSelectionModel().select("10");
        filteredData.setAll(masterData);
    }

    private void showContactPopup(Tenant tenant) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("ติดต่อผู้เช่า");

        Label nameLabel = new Label("ชื่อลูกค้า: " + tenant.getName());
        Label phoneLabel = new Label("เบอร์โทร: " + tenant.getPhone());
        Label emailLabel = new Label("อีเมล์: " + tenant.getEmail());

        Button closeBtn = new Button("ปิด");

        closeBtn.setStyle("-fx-background-color: #cccccc; -fx-text-fill: black; -fx-background-radius: 5;");

        closeBtn.setOnMouseEntered(e ->
                closeBtn.setStyle("-fx-background-color: #aaaaaa; -fx-text-fill: black; -fx-background-radius: 5;")
        );
        closeBtn.setOnMouseExited(e ->
                closeBtn.setStyle("-fx-background-color: #cccccc; -fx-text-fill: black; -fx-background-radius: 5;")
        );

        closeBtn.setOnAction(e -> dialog.close());

        VBox vbox = new VBox(10, nameLabel, phoneLabel, emailLabel, closeBtn);
        vbox.setStyle("-fx-padding: 20; -fx-background-color: #ffffff;");
        vbox.setPrefWidth(300);

        Scene scene = new Scene(vbox);
        dialog.setScene(scene);
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

    public Tenant(String name, String phone, String email) {
        this.name.set(name);
        this.phone.set(phone);
        this.email.set(email);
    }

    public String getName() { return name.get(); }
    public javafx.beans.property.StringProperty nameProperty() { return name; }

    public String getPhone() { return phone.get(); }
    public javafx.beans.property.StringProperty phoneProperty() { return phone; }

    public String getEmail() { return email.get(); }
    public javafx.beans.property.StringProperty emailProperty() { return email; }
}
