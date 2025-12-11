package com.rental.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class HeaderController {

    @FXML private Button userButton;

    /** เก็บ username ของผู้ใช้ที่กำลัง login */
    private static String currentUser = null;

    @FXML
    private void initialize() {
        updateUserDisplay();
    }

    /** อัปเดตชื่อบนปุ่ม */
    private void updateUserDisplay() {
        if (userButton != null) {
            userButton.setText(currentUser == null ? "ลงชื่อเข้าใช้" : currentUser);
        }
    }

    /** ใช้จากภายนอก: ตั้งค่าผู้ใช้หลัง login สำเร็จ */
    public static void setCurrentUser(String username) {
        currentUser = username;
    }

    /** ใช้จากภายนอก: อ่านชื่อผู้ใช้ */
    public static String getCurrentUser() {
        return currentUser;
    }

    /** handler ของปุ่ม user */
    @FXML
    private void handleUserButton() {

        if (currentUser == null) {
            goToLoginPage();
        } else {
            openUserMenu();
        }
    }

    /** ไปหน้า Login */
    private void goToLoginPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Stage stage = (Stage) userButton.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "ไม่สามารถเปิดหน้า Login ได้");
        }
    }

    /** เปิดเมนู Profile/Logout */
    private void openUserMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem profile = new MenuItem("Profile");
        profile.setOnAction(e -> {
            showAlert(Alert.AlertType.INFORMATION, "คุณกำลังกด Profile (ยังไม่ทำหน้า)");
        });

        MenuItem logout = new MenuItem("Logout");
        logout.setOnAction(e -> {
            currentUser = null;
            updateUserDisplay();
            showAlert(Alert.AlertType.INFORMATION, "ออกจากระบบสำเร็จ");
        });

        menu.getItems().addAll(profile, logout);
        menu.show(userButton, Side.BOTTOM, 0, 0);
    }

    /** popup alert */
    private void showAlert(Alert.AlertType type, String msg) {
        Alert alert = new Alert(type);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
