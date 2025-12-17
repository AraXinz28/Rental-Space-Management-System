package com.rental.controller;

import com.rental.util.SceneManager;
import com.rental.util.Session;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class HeaderController {

    @FXML
    private Button userButton;

    @FXML
    private void initialize() {
        updateUserDisplay();
    }

    /** อัปเดตข้อความบนปุ่ม */
    private void updateUserDisplay() {
        if (Session.role == null) {
            userButton.setText("ลงชื่อเข้าใช้");
        } else if ("admin".equalsIgnoreCase(Session.role)) {
            userButton.setText("ผู้ดูแลระบบ");
        } else {
            userButton.setText("ผู้ใช้งาน");
        }
    }

    @FXML
    private void handleUserButton() {
        if (Session.role == null) {
            goToLoginPage();
        } else {
            openUserMenu();
        }
    }

    private void goToLoginPage() {
        try {
            SceneManager.switchScene(
                (javafx.stage.Stage) userButton.getScene().getWindow(),
                "/views/login.fxml"
            );
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "ไม่สามารถเปิดหน้า Login ได้");
        }
    }

    private void openUserMenu() {
    ContextMenu menu = new ContextMenu();

    MenuItem logout = new MenuItem("Logout");
    logout.setOnAction(e -> {
        try {
            // 1. ล้าง session
            Session.clear();

            // 2. กลับไปหน้า home
            SceneManager.switchScene(
                (Stage) userButton.getScene().getWindow(),
                "/views/homepage.fxml"
            );

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "ไม่สามารถออกจากระบบได้");
        }
    });

    menu.getItems().add(logout);
    menu.show(userButton, Side.BOTTOM, 0, 0);
}


    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
