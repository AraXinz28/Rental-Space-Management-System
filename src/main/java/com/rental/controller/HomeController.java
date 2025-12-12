package com.rental.controller;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

public class HomeController {
      @FXML
    public void initialize() {
        System.out.println("หน้าหลักผู้เช่าโหลดสำเร็จ");
    }

    // ✅ Hover: ปุ่มขยาย + สีสว่างขึ้น
    @FXML
    private void handleEnter(javafx.scene.input.MouseEvent event) {
        Button btn = (Button) event.getSource();
        btn.setStyle("-fx-padding: 15 40;"
                + "-fx-background-color: #ffa726;"   // สีสว่างขึ้น
                + "-fx-text-fill: white;"
                + "-fx-font-size: 16px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 30;"
                + "-fx-scale-x: 1.05;"               // ✅ ขยาย 5%
                + "-fx-scale-y: 1.05;");
    }

    // ✅ Hover ออก: กลับสภาพเดิม
    @FXML
    private void handleExit(javafx.scene.input.MouseEvent event) {
        Button btn = (Button) event.getSource();
        btn.setStyle("-fx-padding: 15 40;"
                + "-fx-background-color: #7c7c7cff;"   // สีเดิม
                + "-fx-text-fill: white;"
                + "-fx-font-size: 16px;"
                + "-fx-font-weight: bold;"
                + "-fx-background-radius: 30;"
                + "-fx-scale-x: 1;"
                + "-fx-scale-y: 1;");
    }

    // ✅ เมื่อคลิกปุ่ม "เข้าพื้นที่จำหน่าย"
    @FXML
    private void onEnterMarketClick() {
        System.out.println("คลิก: เข้าพื้นที่จำหน่าย");
        showAlert("กำลังเข้าสู่พื้นที่จำหน่าย...");
    }

    // ✅ เมื่อคลิกดูแผนผังตลาด 1
    @FXML
    private void onViewMap1Click() {
        System.out.println("คลิก: ดูแผนผังตลาด 1");
        showAlert("แสดงแผนผังตลาด 1");
    }

    // ✅ เมื่อคลิกดูแผนผังตลาด 2
    @FXML
    private void onViewMap2Click() {
        System.out.println("คลิก: ดูแผนผังตลาด 2");
        showAlert("แสดงแผนผังตลาด 2");
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ข้อมูล");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
