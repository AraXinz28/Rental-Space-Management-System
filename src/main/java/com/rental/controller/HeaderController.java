package com.rental.controller;

import com.rental.model.User;
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

    /** อัปเดตข้อความบนปุ่ม -> แสดงชื่อผู้ใช้ */
    private void updateUserDisplay() {
        if (!Session.isLoggedIn()) {
            userButton.setText("ลงชื่อเข้าใช้");
            return;
        }

        User user = Session.getCurrentUser();
        String name = (user.getUsername() == null || user.getUsername().isBlank())
                ? "ผู้ใช้งาน"
                : user.getUsername();

        userButton.setText(name);
    }

    @FXML
    private void handleUserButton() {
        if (!Session.isLoggedIn()) {
            goToLoginPage();
        } else {
            openUserMenu();
        }
    }

    private void goToLoginPage() {
        try {
            SceneManager.switchScene(
                    (Stage) userButton.getScene().getWindow(),
                    "/views/login.fxml"
            );
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "ไม่สามารถเปิดหน้า Login ได้");
        }
    }

    private void openUserMenu() {
        ContextMenu menu = new ContextMenu();
        User user = Session.getCurrentUser();

        MenuItem roleItem = new MenuItem(
                "admin".equalsIgnoreCase(user.getRole()) ? "🔧 ผู้ดูแลระบบ" : "👤 ผู้ใช้งาน"
        );
        roleItem.setDisable(true);

        MenuItem logout = new MenuItem("🚪 Logout");
        logout.setOnAction(e -> {
            try {
                Session.clear();
                updateUserDisplay();

                SceneManager.switchScene(
                        (Stage) userButton.getScene().getWindow(),
                        "/views/homepage.fxml"
                );
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "ไม่สามารถออกจากระบบได้");
            }
        });

        menu.getItems().addAll(roleItem, new SeparatorMenuItem(), logout);
        menu.show(userButton, Side.BOTTOM, 0, 0);
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}