package com.rental.controller;

import com.rental.database.SupabaseClient;
import com.rental.model.Zone;
import com.rental.util.SceneManager;
import javafx.beans.property.ReadOnlyObjectWrapper;
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
import org.json.JSONArray;
import org.json.JSONObject;


public class ZoneManagementController {
    

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo;

    @FXML private TableView<Zone> zoneTable;
    @FXML private TableColumn<Zone, String> colZoneName;
    @FXML private TableColumn<Zone, Integer> colSlot;
    @FXML private TableColumn<Zone, Void> colAction;
    @FXML private TableColumn<Zone, String> colStatus;
    @FXML private Button zoneTab;
    @FXML private Button spaceTab;

    private ObservableList<Zone> masterList = FXCollections.observableArrayList();

    private final SupabaseClient supabase = new SupabaseClient();

    /* ===== INITIALIZE ===== */
    @FXML
    private void initialize() {
        colStatus.setCellValueFactory(
            new PropertyValueFactory<>("zoneStatus")
        );
        colZoneName.setCellValueFactory(
                new PropertyValueFactory<>("zoneName")
        );
        colSlot.setCellValueFactory(
                new PropertyValueFactory<>("slotCount")
        );
        colAction.setCellValueFactory(
                param -> new ReadOnlyObjectWrapper<>(null)
        );
        setupEditColumn();
        loadZoneData();
        setupStatusCell();


        
        statusCombo.getItems().addAll(
            "เปิดให้บริการ",
                        "ปิดปรับปรุง"
        );
    }
    /* ===== LOAD ZONE DATA ===== */
    private void loadZoneData() {

    masterList.clear();

        try {
            String response = supabase.selectAll("zone");
            JSONArray array = new JSONArray(response);

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                masterList.add(new Zone(
                        obj.getInt("id"),
                        obj.getString("zone_name"),
                        obj.getInt("slot_count"),
                        obj.optString("zone_status", "")
                ));
            }
            /* จัดเรียงตามชื่อโซน */
            masterList.sort((a, b) ->
        a.getZoneName().compareToIgnoreCase(b.getZoneName())
        );

            zoneTable.setItems(masterList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ===== EDIT COLUMN ===== */
    private void setupEditColumn() {
    colAction.setCellFactory(col -> new TableCell<>() {

        private final Button editBtn = new Button("Edit");

        {
            // ปุ่ม Edit 
            editBtn.setStyle("""
                -fx-background-color: transparent;
                -fx-text-fill: #9e9e9e;
                -fx-font-weight: bold;
                -fx-cursor: hand;
            """);

            editBtn.setOnMouseEntered(e ->
                editBtn.setStyle("""
                    -fx-background-color: transparent;
                    -fx-text-fill: #616161;
                    -fx-font-weight: bold;
                    -fx-cursor: hand;
                """)
            );

            editBtn.setOnMouseExited(e ->
                editBtn.setStyle("""
                    -fx-background-color: transparent;
                    -fx-text-fill: #9e9e9e;
                    -fx-font-weight: bold;
                    -fx-cursor: hand;
                """)
            );

            editBtn.setOnAction(e -> {
                Zone zone = getTableView().getItems().get(getIndex());
                goToEditZone(zone); 
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

    /* ===== GO EDIT ZONE ===== */
    private void goToEditZone(Zone zone) {
        try {
            FXMLLoader loader =
                new FXMLLoader(getClass().getResource("/views/edit_zone.fxml"));
            Parent root = loader.load();

            EditZoneController controller = loader.getController();
            controller.setZoneData(zone);

            Stage stage = (Stage) zoneTable.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /* ===== ADD ZONE ===== */
    @FXML
    private void goToAddZone() {
        try {
            Stage stage = (Stage) zoneTable.getScene().getWindow();
            SceneManager.switchScene(stage, "/views/add_zone.fxml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void setupStatusCell() {
        colStatus.setCellFactory(col -> new TableCell<>() {
        private final Label label = new Label();
        @Override
        protected void updateItem(String status, boolean empty) {
            super.updateItem(status, empty);

            if (empty || status == null || status.isBlank()) {
                setGraphic(null);
            } else {
                label.setText(status);
                label.setStyle(getStatusStyle(status));
                setGraphic(label);
                setAlignment(Pos.CENTER);
            }
        }
    });
    }
    private String getStatusStyle(String status) {
    return switch (status) {
        case "เปิดให้บริการ" ->
                "-fx-background-color:#4F6A4A;" +
                "-fx-text-fill:white;" +
                "-fx-padding:4 14;" +
                "-fx-background-radius:20;";
        case "ปิดปรับปรุง" ->
                "-fx-background-color:#B7AE75;" +
                "-fx-text-fill:white;" +
                "-fx-padding:4 14;" +
                "-fx-background-radius:20;";
        default ->
                "-fx-background-color:#999;" +
                "-fx-text-fill:white;" +
                "-fx-padding:4 14;" +
                "-fx-background-radius:20;";
    };
    }
    @FXML
    private void goToSpaceManagement() {
        try {
            Stage stage = (Stage) zoneTable.getScene().getWindow();
            SceneManager.switchScene(stage, "/views/space_management.fxml");
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

        ObservableList<Zone> filtered = FXCollections.observableArrayList();

        for (Zone zone : masterList) {

            boolean matchKeyword =
                    keyword.isEmpty()
                    || zone.getZoneName().toLowerCase().contains(keyword);

            boolean matchStatus =
                    selectedStatus == null
                    || selectedStatus.equals("ทุกสถานะ")
                    || zone.getZoneStatus().equals(selectedStatus);

            if (matchKeyword && matchStatus) {
                filtered.add(zone);
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
