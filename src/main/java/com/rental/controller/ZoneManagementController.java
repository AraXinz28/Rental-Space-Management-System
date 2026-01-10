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
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

public class ZoneManagementController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo;

    @FXML private TableView<Zone> zoneTable;
    @FXML private TableColumn<Zone, String> colZoneName;
    @FXML private TableColumn<Zone, Integer> colSlot;
    @FXML private TableColumn<Zone, String> colStatus;
    @FXML private TableColumn<Zone, Void> colAction;

    private final ObservableList<Zone> masterList = FXCollections.observableArrayList();
    private final SupabaseClient supabase = new SupabaseClient();

    /* ================= INITIALIZE ================= */
    @FXML
    private void initialize() {

        colZoneName.setCellValueFactory(new PropertyValueFactory<>("zoneName"));
        colSlot.setCellValueFactory(new PropertyValueFactory<>("slotCount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("zoneStatus"));
        colAction.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(null));

        setupStatusCell();
        setupEditColumn();
        loadZoneData();

        searchField.textProperty().addListener((obs, o, n) -> handleSearch());
        statusCombo.valueProperty().addListener((obs, o, n) -> handleSearch());

        statusCombo.getItems().addAll("เปิดให้บริการ", "ปิดปรับปรุง");
    }

    /* ================= LOAD DATA ================= */
    private void loadZoneData() {
        masterList.clear();

        try {
            JSONArray arr = new JSONArray(supabase.selectAll("zone"));

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);

                masterList.add(new Zone(
                        obj.getInt("id"),
                        obj.getString("zone_name"),
                        obj.getInt("slot_count"),
                        obj.optString("zone_status", "")
                ));
            }

            masterList.sort((a, b) ->
                    a.getZoneName().compareToIgnoreCase(b.getZoneName()));

            zoneTable.setItems(masterList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= STATUS CELL ================= */
    private void setupStatusCell() {
        colStatus.setCellFactory(col -> new TableCell<>() {

            private final Label label = new Label();

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }

                label.setText(status);
                label.setStyle(getStatusStyle(status));
                setGraphic(label);
                setAlignment(Pos.CENTER);
            }
        });
    }

    private String getStatusStyle(String status) {
        return switch (status) {
            case "เปิดให้บริการ" ->
                "-fx-background-color:#4F6A4A;-fx-text-fill:white;-fx-padding:4 14;-fx-background-radius:20;";
            case "ปิดปรับปรุง" ->
                "-fx-background-color:#C0BDBD;-fx-text-fill:white;-fx-padding:4 14;-fx-background-radius:20;";
            default ->
                "-fx-background-color:#999;-fx-text-fill:white;-fx-padding:4 14;-fx-background-radius:20;";
        };
    }

    /* ================= EDIT COLUMN ================= */
    private void setupEditColumn() {
    colAction.setCellFactory(col -> new TableCell<>() {

        private final Button editBtn = new Button("Edit");
        private final Button deleteBtn = new Button("Delete");
        private final HBox box = new HBox(10, editBtn, deleteBtn);

        {
            box.setAlignment(Pos.CENTER);

            editBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:#9e9e9e;"
            );
            deleteBtn.setStyle(
                "-fx-background-color:transparent;-fx-text-fill:#B00020;"
            );

            editBtn.setOnAction(e ->
                goToEditZone(getTableView().getItems().get(getIndex()))
            );

            deleteBtn.setOnAction(e ->
                confirmDelete(getTableView().getItems().get(getIndex()))
            );
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : box);
        }
        });
    }

    private void confirmDelete(Zone zone) {
    try {
        /* ===== CHECK: มี stall ถูกเช่าอยู่ไหม ===== */
        String res = supabase.selectWhere(
            "stalls",
            "zone_name",
            zone.getZoneName()
        );

        JSONArray arr = new JSONArray(res);
        for (int i = 0; i < arr.length(); i++) {
            if ("rented".equals(arr.getJSONObject(i).getString("status"))) {
                new Alert(
                    Alert.AlertType.WARNING,
                    "ไม่สามารถลบโซนที่มีพื้นที่ถูกเช่าอยู่"
                ).showAndWait();
                return; // หยุดตรงนี้ ไม่ไปลบ
            }
        }

        /* ===== ถ้าไม่มี rented ค่อยถามยืนยัน ===== */
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("ยืนยันการลบ");
        alert.setHeaderText(null);
        alert.setContentText(
            "การลบโซนนี้จะลบพื้นที่ทั้งหมดในโซนด้วย\nต้องการดำเนินการต่อหรือไม่?"
        );

        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                deleteZoneWithStalls(zone);
            }
        });

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(
                Alert.AlertType.ERROR,
                "เกิดข้อผิดพลาดในการตรวจสอบข้อมูลพื้นที่"
            ).showAndWait();
        }
    }

    private void deleteZoneWithStalls(Zone zone) {
    try {
        /* ===== delete stalls (child) ===== */
        supabase.delete(
            "stalls",
            "zone_name",
            zone.getZoneName()
        );

        /* ===== delete zone (parent) ===== */
        supabase.delete(
            "zone",
            "id",
            String.valueOf(zone.getId())
        );

        loadZoneData(); // refresh table

        new Alert(
            Alert.AlertType.INFORMATION,
            "ลบโซนและพื้นที่เรียบร้อยแล้ว"
        ).showAndWait();

        } catch (Exception e) {
        e.printStackTrace();
        new Alert(
            Alert.AlertType.ERROR,
            "ไม่สามารถลบโซนได้"
            ).showAndWait();
        }
        
    }

    

    /* ================= NAVIGATION ================= */
    private void goToEditZone(Zone zone) {
        try {
            FXMLLoader loader =
                new FXMLLoader(getClass().getResource("/views/edit_zone.fxml"));
            Parent root = loader.load();

            loader.getController()
                  .getClass()
                  .cast(loader.getController());

            EditZoneController controller = loader.getController();
            controller.setZoneData(zone);

            Stage stage = (Stage) zoneTable.getScene().getWindow();
            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToAddZone() throws Exception {
        SceneManager.switchScene(
            (Stage) zoneTable.getScene().getWindow(),
            "/views/add_zone.fxml"
        );
    }

    @FXML
    private void goToSpaceManagement() throws Exception {
        SceneManager.switchScene(
            (Stage) zoneTable.getScene().getWindow(),
            "/views/space_management.fxml"
        );
    }

    /* ================= SEARCH ================= */
    @FXML
    private void handleSearch() {
        String keyword = searchField.getText() == null
                ? "" : searchField.getText().toLowerCase();

        String selectedStatus = statusCombo.getValue();

        ObservableList<Zone> filtered = FXCollections.observableArrayList();

        for (Zone z : masterList) {
            boolean matchKeyword =
                keyword.isEmpty()
                || z.getZoneName().toLowerCase().contains(keyword);

            boolean matchStatus =
                selectedStatus == null
                || z.getZoneStatus().equals(selectedStatus);

            if (matchKeyword && matchStatus) {
                filtered.add(z);
            }
        }

        zoneTable.setItems(filtered);
    }
}
