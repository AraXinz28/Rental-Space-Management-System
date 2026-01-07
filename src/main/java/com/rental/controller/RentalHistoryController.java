package com.rental.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import com.rental.util.Session;
import com.rental.database.SupabaseClient;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import com.google.gson.*;

public class RentalHistoryController {
    // Labels
    @FXML private Label lblName;
    @FXML private Label lblPhone;
    @FXML private Label lblZone;
    @FXML private Label lblLock;
    @FXML private Label lblProduct;
    @FXML private Label lblDate;
    @FXML private Label lblPayment;
    @FXML private Label lblCategory;
    @FXML private Label lblApproval;

    // Buttons
    @FXML private Button btnCancel;
    @FXML private Button btnPrint;

    // Table
    @FXML private TableColumn<RentalHistory, String> colItem;
    @FXML private TableColumn<RentalHistory, String> colStartDate;
    @FXML private TableColumn<RentalHistory, String> colEndDate;
    @FXML private TableColumn<RentalHistory, String> colStatus;
    @FXML private TableColumn<RentalHistory, Void> colDetail;
    @FXML private TableView<RentalHistory> historyTable;

    // ใช้ SupabaseClient แทน JDBC
    private final SupabaseClient client = new SupabaseClient();

    @FXML 
    public void initialize() {

historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
historyTable.setTableMenuButtonVisible(false);

// ปิดการสร้างคอลัมน์อัตโนมัติ
historyTable.setEditable(false);
historyTable.getColumns().setAll(colItem, colStartDate, colEndDate, colStatus, colDetail);



        if (!Session.isLoggedIn()) { 
            System.out.println("ยังไม่มีผู้ใช้ล็อกอิน");
            return; 
        }

        long currentUserId = Session.getCurrentUser().getId(); 
        System.out.println("Session userId = " + currentUserId);

        loadUserInfo(currentUserId); 
        loadBookingHistory(currentUserId);

        // Hover effects
        btnCancel.setOnMouseEntered(e -> btnCancel.setStyle("-fx-background-color: #9e0404ff; -fx-text-fill: white;"));
        btnCancel.setOnMouseExited(e -> btnCancel.setStyle("-fx-background-color: #d80202ff; -fx-text-fill: white;"));

        btnPrint.setOnMouseEntered(e -> btnPrint.setStyle("-fx-background-color: #013f8aff; -fx-text-fill: white;"));
        btnPrint.setOnMouseExited(e -> btnPrint.setStyle("-fx-background-color: #066edcff; -fx-text-fill: white;"));

        // Map columns
        colItem.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colStartDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        colEndDate.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("paymentStatus"));

        // Status color
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                switch (status) {
                    case "เสร็จสิ้น" -> setStyle("-fx-text-fill: #009e0b;");
                    case "ยกเลิก" -> setStyle("-fx-text-fill: #c80000;");
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

        // Button actions
        btnCancel.setOnAction(e -> handleCancel());
        btnPrint.setOnAction(e -> handlePrint());
    }

    // ใช้ REST API ดึงข้อมูลผู้ใช้
    private void loadUserInfo(long userId) {
        try {
            String json = client.selectWhere("bookings", "user_id", String.valueOf(userId));
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            if (!arr.isEmpty()) {
                JsonObject obj = arr.get(0).getAsJsonObject();

                lblName.setText(getSafe(obj, "full_name"));
                lblPhone.setText(getSafe(obj, "phone"));
                lblZone.setText(getSafe(obj, "stall_id"));
                lblLock.setText(getSafe(obj, "stall_id"));
                lblProduct.setText(getSafe(obj, "product_type"));
                lblDate.setText(getSafe(obj, "start_date") + " - " + getSafe(obj, "end_date"));

                JsonArray paymentsArr = obj.getAsJsonArray("payments");
                JsonObject payments = (paymentsArr != null && paymentsArr.size() > 0)
                        ? paymentsArr.get(0).getAsJsonObject()
                        : null;

                String paymentStatusEn = payments != null && payments.has("status") && !payments.get("status").isJsonNull()
                        ? payments.get("status").getAsString() : "-";

                lblPayment.setText(payments != null ? ("payments: " + paymentStatusEn) : "ยังไม่เชื่อม payments");
                lblApproval.setText(toThaiStatus(paymentStatusEn));
            }
        } catch (Exception e) {
            e.printStackTrace();
            lblPayment.setText("ยังไม่เชื่อม payments");
            lblApproval.setText("-");
        }
    }

