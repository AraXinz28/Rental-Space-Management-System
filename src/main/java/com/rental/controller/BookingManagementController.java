package com.rental.controller;

import com.rental.database.SupabaseClient;
import org.json.JSONArray;
import org.json.JSONObject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.text.Collator;
import java.time.LocalDate;
import java.util.Locale;

public class BookingManagementController {

    @FXML private HeaderController headerController;

    // ===== Filter =====
    @FXML private TextField searchField;
    @FXML private ComboBox<String> zoneCombo;
    @FXML private ComboBox<String> paymentCombo;
    @FXML private DatePicker datePicker;
    @FXML private Button resetBtn;

    // ===== Sort =====
    @FXML private Button sortBtn;
    @FXML private ComboBox<String> sortCombo;

    // ===== Table =====
    @FXML private TableView<Booking> bookingTable;
    @FXML private TableColumn<Booking, String> colName;
    @FXML private TableColumn<Booking, String> colDate;
    @FXML private TableColumn<Booking, String> colZone;
    @FXML private TableColumn<Booking, String> colPayment;
    @FXML private TableColumn<Booking, String> colStatus;

    @FXML private VBox menu;

    private final ObservableList<Booking> masterData = FXCollections.observableArrayList();
    private FilteredList<Booking> filteredData;
    private SortedList<Booking> sortedData;

    // ================= INITIALIZE =================
    @FXML
    public void initialize() {
        menu.setPrefWidth(250);
        SplitPane.setResizableWithParent(menu, false);

        setupDropdowns();
        setupSort();
        setupTable();
        loadDataFromSupabase();
        setupFiltering();
    }

    // ================= DROPDOWNS =================
    private void setupDropdowns() {
        zoneCombo.getItems().addAll("A", "B", "C", "D", "E", "F", "G");
        zoneCombo.setPromptText("ทุกโซน");

        paymentCombo.getItems().addAll("QR พร้อมเพย์", "โอนผ่านธนาคาร");
        paymentCombo.setPromptText("ทุกช่องทาง");

        resetBtn.setOnAction(e -> {
            searchField.clear();
            zoneCombo.setValue(null);
            paymentCombo.setValue(null);
            datePicker.setValue(null);

            sortCombo.setValue(null);
            bookingTable.getSortOrder().clear();
        });
    }

    // ================= SORT =================
    private void setupSort() {
        sortCombo.getItems().addAll(
            "ชื่อ (ก → ฮ)",
            "ชื่อ (ฮ → ก)"
            
        );

        sortBtn.setOnAction(e -> applySort());

        // ถ้าอยากให้เลือกแล้วเรียงทันที
        // sortCombo.valueProperty().addListener((o, a, b) -> applySort());
    }

    private void applySort() {
        String option = sortCombo.getValue();
        if (option == null) return;

        bookingTable.getSortOrder().clear();

        switch (option) {
            case "ชื่อ (ก → ฮ)" -> {
                colName.setSortType(TableColumn.SortType.ASCENDING);
                bookingTable.getSortOrder().add(colName);
            }
            case "ชื่อ (ฮ → ก)" -> {
                colName.setSortType(TableColumn.SortType.DESCENDING);
                bookingTable.getSortOrder().add(colName);
            }
           
            
        }
    }

