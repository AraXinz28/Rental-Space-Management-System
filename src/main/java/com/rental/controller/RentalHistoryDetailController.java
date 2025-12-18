package com.rental.controller;

import com.rental.model.RentalHistoryRow;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.text.DecimalFormat;
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

    public void setData(RentalHistoryRow row) {
        lblName.setText(row.getCustomerName());
        lblDate.setText(row.getDate() == null ? "-" : TH_DATE.format(row.getDate()));
        setStatus(row.getStatus());

        // ---- ส่วนนี้ยังไม่มีใน model ของคุณ เลยใส่ตัวอย่างให้เหมือนรูปก่อน ----
        lblPhone.setText("097-231-8564");
        lblZone.setText("A");
        lblLockNo.setText("14");
        lblProductType.setText("อาหาร");
        lblPayment.setText("โอนผ่านธนาคาร");

        lblDeposit.setText(MONEY.format(0) + " ฿");
        lblTotalRent.setText(MONEY.format(150) + " ฿");
        lblPaidTotal.setText(MONEY.format(150) + " ฿");
    }

    private void setStatus(String status) {
        String text = (status == null) ? "-" : status;
        if ("เสร็จสิ้น".equals(text))
            text = "อนุมัติ";
        lblStatusPill.setText(text);

        String base = "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 6 18; -fx-background-radius: 999;";
        if ("อนุมัติ".equals(text) || "เสร็จสิ้น".equals(status)) {
            lblStatusPill.setStyle(base + "-fx-background-color: #4F6F4A;");
        } else if ("ยกเลิก".equals(status)) {
            lblStatusPill.setStyle(base + "-fx-background-color: #6B3F3F;");
        } else {
            lblStatusPill.setStyle(base + "-fx-background-color: #777;");
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

    @FXML
    private void onPrintSlip(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("พิมพ์ใบเสร็จ");
        alert.setHeaderText("ตัวอย่างการพิมพ์ใบเสร็จ");
        alert.setContentText("กำลังพิมพ์ใบเสร็จของ: " + lblName.getText());
        alert.showAndWait();
    }
}
