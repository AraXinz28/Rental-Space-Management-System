package com.rental.controller;

import org.json.JSONArray;
import org.json.JSONObject;

import com.rental.database.SupabaseClient;
import com.rental.model.Stall;
import com.rental.util.SceneManager;

import java.time.LocalDate;
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
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class SpaceManagementController {

    /* ===== filters ===== */
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private DatePicker filterDate;

    /* ===== table ===== */
    @FXML private TableView<Stall> zoneTable;
    @FXML private TableColumn<Stall, String> colZoneName;
    @FXML private TableColumn<Stall, String> colSlot;
    @FXML private TableColumn<Stall, String> colSize;
    @FXML private TableColumn<Stall, String> colStatus;
    @FXML private TableColumn<Stall, String> colBookingDate;
    @FXML private TableColumn<Stall, Void> colAction;

    private final ObservableList<Stall> masterList = FXCollections.observableArrayList();
    private final SupabaseClient supabase = new SupabaseClient();

    /* ===== helper class ===== */
    private static class BookingRange {
        LocalDate start;
        LocalDate end;
    }

    @FXML
    private void initialize() {

        colZoneName.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getZoneName()));
        colSlot.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getStallId()));
        colSize.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getSize()));

        setupStatusCell();
        setupBookingDateCell();
        setupActionColumn();

        loadStallData();
        handleSearch();

        filterDate.setValue(LocalDate.now());
        filterDate.valueProperty().addListener((obs, o, n) -> {
            handleSearch();
            zoneTable.refresh(); 
        });

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

    /* ===== load data (รองรับหลายวัน) ===== */
    private void loadStallData() {

        masterList.clear();

        try {
            /* --- 1) โหลด bookings เป็นช่วง --- */
            String bookingRes = supabase.selectAll("bookings");
            JSONArray bookings = new JSONArray(bookingRes);

            Map<String, BookingRange> bookingMap = new HashMap<>();

            for (int i = 0; i < bookings.length(); i++) {
                JSONObject b = bookings.getJSONObject(i);

                if (b.isNull("start_date") || b.isNull("end_date")) continue;

                String bookingStatus = b.optString("status");
                if (!"paid".equals(bookingStatus) && !"pending".equals(bookingStatus)) continue;

                String stallId = b.getString("stall_id");

                LocalDate start = LocalDate.parse(b.getString("start_date"));
                LocalDate end   = LocalDate.parse(b.getString("end_date"));

                BookingRange r = new BookingRange();
                r.start = start;
                r.end = end;

                bookingMap.put(stallId, r);
            }

            /* --- 2) โหลด stalls --- */
            String stallRes = supabase.selectAll("stalls");
            JSONArray stalls = new JSONArray(stallRes);

            for (int i = 0; i < stalls.length(); i++) {

                JSONObject obj = stalls.getJSONObject(i);

                String status = obj.getString("status");
                if ("closed".equals(status)) status = "maintenance";

                String stallId = obj.getString("stall_id");

                BookingRange r = bookingMap.get(stallId);
                LocalDate start = r != null ? r.start : null;
                LocalDate end   = r != null ? r.end   : null;

                masterList.add(new Stall(
                    obj.getString("zone_name"),
                    stallId,
                    obj.getString("size"),
                    status,
                    obj.getDouble("daily_rate"),
                    start,
                    end
                ));
            }

            zoneTable.setItems(masterList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ===== status cell (อิงวันที่ที่เลือก) ===== */
    private void setupStatusCell() {
        colStatus.setCellFactory(col -> new TableCell<>() {

            private final Label label = new Label();

            {
                label.setMinWidth(120);
                label.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }

                Stall s = getTableRow().getItem();
                LocalDate selected = filterDate.getValue();

                boolean available = isAvailableOnDate(s, selected);

                if (available) {
                    style("ว่าง", "#4F6A4A");
                } else {
                    style("ถูกเช่า", "#6F4949");
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

    /* ===== booking date cell (แสดงช่วงวัน) ===== */
    private void setupBookingDateCell() {

            DateTimeFormatter fmt =
                DateTimeFormatter.ofPattern("d MMM yy", Locale.of("th", "TH"));

            colBookingDate.setCellFactory(col -> new TableCell<>() {
        @Override
        protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || getTableRow().getItem() == null) {
            setText("-");
            return;
        }

        Stall s = getTableRow().getItem();
        LocalDate start = s.getBookingStart();
        LocalDate end   = s.getBookingEnd();

        if (start == null || end == null) {
            setText("-");
        } else if (start.equals(end)) {
            setText(fmt.format(start));
        } else if (start.getMonth() == end.getMonth()) {
            // เดือนเดียวกัน
            setText(start.getDayOfMonth() + "–" + fmt.format(end));
        } else {
            // ข้ามเดือน
            setText(fmt.format(start) + " – " + fmt.format(end));
        }
    }
    });

    }

    /* ===== action column ===== */
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

                Stall s = getTableView().getItems().get(getIndex());
                boolean available = isAvailableOnDate(s, filterDate.getValue());

                deleteBtn.setDisable(!available);
                deleteBtn.setOpacity(available ? 1 : 0.4);

                setGraphic(box);
            }
        });
    }

    /* ===== availability (หัวใจ) ===== */
    private boolean isAvailableOnDate(Stall stall, LocalDate selectedDate) {

        if (selectedDate == null) return true;

        LocalDate start = stall.getBookingStart();
        LocalDate end   = stall.getBookingEnd();

        if (start == null || end == null) return true;

        return selectedDate.isBefore(start) || selectedDate.isAfter(end);
    }

    /* ===== search / filter ===== */
    @FXML
    private void handleSearch() {

        String keyword = searchField.getText() == null
            ? "" : searchField.getText().toLowerCase();

        String selectedStatus = mapStatus(statusCombo.getValue());
        LocalDate selectedDate = filterDate.getValue();

        ObservableList<Stall> filtered = FXCollections.observableArrayList();

        for (Stall s : masterList) {

            boolean matchKeyword =
                keyword.isEmpty()
                || s.getStallId().toLowerCase().contains(keyword)
                || s.getZoneName().toLowerCase().contains(keyword);

            boolean availableOnDate = isAvailableOnDate(s, selectedDate);

            boolean matchStatus;
            if (selectedStatus == null) {
                matchStatus = true;
            } else if (selectedStatus.equals("available")) {
                matchStatus = availableOnDate;
            } else if (selectedStatus.equals("rented")) {
                matchStatus = !availableOnDate;
            } else {
                matchStatus = s.getStatus().equals(selectedStatus);
            }

            if (matchKeyword && matchStatus) {
                filtered.add(s);
            }
        }

        /* --- sort: ว่างก่อน --- */
        filtered.sort((a, b) -> {

    // เรียงตาม Zone (A → Z)
    int z = a.getZoneName().compareToIgnoreCase(b.getZoneName());
    if (z != 0) return z;

    // ว่างก่อน / ถูกเช่าทีหลัง
    boolean aAvail = isAvailableOnDate(a, selectedDate);
    boolean bAvail = isAvailableOnDate(b, selectedDate);
    if (aAvail != bAvail) return aAvail ? -1 : 1;

    // เรียงรหัสพื้นที่แบบ numeric (A01, A02, A10)
    return compareStallId(a.getStallId(), b.getStallId());
    });



        zoneTable.setItems(filtered);
    }

    /* ===== delete ===== */
    private void confirmDelete(Stall stall) {
        if (!isAvailableOnDate(stall, filterDate.getValue())) {
            new Alert(Alert.AlertType.WARNING,
                "ไม่สามารถลบพื้นที่ที่ถูกจองในวันที่เลือกได้")
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
            new Alert(Alert.AlertType.ERROR, "ไม่สามารถเปิดหน้าแก้ไขได้").showAndWait();
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
    private int compareStallId(String a, String b) {

    // A01 → A + 01
    String aPrefix = a.replaceAll("\\d", "");
    String bPrefix = b.replaceAll("\\d", "");

    int p = aPrefix.compareToIgnoreCase(bPrefix);
    if (p != 0) return p;

    int aNum = Integer.parseInt(a.replaceAll("\\D", ""));
    int bNum = Integer.parseInt(b.replaceAll("\\D", ""));

    return Integer.compare(aNum, bNum);
}

}
