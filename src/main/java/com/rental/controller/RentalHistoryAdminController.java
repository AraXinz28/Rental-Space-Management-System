package com.rental.controller;

import com.google.gson.*;
import com.rental.database.SupabaseClient;
import com.rental.model.RentalHistoryRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class RentalHistoryAdminController {

    @FXML
    private TextField txtCustomer;
    @FXML
    private ComboBox<String> cbStatus;
    @FXML
    private DatePicker dpDate;

    // ✅ sort controls ข้างวันที่
    @FXML
    private ComboBox<String> cbSortField;
    @FXML
    private ComboBox<String> cbSortOrder;

    @FXML
    private Button btnPrint;

    @FXML
    private TableView<RentalHistoryRow> table;
    @FXML
    private TableColumn<RentalHistoryRow, String> colCustomer;
    @FXML
    private TableColumn<RentalHistoryRow, LocalDate> colDate;
    @FXML
    private TableColumn<RentalHistoryRow, String> colStatus;
    @FXML
    private TableColumn<RentalHistoryRow, Void> colDetail;

    private final ObservableList<RentalHistoryRow> masterData = FXCollections.observableArrayList();
    private FilteredList<RentalHistoryRow> filtered;
    private SortedList<RentalHistoryRow> sorted;

    private final SupabaseClient supabase = new SupabaseClient();

    private static final DateTimeFormatter TH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {

        // ===== status combo =====
        cbStatus.setItems(FXCollections.observableArrayList("ทั้งหมด", "เสร็จสิ้น", "ยกเลิก", "รอดำเนินการ"));
        cbStatus.getSelectionModel().selectFirst();

        // ===== columns =====
        colCustomer.setCellValueFactory(d -> d.getValue().customerNameProperty());

        colDate.setCellValueFactory(d -> d.getValue().dateProperty());
        colDate.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : TH_DATE.format(item));
                setStyle("-fx-alignment: CENTER;");
            }
        });

        colStatus.setCellValueFactory(d -> d.getValue().statusProperty());
        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label badge = new Label(item);
                badge.setStyle("""
                        -fx-text-fill: white;
                        -fx-padding: 6 16;
                        -fx-background-radius: 999;
                        -fx-font-weight: bold;
                        """);

                if ("เสร็จสิ้น".equals(item)) {
                    badge.setStyle(badge.getStyle() + "-fx-background-color: #4F6F4A;");
                } else if ("ยกเลิก".equals(item)) {
                    badge.setStyle(badge.getStyle() + "-fx-background-color: #6B3F3F;");
                } else if ("ปฏิเสธการชำระเงิน".equals(item)) {
                    badge.setStyle(badge.getStyle() + "-fx-background-color: #6B3F3F;");
                } else {
                    badge.setStyle(badge.getStyle() + "-fx-background-color: #777;");
                }

                setGraphic(badge);
                setText(null);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // detail button
        colDetail.setCellFactory(tc -> new TableCell<>() {
            private final Button btn = new Button("ดูรายละเอียด");
            {
                btn.setStyle("""
                        -fx-background-color: #40446A;
                        -fx-text-fill: white;
                        -fx-background-radius: 999;
                        -fx-padding: 8 16;
                        """);
                btn.setOnAction(e -> openDetail(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setText(null);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // ===== bind lists =====
        filtered = new FilteredList<>(masterData, p -> true);
        sorted = new SortedList<>(filtered);
        table.setItems(sorted);

        // ===== ปรับความกว้างคอลัมน์ให้พอดีทุกหน้าจอ =====
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        final double P = 18; // กัน scrollbar

        colCustomer.prefWidthProperty().bind(table.widthProperty().multiply(0.38).subtract(P));
        colDate.prefWidthProperty().bind(table.widthProperty().multiply(0.22));
        colStatus.prefWidthProperty().bind(table.widthProperty().multiply(0.20));
        colDetail.prefWidthProperty().bind(table.widthProperty().multiply(0.20));

        colCustomer.setMinWidth(220);
        colDate.setMinWidth(160);
        colStatus.setMinWidth(150);
        colDetail.setMinWidth(150);

        table.setFixedCellSize(46);

        // ===== sort controls (ข้างวันที่) =====
        cbSortField.setItems(FXCollections.observableArrayList("ชื่อลูกค้า", "วัน/เดือน/ปี ทำรายการ"));
        cbSortField.getSelectionModel().selectFirst();

        cbSortOrder.setItems(FXCollections.observableArrayList("ก-ฮ", "ฮ-ก"));
        cbSortOrder.getSelectionModel().selectFirst();

        cbSortField.valueProperty().addListener((obs, o, n) -> {
            if ("วัน/เดือน/ปี ทำรายการ".equals(n)) {
                cbSortOrder.setItems(FXCollections.observableArrayList("ก่อน-หลัง", "หลัง-ก่อน"));
                cbSortOrder.getSelectionModel().selectFirst();
            } else {
                cbSortOrder.setItems(FXCollections.observableArrayList("ก-ฮ", "ฮ-ก"));
                cbSortOrder.getSelectionModel().selectFirst();
            }
            applySort();
        });

        cbSortOrder.valueProperty().addListener((obs, o, n) -> applySort());

        // ===== filter listeners: reload from DB =====
        txtCustomer.textProperty().addListener((obs, o, n) -> reloadFromDb());
        cbStatus.valueProperty().addListener((obs, o, n) -> reloadFromDb());
        dpDate.valueProperty().addListener((obs, o, n) -> reloadFromDb());

        // โหลดครั้งแรก
        reloadFromDb();
    }

    // ✅ เรียงอัตโนมัติจาก ComboBox
    private void applySort() {
        java.text.Collator th = java.text.Collator.getInstance(new java.util.Locale("th", "TH"));
        th.setStrength(java.text.Collator.PRIMARY);

        String field = cbSortField.getValue();
        String order = cbSortOrder.getValue();

        Comparator<RentalHistoryRow> cmp;

        if ("วัน/เดือน/ปี ทำรายการ".equals(field)) {
            cmp = Comparator.comparing(RentalHistoryRow::getDate,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            if ("หลัง-ก่อน".equals(order))
                cmp = cmp.reversed();
        } else {
            cmp = (a, b) -> th.compare(
                    a.getCustomerName() == null ? "" : a.getCustomerName(),
                    b.getCustomerName() == null ? "" : b.getCustomerName());
            if ("ฮ-ก".equals(order))
                cmp = cmp.reversed();

            cmp = cmp.thenComparing(RentalHistoryRow::getDate,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        }

        sorted.setComparator(cmp);
    }

    private void reloadFromDb() {
        String name = txtCustomer.getText();
        String statusUiFilter = cbStatus.getValue();
        LocalDate date = dpDate.getValue();

        Task<List<RentalHistoryRow>> task = new Task<>() {
            @Override
            protected List<RentalHistoryRow> call() throws Exception {

                // ✅ ดึงจาก payments เป็นหลัก + สถานะจาก payments.status
                // ✅ กรองวันที่ด้วย payments.created_at (timestamp) ทำใน SupabaseClient
                String json = supabase.selectPaymentsForAdminHistory(name, statusUiFilter, date);

                var parsed = JsonParser.parseString(json);
                if (!parsed.isJsonArray()) {
                    throw new RuntimeException("Supabase error: " + json);
                }

                List<RentalHistoryRow> list = new ArrayList<>();
                JsonArray arr = parsed.getAsJsonArray();

                for (JsonElement el : arr) {
                    JsonObject o = el.getAsJsonObject();

                    long bookingId = o.get("booking_id").getAsLong();

                    // ✅ full_name จาก bookings ที่ join มา
                    String fullName = "";
                    if (o.has("bookings") && o.get("bookings").isJsonObject()) {
                        JsonObject b = o.getAsJsonObject("bookings");
                        if (b.has("full_name") && !b.get("full_name").isJsonNull()) {
                            fullName = b.get("full_name").getAsString();
                        }
                    }

                    // ✅ วันทำรายการ = payments.created_at
                    String createdAtRaw = o.get("created_at").getAsString();
                    LocalDate createdAt = LocalDate.parse(createdAtRaw.substring(0, 10));

                    // ✅ status = payments.status เท่านั้น
                    String payStatus = (o.has("status") && !o.get("status").isJsonNull())
                            ? o.get("status").getAsString()
                            : "pending";

                    String uiStatus = mapPaymentStatusToUi(payStatus);
                    if (uiStatus == null)
                        uiStatus = payStatus;

                    list.add(new RentalHistoryRow(
                            bookingId,
                            fullName,
                            createdAt,
                            uiStatus));
                }

                return list;
            }
        };

        task.setOnSucceeded(e -> {
            masterData.setAll(task.getValue());
            applySort();
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("ผิดพลาด");
            err.setHeaderText("ดึงข้อมูลประวัติไม่สำเร็จ");
            err.setContentText(String.valueOf(ex.getMessage()));
            err.showAndWait();
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ✅ payments.status -> UI
    private static String mapPaymentStatusToUi(String payStatus) {
        if (payStatus == null)
            return null;
        return switch (payStatus) {
            case "approved" -> "เสร็จสิ้น";
            case "pending" -> "รอดำเนินการ";
            case "rejected" -> "ปฏิเสธการชำระเงิน";
            case "cancelled" -> "ยกเลิก";
            default -> null;
        };
    }

    // ✅ เปิดหน้ารายละเอียด
    private void openDetail(RentalHistoryRow row) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/rentalhistorydetail.fxml"));
            Parent root = loader.load();

            RentalHistoryDetailController c = loader.getController();
            c.setData(row);

            Stage stage = (Stage) table.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("เปิดหน้ารายละเอียดไม่สำเร็จ");
            a.setHeaderText("โหลด rentalhistorydetail.fxml ไม่ได้");
            a.setContentText(String.valueOf(ex));
            a.showAndWait();
        }
    }

    // ✅ พิมพ์รายงาน PDF (เต็ม)
    @FXML
    private void onPrint(ActionEvent event) {
        try {
            var chooser = new javafx.stage.FileChooser();
            chooser.setTitle("บันทึกรายงานเป็น PDF");
            chooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
            chooser.setInitialFileName("รายงานประวัติการเช่า.pdf");

            File out = chooser.showSaveDialog(table.getScene().getWindow());
            if (out == null)
                return;

            if (!out.getName().toLowerCase().endsWith(".pdf")) {
                out = new File(out.getAbsolutePath() + ".pdf");
            }

            List<RentalHistoryRow> rows = new ArrayList<>(table.getItems());

            com.rental.report.RentalHistoryPdfReport.export(
                    out,
                    rows,
                    txtCustomer.getText(),
                    cbStatus.getValue(),
                    dpDate.getValue());

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("สำเร็จ");
            ok.setHeaderText("สร้างไฟล์ PDF เรียบร้อย");
            ok.setContentText(out.getAbsolutePath());
            ok.showAndWait();

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(out);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("ผิดพลาด");
            err.setHeaderText("สร้าง PDF ไม่สำเร็จ");
            err.setContentText(String.valueOf(e));
            err.showAndWait();
        }
    }
}