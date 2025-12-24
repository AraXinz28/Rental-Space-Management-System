package com.rental.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.URL;
import java.text.Collator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

public class BookingManagementController implements Initializable {

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
    @FXML private TableView<PaymentRecord> bookingTable;
    @FXML private TableColumn<PaymentRecord, String> colName;
    @FXML private TableColumn<PaymentRecord, String> colDate;
    @FXML private TableColumn<PaymentRecord, String> colZone;
    @FXML private TableColumn<PaymentRecord, String> colPayment;
    @FXML private TableColumn<PaymentRecord, String> colStatus;

    @FXML private VBox menu;

    private final ObservableList<PaymentRecord> masterData = FXCollections.observableArrayList();
    private FilteredList<PaymentRecord> filteredData;
    private SortedList<PaymentRecord> sortedData;

    // Supabase Config
    private static final String SUPABASE_URL = "https://sdmipxsxkquuyxvvqpho.supabase.co";
    private static final String SUPABASE_SERVICE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNkbWlweHN4a3F1dXl4dnZxcGhvIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc2NDc1MzU0NywiZXhwIjoyMDgwMzI5NTQ3fQ.IqSxzTLKHXlfGdH4RyzaYAIVXrlW7_LsrQEuJBlHJ8k";

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        menu.setPrefWidth(250);
        SplitPane.setResizableWithParent(menu, false);

        setupDropdowns();
        setupSort();
        setupTable();
        loadDataFromSupabase();
        setupFiltering();
    }

    private void setupDropdowns() {
        zoneCombo.getItems().addAll("A", "B", "C", "D", "E", "F", "G");
        zoneCombo.setPromptText("ทุกโซน");

        paymentCombo.getItems().addAll("QR PromptPay", "Bank Transfer");
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

    private void setupSort() {
        sortCombo.getItems().addAll("ชื่อ (ก → ฮ)", "ชื่อ (ฮ → ก)");
        sortBtn.setOnAction(e -> applySort());
    }

    private void applySort() {
        String option = sortCombo.getValue();
        if (option == null) return;

        bookingTable.getSortOrder().clear();

        if (option.contains("ก → ฮ")) {
            colName.setSortType(TableColumn.SortType.ASCENDING);
            bookingTable.getSortOrder().add(colName);
        } else {
            colName.setSortType(TableColumn.SortType.DESCENDING);
            bookingTable.getSortOrder().add(colName);
        }
    }

    private void setupTable() {
        colName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        colZone.setCellValueFactory(new PropertyValueFactory<>("stallId"));
        colPayment.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        Collator thaiCollator = Collator.getInstance(Locale.forLanguageTag("th-TH"));
        colName.setComparator(thaiCollator::compare);
        colPayment.setComparator(thaiCollator::compare);

        colStatus.setCellFactory(col -> new TableCell<PaymentRecord, String>() {
            private final ComboBox<String> combo = new ComboBox<>(
                FXCollections.observableArrayList("pending", "approved", "rejected")
            );

            {
                combo.setOnAction(e -> {
                    PaymentRecord record = getTableView().getItems().get(getIndex());
                    String newStatus = combo.getValue();

                    if (newStatus.equals(record.getStatus())) return;

                    if ("rejected".equals(newStatus)) {
                        String reason = showRejectReasonDialog();
                        if (reason == null) {
                            combo.setValue(record.getStatus());
                            return;
                        }
                        updatePaymentStatus(record.getId(), newStatus, reason);
                    } else {
                        updatePaymentStatus(record.getId(), newStatus, null);
                    }
                    record.setStatus(newStatus);
                });
            }

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    combo.setValue(status);
                    setGraphic(combo);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void loadDataFromSupabase() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                String url = SUPABASE_URL + "/rest/v1/payments?select=id,payment_method,payment_date,status,booking:booking_id(full_name,stall_id)";

                Request request = new Request.Builder()
                        .url(url)
                        .header("apikey", SUPABASE_SERVICE_KEY)
                        .header("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());
                    String body = response.body().string();
                    JsonArray array = gson.fromJson(body, JsonArray.class);

                    masterData.clear();
                    for (int i = 0; i < array.size(); i++) {
                        JsonObject obj = array.get(i).getAsJsonObject();
                        JsonObject booking = obj.getAsJsonObject("booking");

                        String fullName = booking != null ? booking.get("full_name").getAsString() : "ไม่ระบุ";
                        String stallId = booking != null ? booking.get("stall_id").getAsString() : "ไม่ระบุ";
                        String paymentMethod = obj.get("payment_method").getAsString();
                        String paymentDate = obj.get("payment_date").getAsString();
                        String status = obj.get("status").getAsString();
                        int id = obj.get("id").getAsInt();

                        String formattedDate = formatThaiDate(paymentDate);

                        masterData.add(new PaymentRecord(id, fullName, formattedDate, stallId, paymentMethod, status));
                    }
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            filteredData = new FilteredList<>(masterData, p -> true);
            sortedData = new SortedList<>(filteredData);
            sortedData.comparatorProperty().bind(bookingTable.comparatorProperty());
            bookingTable.setItems(sortedData);
        });

        task.setOnFailed(e -> showAlert(Alert.AlertType.ERROR, "ไม่สามารถโหลดข้อมูลได้"));

        new Thread(task).start();
    }

    private void setupFiltering() {
        Runnable applyFilter = () -> {
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
                    try {
                        LocalDate paymentDate = LocalDate.parse(p.getPaymentDate(), DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("th-TH")));
                        if (!paymentDate.equals(date)) return false;
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

    private void updatePaymentStatus(int paymentId, String status, String note) {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                JsonObject data = new JsonObject();
                data.addProperty("status", status);
                if (note != null) data.addProperty("reject_reason", note);

                RequestBody body = RequestBody.create(data.toString(), JSON_MEDIA_TYPE);

                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/payments?id=eq." + paymentId)
                        .patch(body)
                        .header("apikey", SUPABASE_SERVICE_KEY)
                        .header("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=minimal")
                        .build();

                try (Response resp = client.newCall(request).execute()) {
                    if (!resp.isSuccessful()) throw new Exception("อัปเดตไม่สำเร็จ");
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> showAlert(Alert.AlertType.INFORMATION, "อัปเดตสถานะสำเร็จ"));
        task.setOnFailed(e -> showAlert(Alert.AlertType.ERROR, "อัปเดตสถานะล้มเหลว"));

        new Thread(task).start();
    }

    private String showRejectReasonDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("ไม่อนุมัติการชำระเงิน");
        dialog.setHeaderText("กรุณาระบุเหตุผล");
        dialog.setContentText("เหตุผล:");
        return dialog.showAndWait().map(String::trim).filter(s -> !s.isEmpty()).orElse(null);
    }

    private String formatThaiDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("th-TH")));
        } catch (Exception e) {
            return dateStr;
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class PaymentRecord {
        private final int id;
        private final String fullName;
        private String paymentDate;
        private final String stallId;
        private final String paymentMethod;
        private String status;

        public PaymentRecord(int id, String fullName, String paymentDate, String stallId, String paymentMethod, String status) {
            this.id = id;
            this.fullName = fullName;
            this.paymentDate = paymentDate;
            this.stallId = stallId;
            this.paymentMethod = paymentMethod;
            this.status = status;
        }

        public int getId() { return id; }
        public String getFullName() { return fullName; }
        public String getPaymentDate() { return paymentDate; }
        public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }
        public String getStallId() { return stallId; }
        public String getPaymentMethod() { return paymentMethod; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}