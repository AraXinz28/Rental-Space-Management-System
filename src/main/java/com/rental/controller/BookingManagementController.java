package com.rental.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import okhttp3.*;

import java.io.IOException;
import java.net.URL;
import java.text.Collator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class BookingManagementController implements Initializable {

    @FXML private HeaderController headerController;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> zoneCombo;
    @FXML private ComboBox<String> paymentCombo;
    @FXML private DatePicker datePicker;
    @FXML private Button resetBtn;
    @FXML private Button sortBtn;
    @FXML private ComboBox<String> sortCombo;
    @FXML private TableView<PaymentRecord> bookingTable;
    @FXML private TableColumn<PaymentRecord, String> colName;
    @FXML private TableColumn<PaymentRecord, String> colDate;
    @FXML private TableColumn<PaymentRecord, String> colZone;
    @FXML private TableColumn<PaymentRecord, String> colStartDate; 
    @FXML private TableColumn<PaymentRecord, String> colEndDate;   
    @FXML private TableColumn<PaymentRecord, String> colPayment;
    @FXML private TableColumn<PaymentRecord, String> colStatus;
    @FXML private VBox menu;

    private final ObservableList<PaymentRecord> masterData = FXCollections.observableArrayList();
    private FilteredList<PaymentRecord> filteredData;
    private SortedList<PaymentRecord> sortedData;

    private static final String SUPABASE_URL = "https://sdmipxsxkquuyxvvqpho.supabase.co";
    private static final String SUPABASE_SERVICE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNkbWlweHN4a3F1dXl4dnZxcGhvIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc2NDc1MzU0NywiZXhwIjoyMDgwMzI5NTQ3fQ.IqSxzTLKHXlfGdH4RyzaYAIVXrlW7_LsrQEuJBlHJ8k";

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupDropdowns();
        setupSort();
        loadDataFromSupabase();
        setupFiltering();
    }

    private void setupTable() {
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        colZone.setCellValueFactory(new PropertyValueFactory<>("stallId"));
        colStartDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colEndDate.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        colPayment.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // แก้ไข Deprecated: ใช้ Locale.of สำหรับ Java รุ่นใหม่
        Collator thaiCollator = Collator.getInstance(Locale.of("th", "TH"));
        colName.setComparator(thaiCollator::compare);

        colStatus.setCellFactory(col -> new TableCell<>() {
            private final ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList("pending", "approved", "rejected"));
            {
                combo.setOnAction(e -> {
                    if (getTableRow() != null && getTableRow().getItem() != null) {
                        PaymentRecord record = getTableRow().getItem();
                        if (!combo.getValue().equals(record.getStatus())) {
                            updatePaymentStatus(record.getId(), combo.getValue(), null);
                            record.setStatus(combo.getValue());
                        }
                    }
                });
            }
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) setGraphic(null);
                else {
                    combo.setValue(status);
                    setGraphic(combo);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void loadDataFromSupabase() {
        Task<ObservableList<PaymentRecord>> task = new Task<>() {
            @Override
            protected ObservableList<PaymentRecord> call() throws Exception {
                // ดึงข้อมูลฟิลด์ใหม่ให้ครบตาม FXML
                String url = SUPABASE_URL + "/rest/v1/payments?select=id,payment_method,payment_date,status,start_date,end_date,booking:booking_id(full_name,stall_id)";
                
                Request request = new Request.Builder()
                        .url(url)
                        .header("apikey", SUPABASE_SERVICE_KEY)
                        .header("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                        .build();

                ObservableList<PaymentRecord> tempList = FXCollections.observableArrayList();
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                    
                    JsonArray array = gson.fromJson(response.body().string(), JsonArray.class);
                    for (int i = 0; i < array.size(); i++) {
                        JsonObject obj = array.get(i).getAsJsonObject();
                        JsonObject booking = (obj.has("booking") && !obj.get("booking").isJsonNull()) ? obj.getAsJsonObject("booking") : null;

                        String fullName = (booking != null && booking.has("full_name")) ? booking.get("full_name").getAsString() : "-";
                        String stallId = (booking != null && booking.has("stall_id")) ? booking.get("stall_id").getAsString() : "-";
                        
                        tempList.add(new PaymentRecord(
                                obj.get("id").getAsInt(),
                                fullName,
                                formatThaiDate(getJsonStr(obj, "payment_date")),
                                stallId,
                                getJsonStr(obj, "payment_method"),
                                getJsonStr(obj, "status"),
                                formatThaiDate(getJsonStr(obj, "start_date")),
                                formatThaiDate(getJsonStr(obj, "end_date"))
                        ));
                    }
                }
                return tempList;
            }
        };

        task.setOnSucceeded(e -> {
            masterData.setAll(task.getValue());
            if (filteredData == null) {
                filteredData = new FilteredList<>(masterData, p -> true);
                sortedData = new SortedList<>(filteredData);
                sortedData.comparatorProperty().bind(bookingTable.comparatorProperty());
                bookingTable.setItems(sortedData);
            }
        });

        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            showAlert(Alert.AlertType.ERROR, "โหลดข้อมูลล้มเหลว");
        });

        new Thread(task).start();
    }

    private String getJsonStr(JsonObject obj, String key) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : "";
    }

    private String formatThaiDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "-";
        try {
            // ตัดเอาเฉพาะส่วนวันที่ในกรณีมีเวลาติดมา (ISO Format)
            String datePart = dateStr.contains("T") ? dateStr.split("T")[0] : dateStr;
            LocalDate date = LocalDate.parse(datePart);
            return date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.of("th", "TH")));
        } catch (Exception e) {
            return dateStr;
        }
    }

    private void setupFiltering() {
        Runnable applyFilter = () -> {
            if (filteredData == null) return;
            String keyword = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
            String zone = zoneCombo.getValue();
            String payment = paymentCombo.getValue();
            LocalDate date = datePicker.getValue();

            filteredData.setPredicate(p -> {
                if (!keyword.isEmpty()) {
                    if (!p.getFullName().toLowerCase().contains(keyword) && !p.getStallId().toLowerCase().contains(keyword)) return false;
                }
                if (zone != null && !p.getStallId().startsWith(zone)) return false;
                if (payment != null && !p.getPaymentMethod().contains(payment.replace("พร้อมเพย์", "PromptPay"))) return false;
                if (date != null) {
                    String formattedPicker = date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.of("th", "TH")));
                    if (!p.getPaymentDate().equals(formattedPicker)) return false;
                }
                return true;
            });
        };

        searchField.textProperty().addListener((o, a, b) -> applyFilter.run());
        zoneCombo.valueProperty().addListener((o, a, b) -> applyFilter.run());
        paymentCombo.valueProperty().addListener((o, a, b) -> applyFilter.run());
        datePicker.valueProperty().addListener((o, a, b) -> applyFilter.run());
    }

    private void updatePaymentStatus(int id, String status, String note) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                JsonObject data = new JsonObject();
                data.addProperty("status", status);
                RequestBody body = RequestBody.create(data.toString(), JSON_MEDIA_TYPE);
                Request req = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/payments?id=eq." + id)
                        .patch(body)
                        .header("apikey", SUPABASE_SERVICE_KEY)
                        .header("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                        .build();
                try (Response resp = client.newCall(req).execute()) {
                    if (!resp.isSuccessful()) throw new IOException("Update failed");
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void setupDropdowns() {
        zoneCombo.getItems().addAll("A", "B", "C", "D", "E", "F", "G");
        paymentCombo.getItems().addAll("QR PromptPay", "Bank Transfer");
        resetBtn.setOnAction(e -> {
            searchField.clear();
            zoneCombo.setValue(null);
            paymentCombo.setValue(null);
            datePicker.setValue(null);
        });
    }

    private void setupSort() {
        sortCombo.getItems().addAll("ชื่อ (ก → ฮ)", "ชื่อ (ฮ → ก)");
        sortBtn.setOnAction(e -> {
            String option = sortCombo.getValue();
            if (option == null) return;
            bookingTable.getSortOrder().clear();
            colName.setSortType(option.contains("ก → ฮ") ? TableColumn.SortType.ASCENDING : TableColumn.SortType.DESCENDING);
            bookingTable.getSortOrder().add(colName);
        });
    }

    private void showAlert(Alert.AlertType type, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public static class PaymentRecord {
        private final int id;
        private final String fullName;
        private final String paymentDate;
        private final String stallId;
        private final String paymentMethod;
        private String status;
        private final String startDate;
        private final String endDate;

        public PaymentRecord(int id, String fullName, String paymentDate, String stallId, String paymentMethod, String status, String startDate, String endDate) {
            this.id = id;
            this.fullName = fullName;
            this.paymentDate = paymentDate;
            this.stallId = stallId;
            this.paymentMethod = paymentMethod;
            this.status = status;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        public int getId() { return id; }
        public String getFullName() { return fullName; }
        public String getPaymentDate() { return paymentDate; }
        public String getStallId() { return stallId; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStartDate() { return startDate; }
        public String getEndDate() { return endDate; }
    }
}