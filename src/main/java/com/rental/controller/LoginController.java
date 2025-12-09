package com.rental.controller;

import com.rental.database.SupabaseClient;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField passwordTextField;
    @FXML
    private Button togglePasswordBtn;
    @FXML
    private Button userButton; 
    @FXML
    private Button registerBtn;

    private final SupabaseClient supabase = new SupabaseClient();

    // เก็บผู้ใช้งานปัจจุบัน (null = ยังไม่ login)
    private String currentUser = null;

    @FXML
    private void initialize() {
        updateUserButton();

        // ซิงค์ค่า PasswordField <-> TextField
        if (passwordTextField != null && passwordField != null) {
            passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
            passwordTextField.setVisible(false);

            togglePasswordBtn.setOnAction(e -> {
                boolean show = !passwordTextField.isVisible();
                passwordTextField.setVisible(show);
                passwordField.setVisible(!show);
            });
        }
    }

    private void updateUserButton() {
        if (currentUser != null) {
            userButton.setText(currentUser);
        } else {
            userButton.setText("ลงชื่อเข้าใช้");
        }
    }

    @FXML
    private void handleUserButton() {
        if (currentUser == null) {
            // ยังไม่ login → ไปหน้า Login
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
                Stage stage = (Stage) userButton.getScene().getWindow();
                stage.getScene().setRoot(loader.load());
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "ไม่สามารถไปหน้า Login ได้");
            }
        } else {
            // login แล้ว → เปิดเมนู Profile / Logout
                   ContextMenu contextMenu = new ContextMenu();

        MenuItem profileItem = new MenuItem("Profile");
        profileItem.setOnAction(e -> {

        });

        MenuItem logoutItem = new MenuItem("Logout");
        logoutItem.setOnAction(e -> {
            currentUser = null;
            updateUserButton();
            showAlert(Alert.AlertType.INFORMATION, "ออกจากระบบเรียบร้อยแล้ว");
        });

        contextMenu.getItems().addAll(profileItem, logoutItem);

        // แสดง popup ใต้ปุ่ม
        contextMenu.show(userButton, Side.BOTTOM, 0, 0);

        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String usernameOrEmail = usernameField.getText().trim();
        String password = passwordField.isVisible() ? passwordField.getText() : passwordTextField.getText();

        if (usernameOrEmail.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "กรุณากรอกข้อมูลให้ครบ");
            return;
        }

        String hashedPassword = sha256(password);

        try {
            String responseUsername = supabase.selectWhere("users", "username", usernameOrEmail);
            String responseEmail = supabase.selectWhere("users", "email", usernameOrEmail);

            JSONArray usersByUsername = new JSONArray(responseUsername);
            JSONArray usersByEmail = new JSONArray(responseEmail);

            JSONObject user = null;
            if (usersByUsername.length() > 0)
                user = usersByUsername.getJSONObject(0);
            else if (usersByEmail.length() > 0)
                user = usersByEmail.getJSONObject(0);

            if (user != null && user.getString("password").equals(hashedPassword)) {
                currentUser = user.getString("username");
                updateUserButton();
                showAlert(Alert.AlertType.INFORMATION, "เข้าสู่ระบบสำเร็จ");
            } else {
                showAlert(Alert.AlertType.ERROR, "ชื่อผู้ใช้หรือรหัสผ่านไม่ถูกต้อง");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "เกิดข้อผิดพลาดในการเชื่อมต่อ Supabase");
        }
    }

    @FXML
    private void handleRegisterButton(ActionEvent event) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/register.fxml"));
            Scene registerScene = new Scene(fxmlLoader.load());
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            stage.setScene(registerScene);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "ไม่สามารถเปิดหน้า Register ได้");
        }
    }

    private String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(null); 
        alert.setHeaderText(null); 
        alert.setContentText(message);
        alert.showAndWait();
    }

    // เรียกเมื่อ login สำเร็จจาก Register หรือหน้าอื่น
    public void setCurrentUser(String username) {
        this.currentUser = username;
        updateUserButton();
    }
}
