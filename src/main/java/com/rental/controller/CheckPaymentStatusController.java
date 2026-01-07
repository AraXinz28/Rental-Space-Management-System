package com.rental.controller;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class CheckPaymentStatusController {

    /* ================= FXML ================= */
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbMethod;
    @FXML private ComboBox<String> cbSort;
    @FXML private DatePicker dpDate;

    @FXML private TableView<PaymentRow> paymentTable;
    @FXML private TableColumn<PaymentRow, String> colShop;
    @FXML private TableColumn<PaymentRow, String> colLock;
    @FXML private TableColumn<PaymentRow, String> colMethod;
    @FXML private TableColumn<PaymentRow, String> colDate;
    @FXML private TableColumn<PaymentRow, Double> colDeposit;
    @FXML private TableColumn<PaymentRow, Double> colRent;
    @FXML private TableColumn<PaymentRow, Double> colTotal;

    @FXML private Label lblMethod;
    @FXML private Label lblDeposit;
    @FXML private Label lblRent;
    @FXML private Label lblTotal;
    @FXML private TextArea txtNote;
    @FXML private ImageView slipImage;

    /* ================= DATA ================= */
    private final ObservableList<PaymentRow> masterData =
            FXCollections.observableArrayList();

    private FilteredList<PaymentRow> filteredData;

    private static final String SUPABASE_URL =
            "https://sdmipxsxkquuyxvvqpho.supabase.co";

    private static final String SUPABASE_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNkbWlweHN4a3F1dXl4dnZxcGhvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQ3NTM1NDcsImV4cCI6MjA4MDMyOTU0N30.AG8XwFmTuMPXZe5bjv2YqeIcfvKFRf95CJLDhfDHp0E";

    @FXML
    public void initialize() {

        /* ===== ComboBox ===== */
        cbMethod.getItems().addAll("ทั้งหมด", "QR PromptPay", "Bank Transfer");
        cbMethod.setValue("ทั้งหมด");

        cbSort.getItems().addAll(
                "ชื่อลูกค้า (ก → ฮ)",
                "ชื่อลูกค้า (ฮ → ก)"
        );
        cbSort.setValue("ชื่อลูกค้า (ก → ฮ)");

        /* ===== Table ===== */
        colShop.setCellValueFactory(c -> c.getValue().shopProperty());
        colLock.setCellValueFactory(c -> c.getValue().lockProperty());
        colMethod.setCellValueFactory(c -> c.getValue().methodProperty());
        colDate.setCellValueFactory(c -> c.getValue().dateProperty());
        colDeposit.setCellValueFactory(c -> c.getValue().depositProperty().asObject());
        colRent.setCellValueFactory(c -> c.getValue().rentProperty().asObject());
        colTotal.setCellValueFactory(c -> c.getValue().totalProperty().asObject());

        filteredData = new FilteredList<>(masterData, p -> true);
        paymentTable.setItems(filteredData);

        paymentTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, row) -> showDetail(row)
        );

        /* ===== REAL-TIME FILTER ===== */
        txtSearch.textProperty().addListener((obs, o, n) -> handleFilter());
        cbMethod.valueProperty().addListener((obs, o, n) -> handleFilter());
        dpDate.valueProperty().addListener((obs, o, n) -> handleFilter());
        cbSort.valueProperty().addListener((obs, o, n) -> handleFilter());

        clearDetail();

        new Thread(this::loadPayments).start();
    }

    /* ================= FILTER + SORT ================= */

    @FXML
    private void handleFilter() {

        String keyword = txtSearch.getText().trim();
        String method = cbMethod.getValue();
        String date = dpDate.getValue() == null ? null : dpDate.getValue().toString();

        filteredData.setPredicate(row -> {

            boolean matchKeyword =
                    keyword.isEmpty()
                    || row.getShop().contains(keyword)
                    || row.getLock().contains(keyword);

            boolean matchMethod =
                    method.equals("ทั้งหมด")
                    || row.getMethod().equals(method);

            boolean matchDate =
                    date == null
                    || row.getDate().equals(date);

            return matchKeyword && matchMethod && matchDate;
        });

        applySort();
    }

    private void applySort() {

        paymentTable.getSortOrder().clear();

        if ("ชื่อลูกค้า (ก → ฮ)".equals(cbSort.getValue())) {
            colShop.setSortType(TableColumn.SortType.ASCENDING);
            paymentTable.getSortOrder().add(colShop);
        }

        if ("ชื่อลูกค้า (ฮ → ก)".equals(cbSort.getValue())) {
            colShop.setSortType(TableColumn.SortType.DESCENDING);
            paymentTable.getSortOrder().add(colShop);
        }
    }

    /* ================= LOAD DATA ================= */

    private void loadPayments() {
        try {

            String url = SUPABASE_URL +
                    "/rest/v1/payments" +
                    "?select=id,booking_id,payment_method,payment_date,amount,proof_image_url,note";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + SUPABASE_KEY)
                    .GET()
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request, HttpResponse.BodyHandlers.ofString()
                    );

            JsonArray payments = JsonParser.parseString(response.body()).getAsJsonArray();

            for (int i = 0; i < payments.size(); i++) {
                JsonObject p = payments.get(i).getAsJsonObject();

                long bookingId = p.get("booking_id").getAsLong();
                String method = p.get("payment_method").getAsString();
                String date = p.get("payment_date").getAsString();
                double total = p.get("amount").getAsDouble();
                String note = p.get("note").isJsonNull() ? "" : p.get("note").getAsString();
                String imageUrl = p.get("proof_image_url").isJsonNull()
                        ? null : p.get("proof_image_url").getAsString();

                BookingInfo booking = loadBooking(bookingId);
                if (booking == null) continue;

                PaymentRow row = new PaymentRow(
                        booking.fullName,
                        booking.stallId,
                        method,
                        date,
                        booking.deposit,
                        booking.rent,
                        total,
                        note,
                        imageUrl
                );

                Platform.runLater(() -> masterData.add(row));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BookingInfo loadBooking(long bookingId) {
        try {

            String url = SUPABASE_URL +
                    "/rest/v1/bookings" +
                    "?select=full_name,stall_id,deposit_price,total_price" +
                    "&booking_id=eq." + bookingId;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + SUPABASE_KEY)
                    .GET()
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request, HttpResponse.BodyHandlers.ofString()
                    );

            JsonArray arr = JsonParser.parseString(response.body()).getAsJsonArray();
            if (arr.isEmpty()) return null;

            JsonObject b = arr.get(0).getAsJsonObject();

            BookingInfo info = new BookingInfo();
            info.fullName = b.get("full_name").getAsString();
            info.stallId = b.get("stall_id").getAsString();
            info.deposit = b.get("deposit_price").isJsonNull()
                    ? 0 : b.get("deposit_price").getAsDouble();
            info.rent = b.get("total_price").isJsonNull()
                    ? 0 : b.get("total_price").getAsDouble();

            return info;

        } catch (Exception e) {
            return null;
        }
    }

    /* ================= ACTION ================= */

    @FXML
    private void handleViewProof() {
        showDetail(paymentTable.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void handleReset() {
        txtSearch.clear();
        cbMethod.setValue("ทั้งหมด");
        dpDate.setValue(null);
        cbSort.setValue("ชื่อลูกค้า (ก → ฮ)");
        filteredData.setPredicate(p -> true);
        paymentTable.getSelectionModel().clearSelection();
        clearDetail();
    }

    /* ================= DETAIL ================= */

    private void clearDetail() {
        lblMethod.setText("");
        lblDeposit.setText("");
        lblRent.setText("");
        lblTotal.setText("");
        slipImage.setImage(null);
    }

    private void showDetail(PaymentRow row) {
        if (row == null) return;

        lblMethod.setText("วิธีชำระเงิน: " + row.getMethod());
        lblDeposit.setText("- ค่ามัดจำ: " + row.getDeposit() + " ฿");
        lblRent.setText("- ค่าเช่า: " + row.getRent() + " ฿");
        lblTotal.setText("- ยอดรวม: " + row.getTotal() + " ฿");
        txtNote.setText(row.getNote());

        if (row.getImageUrl() != null) {
            slipImage.setImage(new Image(row.getImageUrl(), true));
        } else {
            slipImage.setImage(null);
        }
    }

    /* ================= MODEL ================= */

    private static class BookingInfo {
        String fullName;
        String stallId;
        double deposit;
        double rent;
    }

    public static class PaymentRow {

        private final StringProperty shop = new SimpleStringProperty();
        private final StringProperty lock = new SimpleStringProperty();
        private final StringProperty method = new SimpleStringProperty();
        private final StringProperty date = new SimpleStringProperty();
        private final DoubleProperty deposit = new SimpleDoubleProperty();
        private final DoubleProperty rent = new SimpleDoubleProperty();
        private final DoubleProperty total = new SimpleDoubleProperty();
        private final StringProperty note = new SimpleStringProperty();
        private final StringProperty imageUrl = new SimpleStringProperty();

        public PaymentRow(String s, String l, String m, String d,
                          double dep, double r, double t,
                          String n, String img) {
            shop.set(s);
            lock.set(l);
            method.set(m);
            date.set(d);
            deposit.set(dep);
            rent.set(r);
            total.set(t);
            note.set(n);
            imageUrl.set(img);
        }

        public StringProperty shopProperty() { return shop; }
        public StringProperty lockProperty() { return lock; }
        public StringProperty methodProperty() { return method; }
        public StringProperty dateProperty() { return date; }
        public DoubleProperty depositProperty() { return deposit; }
        public DoubleProperty rentProperty() { return rent; }
        public DoubleProperty totalProperty() { return total; }

        public String getShop() { return shop.get(); }
        public String getLock() { return lock.get(); }
        public String getMethod() { return method.get(); }
        public String getDate() { return date.get(); }
        public String getNote() { return note.get(); }
        public String getImageUrl() { return imageUrl.get(); }
        public double getDeposit() { return deposit.get(); }
        public double getRent() { return rent.get(); }
        public double getTotal() { return total.get(); }
    }
} 