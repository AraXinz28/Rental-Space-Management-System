package com.rental.controller;

import com.rental.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.Node;                 // ⭐ สำคัญมาก ต้องมี
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;

public class HomeController {

    @FXML private Button btnEnter; // ⭐ ปุ่มเข้าพื้นที่จำหน่าย

    @FXML
    public void initialize() {
        System.out.println("หน้าหลักผู้เช่าโหลดสำเร็จ");
    }

    // ===========================
    //  Hover Effect: ปุ่มขยาย + สีสว่างขึ้น
    // ===========================
    @FXML
    private void handleEnter(MouseEvent event) {
        Button btn = (Button) event.getSource();
        btn.setStyle(
                "-fx-padding: 15 40;" +
                "-fx-background-color: #ffa726;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 30;" +
                "-fx-scale-x: 1.05;" +
                "-fx-scale-y: 1.05;"
        );
    }

    // ===========================
    //  Hover ออก: กลับสภาพเดิม
    // ===========================
    @FXML
    private void handleExit(MouseEvent event) {
        Button btn = (Button) event.getSource();
        btn.setStyle(
                "-fx-padding: 15 40;" +
                "-fx-background-color: #7c7c7cff;" +
                "-fx-text-fill: white;" +
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 30;" +
                "-fx-scale-x: 1;" +
                "-fx-scale-y: 1;"
        );
    }

    // ===========================
    //  คลิกปุ่ม "เข้าพื้นที่จำหน่าย" → ไป Space.fxml
    // ===========================
    @FXML
    private void onEnterMarketClick(MouseEvent e) {
        try {
            Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
            SceneManager.switchScene(stage, "/views/Space.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("ไม่สามารถโหลดหน้า Space.fxml ได้");
        }
    }

    // ===========================
    //  คลิกดูแผนผังตลาด 1
    // ===========================
    @FXML
    private void openMap1() {
        openImagePopup("images/แผนผัง.png");
    }

    // ===========================
    //  คลิกดูแผนผังตลาด 2
    // ===========================
    @FXML
    private void openMap2() {
        openImagePopup("images/แผนผัง(2).png");
    }

    // ===========================
    //  ฟังก์ชันเปิดภาพแบบ Popup
    // ===========================
    private void openImagePopup(String imagePath) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setResizable(false);

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: rgba(0,0,0,0.7);");

        ImageView imageView = new ImageView(new Image(imagePath));
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(900);
        imageView.setFitHeight(900);

        root.getChildren().add(imageView);
        root.setOnMouseClicked(e -> popup.close());

        Scene scene = new Scene(root, 1100, 900);
        popup.setScene(scene);
        popup.show();
    }

    // ===========================
    //  Popup แจ้งเตือนทั่วไป
    // ===========================
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ข้อมูล");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