    // helper ปลอดภัยเวลาหยิบค่า
    private String getSafe(JsonObject obj, String key) {
        return (obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : "-";
    }

// ใช้ REST API ดึง booking history
private void loadBookingHistory(long userId) {
    try {
        String json = client.selectJoinBookingsPayments(userId);
        System.out.println("DEBUG JSON = " + json);
        System.out.println("DEBUG USER_ID = " + userId);
        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
        historyTable.getItems().clear();

        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();

            // ดึง status จาก booking ก่อน
            String bookingStatus = (obj.has("status") && !obj.get("status").isJsonNull())
                    ? obj.get("status").getAsString()
                    : null;

            JsonArray paymentsArr = obj.getAsJsonArray("payments");
            JsonObject payments = (paymentsArr != null && paymentsArr.size() > 0)
                    ? paymentsArr.get(0).getAsJsonObject()
                    : null;

            // ถ้า booking มี status ใช้ค่านั้น, ถ้าไม่มีก็ fallback ไปใช้ payments
            String paymentStatusEn = (bookingStatus != null)
                    ? bookingStatus
                    : (payments != null && payments.has("status") && !payments.get("status").isJsonNull())
                        ? payments.get("status").getAsString()
                        : "-";

            String payMethod = (payments != null && payments.has("payment_method")) ? payments.get("payment_method").getAsString() : "-";
            String payAmount = (payments != null && payments.has("amount")) ? payments.get("amount").getAsString() : "-";
            String payDate = (payments != null && payments.has("payment_date")) ? payments.get("payment_date").getAsString() : "-";

            historyTable.getItems().add(
                new RentalHistory(
                    obj.get("booking_id").getAsLong(),
                    getSafe(obj, "product_type"),
                    getSafe(obj, "start_date"),
                    getSafe(obj, "end_date"),
                    toThaiStatus(paymentStatusEn),
                    payMethod,
                    payAmount,
                    payDate
                )
            );
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}


    private void showDetails(RentalHistory data) {
        lblProduct.setText(data.getItemName());
        lblDate.setText(data.getStartDate() + " - " + data.getEndDate());
        lblPayment.setText("ช่องทาง: " + data.getPaymentMethod() + " | ยอด: " + data.getAmount());
        lblApproval.setText(data.getPaymentStatus());
        
    } 
@FXML
private void handleCancel() {
    RentalHistory selected = historyTable.getSelectionModel().getSelectedItem();
    if (selected == null) return;

    try {
        String bookingId = String.valueOf(selected.getId());

        // อัปเดต bookings โดยใช้ booking_id
        var res1 = client.update("bookings", "booking_id", bookingId,
              "{\"status\":\"rejected\"}");
        System.out.println("Bookings update result: " + res1);

        // อัปเดต payments โดยใช้ booking_id เช่นกัน
        var res2 = client.update("payments", "booking_id", bookingId,
              "{\"status\":\"rejected\"}");
        System.out.println("Payments update result: " + res2);

        // อัปเดตใน local object และตาราง
        selected.setPaymentStatus("ยกเลิก");
        historyTable.getItems().set(historyTable.getSelectionModel().getSelectedIndex(), selected);
        historyTable.refresh();

        showDetails(selected);
    } catch (Exception e) {
        e.printStackTrace();
    }
}

@FXML
private void handlePrint() {
    RentalHistory selected = historyTable.getSelectionModel().getSelectedItem();
    if (selected == null) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("แจ้งเตือน");
        alert.setHeaderText(null);
        alert.setContentText("กรุณาเลือกรายการก่อนพิมพ์ใบเสร็จ");
        alert.showAndWait();
        return;
    }

    try (PDDocument document = new PDDocument()) {
        PDPage page = new PDPage();
        document.addPage(page);

        // โหลดฟอนต์ไทยจาก resources (ต้องมีไฟล์ THSarabunNew.ttf ใน resources/fonts/)
        PDType0Font font = PDType0Font.load(document,
            getClass().getResourceAsStream("/fonts/THSarabunNew.ttf"));

        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        // หัวข้อกลางหน้า
        contentStream.beginText();
        contentStream.setFont(font, 18);
        contentStream.setLeading(22f);
        contentStream.newLineAtOffset(220, 750);
        contentStream.showText("ใบเสร็จการเช่า");
        contentStream.endText();

        // เส้นคั่น
        contentStream.moveTo(50, 730);
        contentStream.lineTo(550, 730);
        contentStream.stroke();

        // เนื้อหา
        contentStream.beginText();
        contentStream.setFont(font, 14);
        contentStream.setLeading(20f);
        contentStream.newLineAtOffset(70, 700);

        contentStream.showText("ลูกค้า: " + lblName.getText()); contentStream.newLine();
        contentStream.showText("เบอร์โทร: " + lblPhone.getText()); contentStream.newLine();
        contentStream.showText("สินค้า: " + selected.getItemName()); contentStream.newLine();
        contentStream.showText("วันที่เริ่ม: " + selected.getStartDate()); contentStream.newLine();
        contentStream.showText("วันที่สิ้นสุด: " + selected.getEndDate()); contentStream.newLine();
        contentStream.showText("สถานะชำระ: " + selected.getPaymentStatus()); contentStream.newLine();
        contentStream.showText("ช่องทางชำระ: " + selected.getPaymentMethod()); contentStream.newLine();
        contentStream.showText("ยอดชำระ: " + selected.getAmount()); contentStream.newLine();
        contentStream.showText("วันที่ชำระ: " + selected.getPaymentDate());

        contentStream.endText();
        contentStream.close();

        // ทำให้ชื่อไฟล์ปลอดภัย: ใช้เฉพาะ A-Z, a-z, 0-9
        String safeName = lblName.getText().replaceAll("[^A-Za-z0-9]", "_");

        // สร้างโฟลเดอร์ C:/RECEIPTS ถ้ายังไม่มี
        File directory = new File("C:/RECEIPTS");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // ตั้งชื่อไฟล์ให้บันทึกในโฟลเดอร์ที่ปลอดภัย
        String fileName = "C:/RECEIPTS/receipt_" + selected.getId() + "_" + safeName + ".pdf";

        // บันทึกไฟล์
        document.save(fileName);

        // แจ้งผู้ใช้เมื่อสำเร็จ
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("สำเร็จ");
        alert.setHeaderText(null);
        alert.setContentText("สร้างใบเสร็จเรียบร้อยแล้ว!\nไฟล์: " + fileName);
        alert.showAndWait();

    } catch (Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("ผิดพลาด");
        alert.setHeaderText(null);
        alert.setContentText("ไม่สามารถสร้างใบเสร็จได้: " + e.getMessage());
        alert.showAndWait();
        e.printStackTrace();
    }
}

   // ============================
//  INNER CLASS
// ============================
public static class RentalHistory {
    private final long id;
    private final String itemName;
    private final String startDate;
    private final String endDate;
    private String paymentStatus;
    private String paymentMethod;
    private String amount;
    private String paymentDate;

    public RentalHistory(long id, String itemName, String startDate, String endDate,
                         String paymentStatus, String paymentMethod, String amount, String paymentDate) {
        this.id = id;
        this.itemName = itemName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.paymentStatus = paymentStatus;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.paymentDate = paymentDate;
    }

    // overload constructor (5 args)
    public RentalHistory(long id, String itemName, String startDate, String endDate, String paymentStatus) {
        this(id, itemName, startDate, endDate, paymentStatus, "-", "-", "-");
    }

    // getters/setters
    public long getId() { return id; }
    public String getItemName() { return itemName; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getAmount() { return amount; }
    public String getPaymentDate() { return paymentDate; }
}

// ============================
//  HELPER แปลงสถานะอังกฤษ → ไทย
// ============================
private String toThaiStatus(String en) {
    if (en == null) return "-";
    return switch (en.toLowerCase()) {
        case "approved", "completed", "paid" -> "เสร็จสิ้น";
        case "pending" -> "รอดำเนินการ ⏳";
        case "cancelled", "rejected" -> "ยกเลิก";
        default -> en;
    };
}
}
