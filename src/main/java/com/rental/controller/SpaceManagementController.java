package com.rental.controller;

import org.json.JSONArray;
import org.json.JSONObject;

import com.rental.database.SupabaseClient;
import com.rental.model.Stall;
import com.rental.util.SceneManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class SpaceManagementController {
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo;

    @FXML private TableView<Stall> zoneTable;
    @FXML private TableColumn<Stall, String> colZoneName;
    @FXML private TableColumn<Stall, String> colSlot;
    @FXML private TableColumn<Stall, String> colSize;
    @FXML private TableColumn<Stall, String> colStatus;
    @FXML private TableColumn<Stall, Void> colAction;
    
    private ObservableList<Stall> masterList = FXCollections.observableArrayList();

    private final SupabaseClient supabase = new SupabaseClient();

    @FXML
    private void initialize() {
        colZoneName.setCellValueFactory(new PropertyValueFactory<>("zoneName"));
        colSlot.setCellValueFactory(new PropertyValueFactory<>("stallId"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        setupStatusCell();
        setupActionColumn();
        loadStallData();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        statusCombo.valueProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        
        statusCombo.getItems().addAll(
            "available",
            "rented",
            "maintenance",
            "closed"
        );

    }

    /* ===== โหลดข้อมูลพื้นที่ ===== */
    private void loadStallData() {
    masterList.clear();

    try {
        String response = supabase.selectAll("stalls");
        JSONArray array = new JSONArray(response);

        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);

            masterList.add(new Stall(
                    obj.getString("zone_name"),
                    obj.getString("stall_id"),
                    obj.getString("size"),
                    obj.getString("status"),
                    obj.getDouble("daily_rate"),
                    obj.optString("amenities", "")
            ));
        }

        zoneTable.setItems(masterList);

    } catch (Exception e) {
        e.printStackTrace();
    }
}


    /* ===== สถานะ ===== */
    private void setupStatusCell() {
    colStatus.setCellFactory(col -> new TableCell<>() {

        private final Label label = new Label();

        {
            label.setMinWidth(120);
            label.setAlignment(Pos.CENTER);
        }

        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);

            if (empty || status == null || status.isBlank()) {
                setGraphic(null);
                return;
            }

            switch (status) {
                case "maintenance" -> {
                    label.setText("กำลังดำเนินการ");
                    label.setStyle(
                        "-fx-background-color:#B7AE75;" +
                        "-fx-text-fill:white;" +
                        "-fx-padding:4 14;" +
                        "-fx-background-radius:20;"
                    );
                }
                case "rented" -> {
                    label.setText("ถูกเช่า");
                    label.setStyle(
                        "-fx-background-color:#6F4949;" +
                        "-fx-text-fill:white;" +
                        "-fx-padding:4 14;" +
                        "-fx-background-radius:20;"
                    );
                }
                case "closed" -> {
                    label.setText("ปิดปรับปรุง");
                    label.setStyle(
                        "-fx-background-color:#9E9E9E;" +
                        "-fx-text-fill:white;" +
                        "-fx-padding:4 14;" +
                        "-fx-background-radius:20;"
                    );
                }
                default -> {
                    label.setText("ว่าง");
                    label.setStyle(
                        "-fx-background-color:#4F6A4A;" +
                        "-fx-text-fill:white;" +
                        "-fx-padding:4 14;" +
                        "-fx-background-radius:20;"
                    );
                }
            }

            setGraphic(label);        
            setAlignment(Pos.CENTER); 
        }
    });
}


    /* ===== ปุ่ม Edit ===== */
    private void setupActionColumn() {
        colAction.setCellFactory(col -> new TableCell<>() {

            private final Button editBtn = new Button("Edit");

            {
                editBtn.setStyle("""
                    -fx-background-color: transparent;
                    -fx-text-fill: #9e9e9e;
                    -fx-font-weight: bold;
                    -fx-cursor: hand;
                """);

                editBtn.setOnAction(e -> {
                    Stall stall = getTableView().getItems().get(getIndex());

                    try {
                        FXMLLoader loader =
                                new FXMLLoader(getClass().getResource("/views/edit_space.fxml"));
                        Parent root = loader.load();

                        EditSpaceController controller = loader.getController();
                        controller.setStallData(stall);

                        Stage stage = (Stage) zoneTable.getScene().getWindow();
                        stage.setScene(new Scene(root));

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : editBtn);
                setAlignment(Pos.CENTER);
            }
        });
    }

    /* ===== Navigation ===== */
    @FXML
    private void goToZoneManagement() {
        try {
            Stage stage = (Stage) zoneTable.getScene().getWindow();
            SceneManager.switchScene(stage, "/views/zone_management.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToAddSpace() {
        try {
            Stage stage = (Stage) zoneTable.getScene().getWindow();
            SceneManager.switchScene(stage, "/views/add_space.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handleSearch() {

        String keyword = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase();

        String selectedStatus = statusCombo.getValue();

        ObservableList<Stall> filtered = FXCollections.observableArrayList();

        for (Stall stall : masterList) {

            boolean matchKeyword =
                    keyword.isEmpty()
                    || stall.getStallId().toLowerCase().contains(keyword)
                    || stall.getZoneName().toLowerCase().contains(keyword);

            boolean matchStatus =
                    selectedStatus == null
                    || selectedStatus.equals("ทุกสถานะ")
                    || stall.getStatus().equals(selectedStatus);

            if (matchKeyword && matchStatus) {
                filtered.add(stall);
            }
        }

        zoneTable.setItems(filtered);
    }

    @FXML
    private void handleReset() {
    searchField.clear();
    statusCombo.setValue(null);
    zoneTable.setItems(masterList);
}

}
