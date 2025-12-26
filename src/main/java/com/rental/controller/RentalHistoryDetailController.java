package com.rental.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rental.database.SupabaseClient;
import com.rental.model.RentalHistoryRow;
import com.rental.model.ReceiptData;
import com.rental.report.ReceiptPdfReport;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RentalHistoryDetailController {

    @FXML
    private Label lblStatusPill;
    @FXML
    private Label lblName;
    @FXML
    private Label lblZone;
    @FXML
    private Label lblLockNo;
    @FXML
    private Label lblProductType;
    @FXML
    private Label lblPhone;
    @FXML
    private Label lblDate;
    @FXML
    private Label lblPayment;
    @FXML
    private Label lblDeposit;
    @FXML
    private Label lblTotalRent;
    @FXML
    private Label lblPaidTotal;

    private static final DateTimeFormatter TH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");

    private final SupabaseClient supabase = new SupabaseClient();

    // ✅ เก็บข้อมูลสำหรับพิมพ์ใบเสร็จ
    private ReceiptData receipt = null;

    public void setData(RentalHistoryRow row) {
        lblName.setText(row.getCustomerName());

        // ⚠️ ถ้าหน้ารวมยังส่ง start_date มาอยู่ ตรงนี้ยังจะเป็น start_date
        // แต่เราจะ override ด้วย created_at จาก DB ตอน loadDetailFromDb()
        lblDate.setText(row.getDate() == null ? "-" : TH_DATE.format(row.getDate()));

        setStatus(row.getStatus());

        long bookingId = row.getBookingId();
        System.out.println("DETAIL bookingId = " + bookingId);

        setLoadingUI();
        loadDetailFromDb(bookingId);
    }

    private void setLoadingUI() {
        lblPhone.setText("กำลังโหลด...");
        lblProductType.setText("กำลังโหลด...");
        lblZone.setText("กำลังโหลด...");
        lblLockNo.setText("กำลังโหลด...");
        lblPayment.setText("กำลังโหลด..."); // ✅
        lblDeposit.setText(MONEY.format(0) + " ฿");
        lblTotalRent.setText(MONEY.format(0) + " ฿");
        lblPaidTotal.setText(MONEY.format(0) + " ฿");
    }

    private void loadDetailFromDb(long bookingId) {

        if (bookingId <= 0) {
            System.out.println("DETAIL bookingId invalid => " + bookingId);
            fillEmptyDetail();
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {

                String json = supabase.selectBookingDetailById(bookingId);
                System.out.println("DETAIL JSON = " + json);

                var parsed = JsonParser.parseString(json);

                // ✅ กันพัง: ถ้า Supabase ส่ง error object มา
                if (!parsed.isJsonArray()) {
                    throw new RuntimeException("Supabase error response: " + json);
                }

                JsonArray arr = parsed.getAsJsonArray();
                if (arr.isEmpty()) {
                    throw new RuntimeException("ไม่พบข้อมูล booking_id=" + bookingId);
                }

                JsonObject o = arr.get(0).getAsJsonObject();

                String fullName = getStr(o, "full_name", "-");
                String phone = getStr(o, "phone", "-");
                String productType = getStr(o, "product_type", "-");
                String statusDb = getStr(o, "status", "pending");

                // ✅ วันทำรายการ = created_at (timestamp -> yyyy-MM-dd)
                LocalDate createdAt = parseTimestampToLocalDate(getStr(o, "created_at", null));

                // ช่วงเช่า (ยังใช้ start/end เดิม)
                LocalDate startDate = parseDate(getStr(o, "start_date", null));
                LocalDate endDate = parseDate(getStr(o, "end_date", null));

                BigDecimal deposit = getDecimal(o, "deposit_price");
                BigDecimal total = getDecimal(o, "total_price");

                String stallId = getStr(o, "stall_id", "-");

                // ✅ ดึงวิธีชำระเงินจาก payments (เอา record ล่าสุด)
                String paymentMethod = "-";
                String paymentStatus = null;

                if (o.has("payments") && o.get("payments").isJsonArray()) {
                    JsonArray pays = o.getAsJsonArray("payments");

                    JsonObject latest = null;
                    for (var pe : pays) {
                        JsonObject po = pe.getAsJsonObject();
                        if (latest == null) {
                            latest = po;
                        } else {
                            String a = getStr(po, "created_at", "");
                            String b = getStr(latest, "created_at", "");
                            if (a.compareTo(b) > 0)
                                latest = po;
                        }
                    }

                    if (latest != null) {
                        paymentMethod = getStr(latest, "payment_method", "-");
                        paymentStatus = getStr(latest, "status", null);
                    }
                }

                // ✅ สถานะ UI อิง paymentStatus เป็นหลัก (ถ้ามี)
                String statusUi;
                if ("approved".equals(paymentStatus))
                    statusUi = "เสร็จสิ้น";
                else if ("pending".equals(paymentStatus))
                    statusUi = "รอดำเนินการ";
                else if ("rejected".equals(paymentStatus))
                    statusUi = "ยกเลิก";
                else
                    statusUi = mapStatusToUi(statusDb);

                // เดา zone/lock จาก stall_id เช่น A14 หรือ A-14
                String zone = "-";
                String lockNo = "-";

                if (stallId != null && !stallId.equals("-")) {
                    String s = stallId.replace("-", "").trim();
                    if (s.length() >= 2 && Character.isLetter(s.charAt(0))) {
                        zone = String.valueOf(s.charAt(0));
                        lockNo = s.substring(1);
                    } else {
                        lockNo = s;
                    }
                }

                // ✅ สร้าง ReceiptData ไว้พิมพ์ PDF
                ReceiptData rd = new ReceiptData();
                rd.bookingId = bookingId;
                rd.fullName = fullName;
                rd.phone = phone;
                rd.productType = productType;
                rd.status = statusUi;
                rd.startDate = startDate;
                rd.endDate = endDate;
                rd.deposit = deposit;
                rd.total = total;
                rd.stallId = stallId;
                rd.zone = zone;
                rd.lockNo = lockNo;

                // ✅ ใช้วิธีชำระเงินจาก payments
                rd.paymentMethod = paymentMethod;

                // ✅ แก้ error: ทำให้เป็น final ก่อนเข้า lambda
                final String finalZone = zone;
                final String finalLockNo = lockNo;
                final String finalFullName = fullName;
                final String finalPhone = phone;
                final String finalProductType = productType;
                final String finalPaymentMethod = paymentMethod;
                final LocalDate finalCreatedAt = createdAt;
                final BigDecimal finalDeposit = deposit;
                final BigDecimal finalTotal = total;
                final String finalStatusUi = statusUi;

                Platform.runLater(() -> {
                    receipt = rd;

                    lblName.setText(finalFullName);
                    lblPhone.setText(finalPhone);
                    lblProductType.setText(finalProductType);

                    lblZone.setText(finalZone);
                    lblLockNo.setText(finalLockNo);

                    // ✅ แสดงวันทำรายการ = created_at
                    lblDate.setText(finalCreatedAt == null ? "-" : TH_DATE.format(finalCreatedAt));

                    // ✅ แสดงวิธีชำระเงินจาก payments
                    lblPayment.setText(finalPaymentMethod);

                    lblDeposit.setText(MONEY.format(finalDeposit) + " ฿");
                    lblTotalRent.setText(MONEY.format(finalTotal) + " ฿");
                    lblPaidTotal.setText(MONEY.format(finalTotal.add(finalDeposit)) + " ฿");

                    setStatus(finalStatusUi);
                });

                return null;
            }
        };

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            ex.printStackTrace();

            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("ผิดพลาด");
            a.setHeaderText("โหลดรายละเอียดจากฐานข้อมูลไม่สำเร็จ");
            a.setContentText(String.valueOf(ex.getMessage()));
            a.showAndWait();

            fillEmptyDetail();
        });

        new Thread(task).start();
    }

    private void fillEmptyDetail() {
        receipt = null;
        lblPhone.setText("-");
        lblZone.setText("-");
        lblLockNo.setText("-");
        lblProductType.setText("-");
        lblPayment.setText("-");
        lblDeposit.setText(MONEY.format(0) + " ฿");
        lblTotalRent.setText(MONEY.format(0) + " ฿");
        lblPaidTotal.setText(MONEY.format(0) + " ฿");
    }

    private void setStatus(String status) {
        String text = (status == null) ? "-" : status;
        lblStatusPill.setText(text);

        String base = "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 18; -fx-background-radius: 999;";
        if ("เสร็จสิ้น".equals(text)) {
            lblStatusPill.setStyle(base + "-fx-background-color: #4F6F4A;");
        } else if ("ยกเลิก".equals(text)) {
            lblStatusPill.setStyle(base + "-fx-background-color: #6B3F3F;");
        } else {
            lblStatusPill.setStyle(base + "-fx-background-color: #777;");
        }
    }

    private static String mapStatusToUi(String dbStatus) {
        return switch (dbStatus) {
            case "paid", "completed" -> "เสร็จสิ้น";
            case "cancelled" -> "ยกเลิก";
            case "pending" -> "รอดำเนินการ";
            default -> dbStatus;
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

    // ✅ created_at timestamp -> LocalDate
    private static LocalDate parseTimestampToLocalDate(String s) {
        if (s == null || s.isBlank())
            return null;
        if (s.length() >= 10)
            return LocalDate.parse(s.substring(0, 10));
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

    @FXML
    private void onBack(ActionEvent e) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/views/rentalhistorymanage.fxml"));
            Stage stage = (Stage) lblName.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ✅ พิมพ์ใบเสร็จแบบ PDF โมเดิร์น
    @FXML
    private void onPrintSlip(ActionEvent event) {
        try {
            if (receipt == null) {
                Alert a = new Alert(Alert.AlertType.WARNING);
                a.setTitle("ยังพิมพ์ไม่ได้");
                a.setHeaderText("ข้อมูลยังโหลดไม่เสร็จ");
                a.setContentText("กรุณารอสักครู่ แล้วลองกดพิมพ์อีกครั้ง");
                a.showAndWait();
                return;
            }

            var chooser = new javafx.stage.FileChooser();
            chooser.setTitle("บันทึกใบเสรจเป็น PDF");
            chooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
            chooser.setInitialFileName("ใบเสร็จ_RC-" + receipt.bookingId + ".pdf");

            File out = chooser.showSaveDialog(lblName.getScene().getWindow());
            if (out == null)
                return;

            if (!out.getName().toLowerCase().endsWith(".pdf")) {
                out = new File(out.getAbsolutePath() + ".pdf");
            }

            ReceiptPdfReport.export(out, receipt);

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("สำเร็จ");
            ok.setHeaderText("สร้างใบเสร็จ PDF เรียบร้อย");
            ok.setContentText(out.getAbsolutePath());
            ok.showAndWait();

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(out);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("ผิดพลาด");
            err.setHeaderText("พิมพ์ใบเสร็จไม่สำเร็จ");
            err.setContentText(String.valueOf(ex.getMessage()));
            err.showAndWait();
        }
    }
}
