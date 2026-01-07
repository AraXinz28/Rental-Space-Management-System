package com.rental.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rental.database.SupabaseClient;
import com.rental.model.ReceiptData;
import com.rental.report.ReceiptPdfReport;
import com.rental.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.awt.Desktop;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ✅ Payments-only History
 * - แสดงเฉพาะรายการที่มี payments record (ส่งสลิป/ชำระเงินแล้ว)
 *
 * สถานะในตาราง:
 * - pending => รอดำเนินการ
 * - approved/paid/completed => สำเร็จ
 * - rejected => ถูกปฏิเสธ
 * - cancelled => ยกเลิก
 *
 * ปริ้นใบเสร็จ: ได้เฉพาะ "สำเร็จ"
 *
 * ✅ ยกเลิก:
 * - ยกเลิกได้เฉพาะ pending และ สำเร็จ
 * - อัปเดตเฉพาะตาราง payments
 */
public class RentalHistoryController {

    // Labels
    @FXML
    private Label lblName;
    @FXML
    private Label lblPhone;
    @FXML
    private Label lblZone;
    @FXML
    private Label lblLock;
    @FXML
    private Label lblProduct;
    @FXML
    private Label lblDate;
    @FXML
    private Label lblPayment;
    @FXML
    private Label lblCategory;
    @FXML
    private Label lblApproval;

    // Buttons
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnPrint;

    // Table
    @FXML
    private TableColumn<RentalHistory, String> colItem;
    @FXML
    private TableColumn<RentalHistory, String> colStartDate;
    @FXML
    private TableColumn<RentalHistory, String> colEndDate;
    @FXML
    private TableColumn<RentalHistory, String> colStatus;
    @FXML
    private TableColumn<RentalHistory, Void> colDetail;
    @FXML
    private TableView<RentalHistory> historyTable;

    private final SupabaseClient supabase = new SupabaseClient();

