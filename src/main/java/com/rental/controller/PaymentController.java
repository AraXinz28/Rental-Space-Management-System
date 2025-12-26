package com.rental.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rental.util.Session;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import okhttp3.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;

public class PaymentController implements Initializable {

    // ===== FXML Elements =====
    @FXML private ToggleGroup paymentGroup;
    @FXML private RadioButton radioBank;
    @FXML private RadioButton radioQR;

    @FXML private VBox bankDetails;
    @FXML private VBox qrDetails;

    @FXML private Label lblFileStatus;
    @FXML private DatePicker datePicker;
    @FXML private TextArea txtNote;

    @FXML private Label lblStallId;
    @FXML private Label lblDateRange;
    @FXML private Label lblProductType;
    @FXML private Label lblRentFee;
    @FXML private Label lblDeposit;
    @FXML private Label lblTotalAmount;

    @FXML private Label lblLoadStatus;

    @FXML private ImageView qrImageView;
    @FXML private Button btnDownloadQr;

    private File selectedFile;
    private long currentBookingId = 0;
    private JsonObject currentBooking;

    // ===== Supabase Config =====
    private static final String SUPABASE_URL = "https://sdmipxsxkquuyxvvqpho.supabase.co";
    private static final String SUPABASE_SERVICE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNkbWlweHN4a3F1dXl4dnZxcGhvIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc2NDc1MzU0NywiZXhwIjoyMDgwMzI5NTQ3fQ.IqSxzTLKHXlfGdH4RyzaYAIVXrlW7_LsrQEuJBlHJ8k";
    private static final String STORAGE_BUCKET = "payment-proofs";

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Gson gson = new Gson();
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private long getCurrentUserId() {
        if (Session.isLoggedIn() && Session.getCurrentUser() != null) {
            return Session.getCurrentUser().getId();
        }
        return 0;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        radioBank.setSelected(true);
        bankDetails.setVisible(true);
        bankDetails.setManaged(true);
        qrDetails.setVisible(false);
        qrDetails.setManaged(false);
        btnDownloadQr.setVisible(false);
        btnDownloadQr.setManaged(false);

        datePicker.setValue(LocalDate.now());
        lblFileStatus.setText("ยังไม่ได้เลือกไฟล์");

        paymentGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == radioQR) {
                switchSmooth(qrDetails, bankDetails);
                btnDownloadQr.setVisible(true);
                btnDownloadQr.setManaged(true);
            } else {
                switchSmooth(bankDetails, qrDetails);
                btnDownloadQr.setVisible(false);
                btnDownloadQr.setManaged(false);
            }
        });

        loadLatestBookingForCurrentUser();
    }

    private void loadLatestBookingForCurrentUser() {
        long userId = getCurrentUserId();

        if (userId == 0) {
            lblLoadStatus.setText("กรุณาล็อกอินก่อนใช้งาน");
            lblLoadStatus.setStyle("-fx-text-fill: red;");
            clearSummaryCard();
            return;
        }

        lblLoadStatus.setText("กำลังโหลดข้อมูลการจองล่าสุดของคุณ...");
        lblLoadStatus.setStyle("-fx-text-fill: #666;");

        Task<JsonObject> task = new Task<>() {
            @Override
            protected JsonObject call() throws Exception {
                // เพิ่มเงื่อนไขไม่โหลดรายการที่ชำระแล้ว
                String url = SUPABASE_URL + "/rest/v1/bookings?user_id=eq." + userId +
                             "&status=neq.paid" +
                             "&order=created_at.desc&limit=1&select=*";

                Request request = new Request.Builder()
                        .url(url)
                        .header("apikey", SUPABASE_SERVICE_KEY)
                        .header("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("HTTP " + response.code() + ": " + response.body().string());
                    }
                    String body = response.body().string();
                    JsonArray array = gson.fromJson(body, JsonArray.class);
                    if (array.size() == 0) {
                        throw new Exception("ไม่มีรายการจองที่ต้องชำระ");
                    }
                    return array.get(0).getAsJsonObject();
                }
            }
        };

        task.setOnSucceeded(e -> {
            currentBooking = task.getValue();
            currentBookingId = currentBooking.get("booking_id").getAsLong();

            updateSummaryCard(currentBooking);

            String fullName = currentBooking.get("full_name").getAsString();
            lblLoadStatus.setText("โหลดข้อมูลการจองล่าสุดสำเร็จ – คุณ " + fullName);
            lblLoadStatus.setStyle("-fx-text-fill: green;");
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            String message = ex.getMessage();
            if (message != null && message.contains("ไม่มีรายการจองที่ต้องชำระ")) {
                lblLoadStatus.setText("คุณไม่มีรายการจองที่ต้องชำระเงินในขณะนี้ 🎉");
                lblLoadStatus.setStyle("-fx-text-fill: green;");
            } else {
                lblLoadStatus.setText("ไม่สามารถโหลดข้อมูลได้");
                lblLoadStatus.setStyle("-fx-text-fill: red;");
                showAlert(Alert.AlertType.ERROR, "เกิดข้อผิดพลาด: " + message);
            }
            clearSummaryCard();
        });

        new Thread(task).start();
    }

    private void updateSummaryCard(JsonObject booking) {
        String stall = booking.get("stall_id").getAsString();
        String productType = booking.get("product_type").getAsString();
        String start = booking.get("start_date").getAsString();
        String end = booking.get("end_date").getAsString();

        double totalPrice = booking.get("total_price").getAsDouble();
        double depositPrice = booking.get("deposit_price").getAsDouble();
        double rent = totalPrice - depositPrice;

        lblStallId.setText("หมายเลขพื้นที่ : " + stall);
        lblDateRange.setText("วันที่จอง : " + formatThaiDate(start) + " ถึง " + formatThaiDate(end));
        lblProductType.setText("ประเภทสินค้า: " + productType);
        lblRentFee.setText(String.format("%,.0f ฿", rent));
        lblDeposit.setText(String.format("%,.0f ฿", depositPrice));
        lblTotalAmount.setText(String.format("%,.0f ฿", totalPrice));
    }

    @FXML
    private void handleUpload(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("เลือกไฟล์หลักฐานการชำระเงิน");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));

        File file = chooser.showOpenDialog(null);
        if (file != null) {
            selectedFile = file;
            lblFileStatus.setText(file.getName());
        }
    }

    @FXML
    private void handleDownloadQr(ActionEvent e) {
        if (!radioQR.isSelected()) {
            showAlert(Alert.AlertType.WARNING, "สามารถดาวน์โหลดได้เฉพาะการชำระแบบ QR พร้อมเพย์");
            return;
        }

        Image image = qrImageView.getImage();
        if (image == null) {
            showAlert(Alert.AlertType.WARNING, "ไม่พบรูป QR สำหรับดาวน์โหลด");
            return;
        }

        String dateText = datePicker.getValue().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = "QR_PromptPay_" + dateText + ".png";

        FileChooser chooser = new FileChooser();
        chooser.setTitle("บันทึกไฟล์ QR พร้อมเพย์");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        chooser.setInitialFileName(fileName);

        File file = chooser.showSaveDialog(qrImageView.getScene().getWindow());
        if (file == null) return;

        try {
            int width = (int) image.getWidth();
            int height = (int) image.getHeight();
            BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            PixelReader reader = image.getPixelReader();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    buffered.setRGB(x, y, reader.getArgb(x, y));
                }
            }
            ImageIO.write(buffered, "png", file);
            showAlert(Alert.AlertType.INFORMATION, "บันทึก QR สำเร็จ!");
        } catch (IOException ex) {
            showAlert(Alert.AlertType.ERROR, "ไม่สามารถบันทึกไฟล์ได้");
        }
    }

    @FXML
    private void handleSave(ActionEvent e) {
        if (currentBookingId == 0) {
            showAlert(Alert.AlertType.WARNING, "ไม่มีข้อมูลการจองที่ต้องชำระ");
            return;
        }
        if (selectedFile == null && radioBank.isSelected()) {
            showAlert(Alert.AlertType.WARNING, "กรุณาอัปโหลดหลักฐานการชำระเงิน");
            return;
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String proofUrl = null;
                if (selectedFile != null) {
                    String fileName = "proof_" + currentBookingId + "_" + System.currentTimeMillis() + ".jpg";
                    RequestBody fileBody = RequestBody.create(selectedFile, MediaType.get("image/jpeg"));

                    MultipartBody multipart = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", fileName, fileBody)
                        .addFormDataPart("cache-control", "max-age=3600")
                        .build();

                    Request uploadReq = new Request.Builder()
                        .url(SUPABASE_URL + "/storage/v1/object/" + STORAGE_BUCKET + "/" + fileName)
                        .post(multipart)
                        .header("apikey", SUPABASE_SERVICE_KEY)
                        .header("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                        .header("x-upsert", "true")
                        .build();

                    try (Response resp = httpClient.newCall(uploadReq).execute()) {
                        if (!resp.isSuccessful()) {
                            throw new Exception("อัปโหลดสลิปไม่สำเร็จ: " + resp.code() + " - " + resp.body().string());
                        }
                    }

                    proofUrl = SUPABASE_URL + "/storage/v1/object/public/" + STORAGE_BUCKET + "/" + fileName;
                }

                double amount = currentBooking.get("total_price").getAsDouble();
                String method = radioBank.isSelected() ? "Bank Transfer" : "QR PromptPay";

                Map<String, Object> data = new HashMap<>();
                data.put("booking_id", currentBookingId);
                data.put("payment_method", method);
                data.put("payment_date", datePicker.getValue().toString());
                data.put("amount", amount);
                data.put("proof_image_url", proofUrl);
                data.put("note", txtNote.getText().trim().isEmpty() ? null : txtNote.getText().trim());
                data.put("status", "pending");

                String json = gson.toJson(data);
                RequestBody body = RequestBody.create(json, JSON);

                Request req = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/payments")
                        .post(body)
                        .header("apikey", SUPABASE_SERVICE_KEY)
                        .header("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=minimal")
                        .build();

                try (Response resp = httpClient.newCall(req).execute()) {
                    if (!resp.isSuccessful()) throw new Exception("บันทึกไม่สำเร็จ: " + resp.body().string());
                }

                // ===== อัปเดตสถานะ bookings เป็น paid =====
                JsonObject updateData = new JsonObject();
                updateData.addProperty("status", "paid");

                String updateJson = updateData.toString();
                RequestBody updateBody = RequestBody.create(updateJson, JSON);

                Request updateReq = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/bookings?booking_id=eq." + currentBookingId)
                        .patch(updateBody)
                        .header("apikey", SUPABASE_SERVICE_KEY)
                        .header("Authorization", "Bearer " + SUPABASE_SERVICE_KEY)
                        .header("Content-Type", "application/json")
                        .header("Prefer", "return=minimal")
                        .build();

                try (Response updateResp = httpClient.newCall(updateReq).execute()) {
                    if (!updateResp.isSuccessful()) {
                        System.out.println("อัปเดตสถานะ bookings ล้มเหลว: " + updateResp.code());
                    }
                }

                return null;
            }
        };

        task.setOnSucceeded(ev -> {
            showAlert(Alert.AlertType.INFORMATION, "✅ บันทึกการชำระเงินสำเร็จแล้ว!\nรอการยืนยันจากทีมงาน");
            handleClear(e);
            // โหลดใหม่เพื่อเช็คว่ารายการหายไปหรือไม่
            loadLatestBookingForCurrentUser();
        });

        task.setOnFailed(ev -> showAlert(Alert.AlertType.ERROR, "เกิดข้อผิดพลาด: " + task.getException().getMessage()));

        new Thread(task).start();
    }

    @FXML
    private void handleClear(ActionEvent e) {
        selectedFile = null;
        txtNote.clear();
        lblFileStatus.setText("ยังไม่ได้เลือกไฟล์");
        datePicker.setValue(LocalDate.now());
        paymentGroup.selectToggle(radioBank);
        switchSmooth(bankDetails, qrDetails);
    }

    private void switchSmooth(Node show, Node hide) {
        hide.setDisable(true);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), hide);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(200), hide);
        slideOut.setFromX(0);
        slideOut.setToX(-20);

        ParallelTransition out = new ParallelTransition(fadeOut, slideOut);
        out.setOnFinished(ev -> {
            hide.setVisible(false);
            hide.setManaged(false);
            hide.setOpacity(1);
            hide.setTranslateX(0);
        });

        show.setVisible(true);
        show.setManaged(true);
        show.setOpacity(0);
        show.setTranslateX(20);
        show.setDisable(false);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), show);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(250), show);
        slideIn.setFromX(20);
        slideIn.setToX(0);

        new ParallelTransition(fadeIn, slideIn).play();
        out.play();
    }

    private void clearSummaryCard() {
        lblStallId.setText("หมายเลขพื้นที่ : -");
        lblDateRange.setText("วันที่จอง : -");
        lblProductType.setText("ประเภทสินค้า: -");
        lblRentFee.setText("0 ฿");
        lblDeposit.setText("0 ฿");
        lblTotalAmount.setText("0 ฿");
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String formatThaiDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.of("th", "TH")));
        } catch (Exception e) {
            return dateStr;
        }
    }
}