    // ================= TABLE =================
    private void setupTable() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colZone.setCellValueFactory(new PropertyValueFactory<>("zone"));
        colPayment.setCellValueFactory(new PropertyValueFactory<>("payment"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // ----- Thai Sort -----
        Collator thaiCollator = Collator.getInstance(Locale.forLanguageTag("th-TH"));
        colName.setComparator(thaiCollator::compare);
        colPayment.setComparator(thaiCollator::compare);

        // ----- Status Dropdown -----
        colStatus.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<String> combo = new ComboBox<>(
                FXCollections.observableArrayList("รอดำเนินการ", "อนุมัติ", "ไม่อนุมัติ")
            );

            {
                combo.setOnAction(e -> {
                    Booking booking = getTableView().getItems().get(getIndex());
                    String newDbStatus = mapToDbStatus(combo.getValue());

                    if (newDbStatus.equals(booking.getStatus())) return;

                    if ("rejected".equals(newDbStatus)) {
                        String reason = showRejectReasonDialog();
                        if (reason == null) {
                            combo.setValue(mapToUiStatus(booking.getStatus()));
                            return;
                        }
                        booking.setStatus("rejected");
                        booking.setRejectReason(reason);
                        updateRejectToSupabase(booking.getId(), reason);
                    } else {
                        booking.setStatus(newDbStatus);
                        booking.setRejectReason(null);
                        updateStatusToSupabase(booking.getId(), newDbStatus);
                    }
                });
            }

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    combo.setValue(mapToUiStatus(status));
                    setGraphic(combo);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    // ================= LOAD DATA =================
    private void loadDataFromSupabase() {
        try {
            SupabaseClient client = new SupabaseClient();
            String json = client.selectAll("booking_demo_status");

            JSONArray array = new JSONArray(json);
            masterData.clear();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                masterData.add(new Booking(
                    obj.optInt("id"),
                    obj.optString("customer_name"),
                    obj.optString("booking_date"),
                    obj.optString("zone"),
                    obj.optString("payment_method"),
                    obj.optString("status", "pending"),
                    obj.optString("reject_reason", null)
                ));
            }

            filteredData = new FilteredList<>(masterData, p -> true);
            sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(bookingTable.comparatorProperty());

            bookingTable.setItems(sortedData);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= FILTER =================
    private void setupFiltering() {
        Runnable applyFilter = () -> {
            String keyword = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
            String zone = zoneCombo.getValue();
            String payment = paymentCombo.getValue();
            LocalDate date = datePicker.getValue();

            filteredData.setPredicate(b -> {
                if (!keyword.isEmpty()) {
                    if (!b.getName().toLowerCase().contains(keyword)
                        && !b.getZone().toLowerCase().contains(keyword)) return false;
                }
                if (zone != null && !b.getZone().startsWith(zone)) return false;
                if (payment != null && !b.getPayment().equals(payment)) return false;
                if (date != null) {
                    try {
                        if (!LocalDate.parse(b.getDate()).equals(date)) return false;
                    } catch (Exception ignored) {}
                }
                return true;
            });
        };

        searchField.textProperty().addListener((o, a, b) -> applyFilter.run());
        zoneCombo.valueProperty().addListener((o, a, b) -> applyFilter.run());
        paymentCombo.valueProperty().addListener((o, a, b) -> applyFilter.run());
        datePicker.valueProperty().addListener((o, a, b) -> applyFilter.run());
    }

    // ================= DIALOG =================
    private String showRejectReasonDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("ไม่อนุมัติรายการ");
        dialog.setHeaderText("กรุณาระบุเหตุผล");
        dialog.setContentText("เหตุผล:");
        return dialog.showAndWait()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);
    }

    // ================= SUPABASE =================
    private void updateStatusToSupabase(int id, String status) {
        try {
            new SupabaseClient().update(
                "booking_demo_status",
                "id",
                String.valueOf(id),
                "{\"status\":\"" + status + "\",\"reject_reason\":null}"
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateRejectToSupabase(int id, String reason) {
        try {
            new SupabaseClient().update(
                "booking_demo_status",
                "id",
                String.valueOf(id),
                String.format("{\"status\":\"rejected\",\"reject_reason\":\"%s\"}",
                        reason.replace("\"", "\\\""))
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= MAPPING =================
    private String mapToDbStatus(String ui) {
        return switch (ui) {
            case "อนุมัติ" -> "approved";
            case "ไม่อนุมัติ" -> "rejected";
            default -> "pending";
        };
    }

    private String mapToUiStatus(String db) {
        return switch (db) {
            case "approved" -> "อนุมัติ";
            case "rejected" -> "ไม่อนุมัติ";
            default -> "รอดำเนินการ";
        };
    }

    // ================= MODEL =================
    public static class Booking {
        private final int id;
        private final String name, date, zone, payment;
        private String status;
        private String rejectReason;

        public Booking(int id, String name, String date, String zone,
                       String payment, String status, String rejectReason) {
            this.id = id;
            this.name = name;
            this.date = date;
            this.zone = zone;
            this.payment = payment;
            this.status = status;
            this.rejectReason = rejectReason;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getDate() { return date; }
        public String getZone() { return zone; }
        public String getPayment() { return payment; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRejectReason() { return rejectReason; }
        public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    }
}
