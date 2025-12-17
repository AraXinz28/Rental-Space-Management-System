package com.rental.controller;

import com.rental.database.SupabaseClient;
import com.rental.util.SceneManager;
import com.rental.util.Session;

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

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Button togglePasswordBtn;
    @FXML private Button userButton; 
    @FXML private Button registerBtn;

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
    if (Session.role == null) {
        userButton.setText("ลงชื่อเข้าใช้");
        return;
    }

    if ("admin".equalsIgnoreCase(Session.role)) {
        userButton.setText("ผู้ดูแลระบบ");
    } else {
        userButton.setText("ผู้ใช้งาน");
    }
}


    @FXML
private void handleUserButton() {
    if (currentUser == null) {
        try {
            Stage stage = (Stage) userButton.getScene().getWindow();
            SceneManager.switchScene(stage, "/views/login.fxml");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "ไม่สามารถเปิดหน้า Login ได้");
        }
    } else {
        ContextMenu menu = new ContextMenu();

        MenuItem logout = new MenuItem("Logout");
        logout.setOnAction(e -> {
            currentUser = null;
            updateUserButton();
            showAlert(Alert.AlertType.INFORMATION, "ออกจากระบบแล้ว");
        });

        menu.getItems().add(logout);
        menu.show(userButton, Side.BOTTOM, 0, 0);
    }
}


    @FXML
private void handleLogin(ActionEvent event) {
    String usernameOrEmail = usernameField.getText().trim();
    String password = passwordField.isVisible()
            ? passwordField.getText()
            : passwordTextField.getText();

    if (usernameOrEmail.isEmpty() || password.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "กรุณากรอกข้อมูลให้ครบ");
        return;
    }

    String hashedPassword = sha256(password);

    try {
        // ===== 1. หา user จาก users =====
        String responseUsername = supabase.selectWhere("users", "username", usernameOrEmail);
        String responseEmail = supabase.selectWhere("users", "email", usernameOrEmail);

        JSONArray usersByUsername = new JSONArray(responseUsername);
        JSONArray usersByEmail = new JSONArray(responseEmail);

        JSONObject user = null;
        if (usersByUsername.length() > 0) {
            user = usersByUsername.getJSONObject(0);
        } else if (usersByEmail.length() > 0) {
            user = usersByEmail.getJSONObject(0);
        }

        if (user == null) {
            showAlert(Alert.AlertType.ERROR, "ไม่พบบัญชีผู้ใช้");
            return;
        }

        if (!user.getString("password").equals(hashedPassword)) {
            showAlert(Alert.AlertType.ERROR, "รหัสผ่านไม่ถูกต้อง");
            return;
        }

        // ===== 2. หา role จาก profiles =====
        int userId = user.getInt("id");

        String profileResponse = supabase.selectWhere(
                "profiles",
                "user_id",
                String.valueOf(userId)
        );

        JSONArray profiles = new JSONArray(profileResponse);

        if (profiles.length() == 0) {
            showAlert(Alert.AlertType.ERROR, "ไม่พบ role ของผู้ใช้");
            return;
        }

        String role = profiles.getJSONObject(0).getString("role");

        // ===== 3. login สำเร็จ =====
        currentUser = user.getString("username");

        // ✅ เก็บค่าเข้า Session (สำคัญมาก)
        Session.username = currentUser;
        Session.role = role;

        // อัปเดตปุ่มมุมขวาบน
        updateUserButton();

        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();

        // ===== 4. เปลี่ยนหน้า ตาม role =====
        if ("admin".equalsIgnoreCase(role)) {
            SceneManager.switchScene(stage, "/views/zone_management.fxml");
        } else {
            SceneManager.switchScene(stage, "/views/homepage.fxml");
        }

    } catch (Exception e) {
        e.printStackTrace();
        showAlert(Alert.AlertType.ERROR, "เกิดข้อผิดพลาดในการเชื่อมต่อระบบ");
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
