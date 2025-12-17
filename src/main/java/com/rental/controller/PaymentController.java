package com.rental.controller;
import javafx.scene.image.PixelReader;

import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class PaymentController implements Initializable {

    // ===== Toggle =====
    @FXML private ToggleGroup paymentGroup;
    @FXML private RadioButton radioBank;
    @FXML private RadioButton radioQR;

    // ===== Sections =====
    @FXML private VBox bankDetails;
    @FXML private VBox qrDetails;

    // ===== Form =====
    @FXML private Label lblFileStatus;
    @FXML private DatePicker datePicker;
    @FXML private TextArea txtNote;

    // ===== QR =====
    @FXML private ImageView qrImageView;
    @FXML private Button btnDownloadQr;


    private File selectedFile;

    // mock booking id (ภายหลังเปลี่ยนเป็นของจริงได้)
  

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        radioBank.setSelected(true);

        bankDetails.setVisible(true);
        bankDetails.setManaged(true);

        qrDetails.setVisible(false);
        qrDetails.setManaged(false);

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

    }

    // =========================
    // Smooth Switch
    // =========================
    private void switchSmooth(Node show, Node hide) {

        hide.setDisable(true);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), hide);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(200), hide);
        slideOut.setFromX(0);
        slideOut.setToX(-20);

        ParallelTransition out = new ParallelTransition(fadeOut, slideOut);
        out.setOnFinished(e -> {
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

    // =========================
    // Upload proof
    // =========================
    @FXML
    private void handleUpload(ActionEvent e) {

        FileChooser chooser = new FileChooser();
        chooser.setTitle("เลือกไฟล์หลักฐานการชำระเงิน");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File file = chooser.showOpenDialog(null);
        if (file != null) {
            selectedFile = file;
            lblFileStatus.setText(file.getName());
        }
    }

    // =========================
    // Download QR (PNG)
    // =========================
   @FXML
private void handleDownloadQr(ActionEvent e) {

    // อนุญาตเฉพาะ QR
    if (!radioQR.isSelected()) {
        new Alert(
                Alert.AlertType.WARNING,
                "สามารถดาวน์โหลดได้เฉพาะการชำระแบบ QR พร้อมเพย์",
                ButtonType.OK
        ).showAndWait();
        return;
    }

    Image image = qrImageView.getImage();
    if (image == null) {
        new Alert(
                Alert.AlertType.WARNING,
                "ไม่พบรูป QR สำหรับดาวน์โหลด",
                ButtonType.OK
        ).showAndWait();
        return;
    }

    String dateText = datePicker.getValue()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    String fileName = "QR_A01_" + dateText + ".png";

    FileChooser chooser = new FileChooser();
    chooser.setTitle("บันทึกไฟล์ QR พร้อมเพย์");
    chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PNG Image", "*.png")
    );
    chooser.setInitialFileName(fileName);

    File file = chooser.showSaveDialog(qrImageView.getScene().getWindow());
    if (file == null) return;

    try {
        // === แปลง Image -> BufferedImage (ไม่ใช้ SwingFXUtils) ===
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        BufferedImage bufferedImage =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        PixelReader reader = image.getPixelReader();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bufferedImage.setRGB(x, y, reader.getArgb(x, y));
            }
        }

        ImageIO.write(bufferedImage, "png", file);

    } catch (IOException ex) {
        new Alert(
                Alert.AlertType.ERROR,
                "ไม่สามารถบันทึกไฟล์ได้",
                ButtonType.OK
        ).showAndWait();
    }
}


    // =========================
    // Save (Mock)
    // =========================
    @FXML
    private void handleSave(ActionEvent e) {

        String paymentType = radioBank.isSelected()
                ? "Bank Transfer"
                : "QR PromptPay";

        String proof = selectedFile != null
                ? selectedFile.getName()
                : "ไม่มีไฟล์";

        String summary = """
                ✅ บันทึกการชำระเงิน
                วิธีชำระ: %s
                วันที่: %s
                หมายเหตุ: %s
                หลักฐาน: %s
                """.formatted(
                paymentType,
                datePicker.getValue(),
                txtNote.getText(),
                proof
        );

        new Alert(Alert.AlertType.INFORMATION, summary, ButtonType.OK).showAndWait();
    }

    // =========================
    // Clear
    // =========================
    @FXML
    private void handleClear(ActionEvent e) {

        paymentGroup.selectToggle(radioBank);

        bankDetails.setVisible(true);
        bankDetails.setManaged(true);

        qrDetails.setVisible(false);
        qrDetails.setManaged(false);

        datePicker.setValue(LocalDate.now());
        txtNote.clear();
        lblFileStatus.setText("ยังไม่ได้เลือกไฟล์");
        selectedFile = null;
    }
}