    @FXML
    public void initialize() {

        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        historyTable.setTableMenuButtonVisible(false);
        historyTable.setEditable(false);
        historyTable.getColumns().setAll(colItem, colStartDate, colEndDate, colStatus, colDetail);

        if (!Session.isLoggedIn()) {
            System.out.println("ยังไม่มีผู้ใช้ล็อกอิน");
            return;
        }

        long currentUserId = Session.getCurrentUser().getId();

        colItem.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colStartDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colEndDate.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusTh"));

        // Status color
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(status);
                switch (status) {
                    case "สำเร็จ" -> setStyle("-fx-text-fill: #009e0b;");
                    case "ยกเลิก", "ถูกปฏิเสธ" -> setStyle("-fx-text-fill: #c80000;");
                    case "รอดำเนินการ" -> setStyle("-fx-text-fill: #b26b00;");
                    default -> setStyle("");
                }
            }
        });

        // Detail button
        colDetail.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("ดูรายละเอียด");
            {
                btn.setOnAction(e -> {
                    RentalHistory data = getTableView().getItems().get(getIndex());
                    showDetails(data);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        // ✅ โหลดแบบ payments-only
        loadUserInfoFromAnyPayment(currentUserId);
        loadPaymentHistory(currentUserId);

        // Cancel
        btnCancel.setOnAction(e -> handleCancel());

        // Print: เฉพาะ "สำเร็จ"
        btnPrint.setDisable(true);
        historyTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldV, newV) -> btnPrint.setDisable(newV == null || !isFinished(newV)));
        btnPrint.setOnAction(e -> handlePrint());
    }

    // =========================
    // Load user info (จาก payments แถวแรก)
    // =========================
    private void loadUserInfoFromAnyPayment(long userId) {
        try {
            String json = supabase.selectPaymentsJoinBookings(userId);
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            if (arr.isEmpty())
                return;

            JsonObject pay = arr.get(0).getAsJsonObject();
            JsonObject b = pay.has("bookings") && pay.get("bookings").isJsonObject()
                    ? pay.getAsJsonObject("bookings")
                    : null;
            if (b == null)
                return;

            lblName.setText(getStr(b, "full_name", "-"));
            lblPhone.setText(getStr(b, "phone", "-"));

            String stallId = getStr(b, "stall_id", "-");
            String[] zl = splitZoneLock(stallId);
            lblZone.setText(zl[0]);
            lblLock.setText(zl[1]);

            lblProduct.setText(getStr(b, "product_type", "-"));

            String start = getStr(b, "start_date", "-");
            String end = getStr(b, "end_date", "-");
            lblDate.setText(start + " - " + end);

            String payStatus = getStr(pay, "status", "pending");
            lblApproval.setText(mapPaymentStatusToUi(payStatus));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // Load history (payments join bookings)
    // =========================
    private void loadPaymentHistory(long userId) {
        try {
            String json = supabase.selectPaymentsJoinBookings(userId);
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();

            historyTable.getItems().clear();

            for (var el : arr) {
                JsonObject p = el.getAsJsonObject();

                long paymentId = p.has("id") ? p.get("id").getAsLong() : 0;

                String payStatus = getStr(p, "status", "pending");
                String payMethod = getStr(p, "payment_method", "-");
                String payAmount = getStr(p, "amount", "-");
                String payDate = getStr(p, "payment_date", "-");

                JsonObject b = p.has("bookings") && p.get("bookings").isJsonObject()
                        ? p.getAsJsonObject("bookings")
                        : null;

                // ✅ กันแถว "-" : ถ้า booking ไม่ติดมา ไม่แสดง
                if (b == null)
                    continue;

                long bookingId = b.has("booking_id") ? b.get("booking_id").getAsLong() : 0;
                String productType = getStr(b, "product_type", "-");
                String startDate = getStr(b, "start_date", "-");
                String endDate = getStr(b, "end_date", "-");

                String statusTh = mapPaymentStatusToUi(payStatus);

                historyTable.getItems().add(new RentalHistory(
                        paymentId,
                        bookingId,
                        productType,
                        startDate,
                        endDate,
                        payStatus,
                        statusTh,
                        payMethod,
                        payAmount,
                        payDate));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // Detail
    // =========================
    private void showDetails(RentalHistory data) {
        lblProduct.setText(data.getItemName());
        lblDate.setText(data.getStartDate() + " - " + data.getEndDate());
        lblPayment.setText("ช่องทาง: " + data.getPaymentMethod() + " | ยอด: " + data.getAmount());
        lblApproval.setText(data.getStatusTh());
    }

    // =========================
    // Cancel: ยกเลิกได้เฉพาะ "รอดำเนินการ" และ "สำเร็จ"
    // อัปเดตเฉพาะตาราง payments
    // =========================
    @FXML
    private void handleCancel() {
        RentalHistory selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        String en = (selected.getStatusEn() == null) ? "" : selected.getStatusEn().trim().toLowerCase();

        boolean canCancel = en.equals("pending") || isFinished(selected);
        if (!canCancel) {
            showAlert(Alert.AlertType.WARNING, "ไม่สามารถยกเลิกได้",
                    "ยกเลิกได้เฉพาะรายการที่มีสถานะ \"รอดำเนินการ\" หรือ \"สำเร็จ\" เท่านั้น\n" +
                            "สถานะปัจจุบัน: " + selected.getStatusTh());
            return;
        }

        try {
            String paymentId = String.valueOf(selected.getPaymentId());

            // ✅ update payments เท่านั้น
            supabase.update("payments", "id", paymentId, "{\"status\":\"cancelled\"}");

            // ✅ อัปเดตในตาราง UI ทันที
            selected.setStatusEn("cancelled");
            selected.setStatusTh("ยกเลิก");
            historyTable.refresh();

            showDetails(selected);
            btnPrint.setDisable(true);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "ผิดพลาด", "ยกเลิกไม่สำเร็จ: " + e.getMessage());
        }
    }

    // =========================
    // Print receipt (ใช้ bookingId)
    // =========================
    @FXML
    private void handlePrint() {
        RentalHistory selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "แจ้งเตือน", "กรุณาเลือกรายการก่อนพิมพ์ใบเสร็จ");
            return;
        }

        if (!isFinished(selected)) {
            showAlert(Alert.AlertType.WARNING, "ไม่สามารถพิมพ์ได้",
                    "สามารถพิมพ์ใบเสร็จได้เฉพาะรายการที่มีสถานะ \"สำเร็จ\" เท่านั้น\n" +
                            "สถานะปัจจุบัน: " + selected.getStatusTh());
            return;
        }

        try {
            ReceiptData receipt = buildReceiptDataFromDb(selected.getBookingId());

            var chooser = new javafx.stage.FileChooser();
            chooser.setTitle("บันทึกใบเสร็จเป็น PDF");
            chooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
            chooser.setInitialFileName("ใบเสร็จ_RC-" + receipt.bookingId + ".pdf");

            File out = chooser.showSaveDialog(historyTable.getScene().getWindow());
            if (out == null)
                return;

            if (!out.getName().toLowerCase().endsWith(".pdf")) {
                out = new File(out.getAbsolutePath() + ".pdf");
            }

            ReceiptPdfReport.export(out, receipt);

            showAlert(Alert.AlertType.INFORMATION, "สำเร็จ",
                    "สร้างใบเสร็จ PDF เรียบร้อย\nไฟล์: " + out.getAbsolutePath());

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(out);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "ผิดพลาด", "พิมพ์ใบเสร็จไม่สำเร็จ: " + ex.getMessage());
        }
    }

    // =========================
    // Build receipt (bookings + payments ล่าสุด)
    // =========================
    private ReceiptData buildReceiptDataFromDb(long bookingId) throws Exception {
        String json = supabase.selectBookingDetailById(bookingId);
        var parsed = JsonParser.parseString(json);

        if (!parsed.isJsonArray())
            throw new RuntimeException("Supabase error response: " + json);

        JsonArray arr = parsed.getAsJsonArray();
        if (arr.isEmpty())
            throw new RuntimeException("ไม่พบข้อมูล booking_id=" + bookingId);

        JsonObject o = arr.get(0).getAsJsonObject();

        ReceiptData rd = new ReceiptData();
        rd.bookingId = bookingId;

        rd.fullName = getStr(o, "full_name", "-");
        rd.phone = getStr(o, "phone", "-");
        rd.productType = getStr(o, "product_type", "-");

        rd.startDate = parseDate(getStr(o, "start_date", null));
        rd.endDate = parseDate(getStr(o, "end_date", null));

        rd.deposit = getDecimal(o, "deposit_price");
        rd.total = getDecimal(o, "total_price");

        rd.stallId = getStr(o, "stall_id", "-");

        String paymentMethod = "-";
        String payStatus = null;

        if (o.has("payments") && o.get("payments").isJsonArray()) {
            JsonArray pays = o.getAsJsonArray("payments");

            JsonObject latest = null;
            for (var pe : pays) {
                JsonObject po = pe.getAsJsonObject();
                if (latest == null)
                    latest = po;
                else {
                    String a = getStr(po, "created_at", "");
                    String b = getStr(latest, "created_at", "");
                    if (a.compareTo(b) > 0)
                        latest = po;
                }
            }

            if (latest != null) {
                String pm = getStr(latest, "payment_method", "-");
                if (pm != null && !pm.isBlank() && !pm.equals("-"))
                    paymentMethod = pm;
                payStatus = getStr(latest, "status", null);
            }
        }

        rd.paymentMethod = paymentMethod;

        String finalStatusEn = (payStatus != null ? payStatus : getStr(o, "status", "pending"));
        rd.status = mapPaymentStatusToUi(finalStatusEn);

        String[] z = splitZoneLock(rd.stallId);
        rd.zone = z[0];
        rd.lockNo = z[1];

        return rd;
    }

    // =========================
    // Helpers
    // =========================
    private boolean isFinished(RentalHistory r) {
        String en = r.getStatusEn();
        if (en == null)
            return false;
        String s = en.trim().toLowerCase();
        return s.equals("approved") || s.equals("paid") || s.equals("completed");
    }

    private static String mapPaymentStatusToUi(String payStatus) {
        if (payStatus == null)
            return "รอดำเนินการ";
        return switch (payStatus.toLowerCase()) {
            case "approved", "paid", "completed" -> "สำเร็จ";
            case "rejected" -> "ถูกปฏิเสธ";
            case "cancelled", "canceled" -> "ยกเลิก";
            case "pending" -> "รอดำเนินการ";
            default -> "รอดำเนินการ";
        };
    }

    private static String getStr(JsonObject o, String key, String def) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull())
            return def;
        try {
            return o.get(key).getAsString();
        } catch (Exception e) {
            return def;
        }
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank())
            return null;
        return LocalDate.parse(s);
    }

    private static BigDecimal getDecimal(JsonObject o, String key) {
        if (o == null || !o.has(key) || o.get(key).isJsonNull())
            return BigDecimal.ZERO;
        try {
            return o.get(key).getAsBigDecimal();
        } catch (Exception e) {
            try {
                return new BigDecimal(o.get(key).getAsString());
            } catch (Exception ex) {
                return BigDecimal.ZERO;
            }
        }
    }

    private static String[] splitZoneLock(String stallId) {
        if (stallId == null || stallId.isBlank() || "-".equals(stallId))
            return new String[] { "-", "-" };
        String s = stallId.replace("-", "").replace(" ", "").trim();
        if (s.length() >= 2 && Character.isLetter(s.charAt(0))) {
            return new String[] { String.valueOf(s.charAt(0)), s.substring(1) };
        }
        return new String[] { stallId, stallId };
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    // =========================
    // Model for table
    // =========================
    public static class RentalHistory {
        private final long paymentId;
        private final long bookingId;

        private final String itemName;
        private final String startDate;
        private final String endDate;

        private String statusEn;
        private String statusTh;

        private final String paymentMethod;
        private final String amount;
        private final String paymentDate;

        public RentalHistory(long paymentId, long bookingId,
                String itemName, String startDate, String endDate,
                String statusEn, String statusTh,
                String paymentMethod, String amount, String paymentDate) {
            this.paymentId = paymentId;
            this.bookingId = bookingId;
            this.itemName = itemName;
            this.startDate = startDate;
            this.endDate = endDate;
            this.statusEn = statusEn;
            this.statusTh = statusTh;
            this.paymentMethod = paymentMethod;
            this.amount = amount;
            this.paymentDate = paymentDate;
        }

        public long getPaymentId() {
            return paymentId;
        }

        public long getBookingId() {
            return bookingId;
        }

        public long getId() {
            return bookingId;
        }

        public String getItemName() {
            return itemName;
        }

        public String getStartDate() {
            return startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public String getStatusEn() {
            return statusEn;
        }

        public void setStatusEn(String statusEn) {
            this.statusEn = statusEn;
        }

        public String getStatusTh() {
            return statusTh;
        }

        public void setStatusTh(String statusTh) {
            this.statusTh = statusTh;
        }

        public String getPaymentMethod() {
            return paymentMethod;
        }

        public String getAmount() {
            return amount;
        }

        public String getPaymentDate() {
            return paymentDate;
        }
    }
}
