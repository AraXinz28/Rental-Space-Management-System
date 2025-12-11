package com.rental.controller;

import com.rental.database.SupabaseClient;
import org.json.JSONArray;
import org.json.JSONObject;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

public class BookingManagementController {
    @FXML private HeaderController headerController;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> zoneCombo;
    @FXML private ComboBox<String> paymentCombo;
    @FXML private DatePicker datePicker;
    @FXML private Button resetBtn, saveBtn;
    @FXML private TableView<Booking> bookingTable;

    @FXML private TableColumn<Booking, String> colName, colDate, colZone, colPayment;
    @FXML private TableColumn<Booking, Boolean> colApprove;
    @FXML private TableColumn<Booking, Boolean> colReject;
    @FXML private VBox menu;

    private final ObservableList<Booking> masterData = FXCollections.observableArrayList();
    private FilteredList<Booking> filteredData;

    @FXML
    public void initialize() {
        menu.setPrefWidth(250);
        SplitPane.setResizableWithParent(menu, false);

        setupDropdowns();
        setupTable();
        loadDataFromSupabase();
        setupFiltering();
        setupButtons();
    }

    private void setupDropdowns() {
        zoneCombo.getItems().addAll("A", "B", "C", "D", "E", "F", "G");
        zoneCombo.setPromptText("ทุกโซน");

        paymentCombo.getItems().addAll("QR พร้อมเพย์", "โอนผ่านธนาคาร");
        paymentCombo.setPromptText("ทุกช่องทาง");
    }

    private void setupTable() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        colZone.setCellValueFactory(new PropertyValueFactory<>("zone"));
        colPayment.setCellValueFactory(new PropertyValueFactory<>("payment"));

        colApprove.setCellValueFactory(cellData -> cellData.getValue().approvedProperty());
        colApprove.setCellFactory(tc -> new RadioButtonTableCell<>(true));

        colReject.setCellValueFactory(cellData -> cellData.getValue().rejectedProperty());
        colReject.setCellFactory(tc -> new RadioButtonTableCell<>(false));

        bookingTable.setSelectionModel(null);
    }

    private void loadDataFromSupabase() {
        try {
            SupabaseClient client = new SupabaseClient();
            String json = client.selectAll("booking_demo_status");

            JSONArray array = new JSONArray(json);
            masterData.clear();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                int id = obj.optInt("id");
                String name = obj.optString("customer_name");
                String date = obj.optString("booking_date");
                String zone = obj.optString("zone");
                String payment = obj.optString("payment_method");
                String status = obj.optString("status");

                masterData.add(new Booking(id, name, date, zone, payment, status));
            }

            filteredData = new FilteredList<>(masterData, p -> true);
            bookingTable.setItems(filteredData);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupFiltering() {
        Runnable applyFilter = () -> {
            String keyword = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
            String zone = zoneCombo.getValue();
            String payment = paymentCombo.getValue();
            LocalDate date = datePicker.getValue();

            filteredData.setPredicate(booking -> {
                if (!keyword.isEmpty()) {
                    if (!booking.getName().toLowerCase().contains(keyword) &&
                        !booking.getZone().toLowerCase().contains(keyword))
                        return false;
                }
                if (zone != null && !booking.getZone().startsWith(zone)) return false;
                if (payment != null && !booking.getPayment().equals(payment)) return false;
                if (date != null) {
                    try {
                        LocalDate bDate = LocalDate.parse(booking.getDate());
                        if (!bDate.equals(date)) return false;
                    } catch (Exception ignored) {}
                }
                return true;
            });
        };

        searchField.textProperty().addListener((obs, old, newV) -> applyFilter.run());
        zoneCombo.valueProperty().addListener((obs, old, newV) -> applyFilter.run());
        paymentCombo.valueProperty().addListener((obs, old, newV) -> applyFilter.run());
        datePicker.valueProperty().addListener((obs, old, newV) -> applyFilter.run());
    }

    private void setupButtons() {
        resetBtn.setOnAction(e -> {
            searchField.clear();
            zoneCombo.setValue(null);
            paymentCombo.setValue(null);
            datePicker.setValue(null);
        });

        saveBtn.setOnAction(e -> {
            int approved = 0, rejected = 0;

            for (Booking b : masterData) {
                try {
                    SupabaseClient client = new SupabaseClient();

                    if (b.isApproved()) {
                        approved++;
                        b.setStatus("approved");
                        String jsonBody = "{\"status\":\"approved\"}";
                        client.update("booking_demo_status", "id", String.valueOf(b.getId()), jsonBody);
                    }
                    if (b.isRejected()) {
                        rejected++;
                        b.setStatus("rejected");
                        String jsonBody = "{\"status\":\"rejected\"}";
                        client.update("booking_demo_status", "id", String.valueOf(b.getId()), jsonBody);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("บันทึกสำเร็จ");
            alert.setHeaderText(null);
            alert.setContentText("อนุมัติ: " + approved + " รายการ\nไม่อนุมัติ: " + rejected + " รายการ");
            alert.showAndWait();

            loadDataFromSupabase();
        });
    }

    private static class RadioButtonTableCell<S> extends TableCell<S, Boolean> {
        private final RadioButton radioButton = new RadioButton();
        private final boolean isApproveColumn;

        public RadioButtonTableCell(boolean isApproveColumn) {
            this.isApproveColumn = isApproveColumn;
            setAlignment(Pos.CENTER);
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        }

        @Override
        protected void updateItem(Boolean value, boolean empty) {
            super.updateItem(value, empty);

            if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                setGraphic(null);
                return;
            }

            radioButton.setOnAction(null);
            radioButton.setSelected(Boolean.TRUE.equals(value));
            radioButton.setTooltip(new Tooltip(isApproveColumn ? "อนุมัติรายการนี้" : "ไม่อนุมัติรายการนี้"));

            radioButton.setOnAction(e -> {
                Booking booking = (Booking) getTableRow().getItem();
                if (booking == null) return;

                if (isApproveColumn) {
                    booking.setApproved(true);
                    booking.setRejected(false);
                    booking.setStatus("approved");
                } else {
                    booking.setRejected(true);
                    booking.setApproved(false);
                    booking.setStatus("rejected");
                }

                getTableView().refresh();
            });

            setGraphic(radioButton);
        }
    }

    public static class Booking {
        private final int id;
        private final String name, date, zone, payment;
        private String status;
        private final BooleanProperty approved = new SimpleBooleanProperty(false);
        private final BooleanProperty rejected = new SimpleBooleanProperty(false);

        public Booking(int id, String name, String date, String zone, String payment, String status) {
            this.id = id;
            this.name = name;
            this.date = date;
            this.zone = zone;
            this.payment = payment;
            this.status = status;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getDate() { return date; }
        public String getZone() { return zone; }
        public String getPayment() { return payment; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public boolean isApproved() { return approved.get(); }
        public BooleanProperty approvedProperty() { return approved; }
        public void setApproved(boolean v) { approved.set(v); }

        public boolean isRejected() { return rejected.get(); }
        public BooleanProperty rejectedProperty() { return rejected; }
        public void setRejected(boolean v) { rejected.set(v); }
    }
}
