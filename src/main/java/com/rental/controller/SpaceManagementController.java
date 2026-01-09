package com.rental.controller;

import org.json.JSONArray;
import org.json.JSONObject;

import com.rental.database.SupabaseClient;
import com.rental.model.Stall;
import com.rental.util.SceneManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

public class SpaceManagementController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo;

    @FXML private TableView<Stall> zoneTable;
    @FXML private TableColumn<Stall, String> colZoneName;
    @FXML private TableColumn<Stall, String> colSlot;
    @FXML private TableColumn<Stall, String> colSize;
    @FXML private TableColumn<Stall, String> colStatus;
    @FXML private TableColumn<Stall, LocalDateTime> colBookingDate;
    @FXML private TableColumn<Stall, Void> colAction;

    private final ObservableList<Stall> masterList = FXCollections.observableArrayList();
    private final SupabaseClient supabase = new SupabaseClient();

    @FXML
    private void initialize() {

        colZoneName.setCellValueFactory(new PropertyValueFactory<>("zoneName"));
        colSlot.setCellValueFactory(new PropertyValueFactory<>("stallId"));
        colSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colBookingDate.setCellValueFactory(new PropertyValueFactory<>("bookingDate"));

        setupStatusCell();
        setupBookingDateCell();
        setupActionColumn();

        loadStallData();


        searchField.textProperty().addListener((obs, o, n) -> handleSearch());
        statusCombo.valueProperty().addListener((obs, o, n) -> handleSearch());

        statusCombo.getItems().addAll(
            "ว่าง",
            "ถูกเช่า",
            "กำลังดำเนินการ",
            "ปิดปรับปรุง"
        );
    }

    /* ===== map สถานะ ===== */
    private String mapStatus(String thaiStatus) {
        if (thaiStatus == null) return null;

        return switch (thaiStatus) {
            case "ว่าง" -> "available";
            case "ถูกเช่า" -> "rented";
            case "กำลังดำเนินการ" -> "processing";
            case "ปิดปรับปรุง" -> "maintenance";
            default -> null;
        };
    }

    /* ===== โหลดข้อมูล ===== */
    private void loadStallData() {

    masterList.clear();

    try {
        /* ===== 1. โหลด booking ทั้งหมดครั้งเดียว ===== */
        String bookingRes = supabase.selectAll("bookings");
        JSONArray bookings = new JSONArray(bookingRes);

        // map: stall_id -> booking start date ล่าสุด
        Map<String, LocalDateTime> bookingDateMap = new HashMap<>();

        for (int i = 0; i < bookings.length(); i++) {
    JSONObject b = bookings.getJSONObject(i);

    if (b.isNull("start_date")) continue;

    
    String bookingStatus = b.optString("status");

   
    if (!"paid".equals(bookingStatus) && !"pending".equals(bookingStatus)) {
    continue;
    }

    String stallId = b.getString("stall_id");
    String startDateStr = b.getString("start_date");

    LocalDateTime startDate =
    LocalDate.parse(startDateStr)
        .atStartOfDay(ZoneOffset.UTC)    
        .withZoneSameInstant(ZoneId.of("Asia/Bangkok")) 
        .toLocalDateTime();

    // เก็บวันที่ล่าสุด
    if (!bookingDateMap.containsKey(stallId)
        || bookingDateMap.get(stallId).isBefore(startDate)) {

        bookingDateMap.put(stallId, startDate);
    }
}


        /* ===== 2. โหลด stalls ===== */
        String stallRes = supabase.selectAll("stalls");
        JSONArray stalls = new JSONArray(stallRes);

        for (int i = 0; i < stalls.length(); i++) {

            JSONObject obj = stalls.getJSONObject(i);

            String status = obj.getString("status");
            if ("closed".equals(status)) status = "maintenance";

            String stallId = obj.getString("stall_id");

            LocalDateTime bookingDate =
                bookingDateMap.getOrDefault(stallId, null);

            masterList.add(new Stall(
                obj.getString("zone_name"),
                stallId,
                obj.getString("size"),
                status,
                obj.getDouble("daily_rate"),
                bookingDate
            ));
        }

        /* ===== 3. sort ===== */
        masterList.sort((a, b) -> {

            int z = a.getZoneName().compareToIgnoreCase(b.getZoneName());
            if (z != 0) return z;

            int s = a.getStallId().compareToIgnoreCase(b.getStallId());
            if (s != 0) return s;

            if (a.getBookingDate() == null && b.getBookingDate() == null) return 0;
            if (a.getBookingDate() == null) return 1;
            if (b.getBookingDate() == null) return -1;

            return b.getBookingDate().compareTo(a.getBookingDate());
        });

        zoneTable.setItems(masterList);

    } catch (Exception e) {
        e.printStackTrace();
    }
}




    /* ===== cell สถานะ ===== */
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

                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }

                switch (status) {
                case "available" -> style("ว่าง", "#4F6A4A");
                case "processing" -> style("กำลังดำเนินการ", "#B7AE75");
                case "rented" -> style("ถูกเช่า", "#6F4949");
                case "maintenance", "closed" -> style("ปิดปรับปรุง", "#9E9E9E");
                default -> style(status, "#999999"); 
}

                setGraphic(label);
                setAlignment(Pos.CENTER);
            }

            private void style(String text, String color) {
                label.setText(text);
                label.setStyle(
                    "-fx-background-color:" + color + ";" +
                    "-fx-text-fill:white;" +
                    "-fx-padding:4 14;" +
                    "-fx-background-radius:20;"
                );
            }
        });
    }

    /* ===== cell วันที่ ===== */
    private void setupBookingDateCell() {

    DateTimeFormatter fmt =
    DateTimeFormatter.ofPattern(
        "d MMMM yyyy ",
        Locale.of("th", "TH")
    );

    colBookingDate.setCellFactory(col -> new TableCell<>() {
        @Override
        protected void updateItem(LocalDateTime date, boolean empty) {
            super.updateItem(date, empty);

            if (empty || date == null) {
                setText("-");
            } else {
                setText(fmt.format(date));
            }
        }
    });
}


    /* ===== ปุ่ม action ===== */
    private void setupActionColumn() {

        colAction.setCellFactory(col -> new TableCell<>() {

            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox box = new HBox(10, editBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER);

                editBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#9e9e9e;");
                deleteBtn.setStyle("-fx-background-color:transparent;-fx-text-fill:#6F4949;");

                editBtn.setOnAction(e ->
                    goToEdit(getTableView().getItems().get(getIndex()))
                );

                deleteBtn.setOnAction(e ->
                    confirmDelete(getTableView().getItems().get(getIndex()))
                );
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                Stall stall = getTableView().getItems().get(getIndex());

                boolean disable =
                    stall.getStatus().equals("rented") ||
                    stall.getStatus().equals("processing");

                deleteBtn.setDisable(disable);
                deleteBtn.setOpacity(disable ? 0.4 : 1);

                setGraphic(box);
            }
        });
    }

    /* ===== delete ===== */
    private void confirmDelete(Stall stall) {

        if (stall.getStatus().equals("rented")
         || stall.getStatus().equals("processing")) {
            new Alert(Alert.AlertType.WARNING,
                "ไม่สามารถลบพื้นที่ที่ถูกเช่าหรือกำลังดำเนินการได้")
                .showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setContentText("ต้องการลบพื้นที่นี้หรือไม่?");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) deleteStall(stall);
        });
    }

    private void deleteStall(Stall stall) {
        try {
            supabase.delete("stalls", "stall_id", stall.getStallId());
            loadStallData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ===== navigation ===== */
    private void goToEdit(Stall stall) {
    try {
        FXMLLoader loader =
            new FXMLLoader(getClass().getResource("/views/edit_space.fxml"));

        Parent root = loader.load();

       
        EditSpaceController controller = loader.getController();
        controller.setStallData(stall);

        Stage stage = (Stage) zoneTable.getScene().getWindow();
        stage.setScene(new Scene(root));

    } catch (Exception e) {
        e.printStackTrace();
        new Alert(
            Alert.AlertType.ERROR,
            "ไม่สามารถเปิดหน้าแก้ไขได้"
        ).showAndWait();
    }
    }


    @FXML
    private void goToZoneManagement() throws Exception {
        SceneManager.switchScene(
            (Stage) zoneTable.getScene().getWindow(),
            "/views/zone_management.fxml"
        );
    }

    @FXML
    private void goToAddSpace() throws Exception {
        SceneManager.switchScene(
            (Stage) zoneTable.getScene().getWindow(),
            "/views/add_space.fxml"
        );
    }

    /* ===== search ===== */
    @FXML
    private void handleSearch() {

        String keyword = searchField.getText() == null
            ? "" : searchField.getText().toLowerCase();

        String selectedStatus = mapStatus(statusCombo.getValue());

        ObservableList<Stall> filtered = FXCollections.observableArrayList();

        for (Stall s : masterList) {
            boolean matchKeyword =
                keyword.isEmpty()
                || s.getStallId().toLowerCase().contains(keyword)
                || s.getZoneName().toLowerCase().contains(keyword);

            boolean matchStatus =
                selectedStatus == null
                || s.getStatus().equals(selectedStatus);

            if (matchKeyword && matchStatus) {
                filtered.add(s);
            }
        }

        zoneTable.setItems(filtered);
    }

}
