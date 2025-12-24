package com.rental.controller;

import com.rental.database.SupabaseClient;
import com.rental.model.Admin;
import com.rental.model.Tenant;
import com.rental.model.User;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private Button togglePasswordBtn;
    @FXML private Button userButton;
    @FXML private Button registerBtn;

    private final SupabaseClient supabase = new SupabaseClient();

    @FXML
    private void initialize() {
        updateUserButton();

        // ซิงค์ค่า PasswordField <-> TextField
        if (passwordTextField != null && passwordField != null) {
            passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
            passwordTextField.setVisible(false);

            if (togglePasswordBtn != null) {
                togglePasswordBtn.setOnAction(e -> {
                    boolean show = !passwordTextField.isVisible();
                    passwordTextField.setVisible(show);
                    passwordField.setVisible(!show);
                });
            }
        }
    }

    private void updateUserButton() {
        if (!Session.isLoggedIn()) {
            userButton.setText("ลงชื่อเข้าใช้");
            return;
        }
        userButton.setText(Session.username());
    }

    @FXML
    private void handleUserButton() {
        if (!Session.isLoggedIn()) {
            try {
                Stage stage = (Stage) userButton.getScene().getWindow();
                SceneManager.switchScene(stage, "/views/login.fxml");
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "ไม่สามารถเปิดหน้า Login ได้");
            }
        } else {
            ContextMenu menu = new ContextMenu();

            MenuItem roleItem = new MenuItem(
                    "admin".equalsIgnoreCase(Session.role()) ? "🔧 ผู้ดูแลระบบ" : "👤 ผู้ใช้งาน"
            );
            roleItem.setDisable(true);

            MenuItem logout = new MenuItem("🚪 Logout");
            logout.setOnAction(e -> {
                Session.clear();
                updateUserButton();
                showAlert(Alert.AlertType.INFORMATION, "ออกจากระบบแล้ว");
            });

            menu.getItems().addAll(roleItem, new SeparatorMenuItem(), logout);
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
            // ===== 1) หา user จาก users (username หรือ email) =====
            String responseUsername = supabase.selectWhere("users", "username", usernameOrEmail);
            String responseEmail    = supabase.selectWhere("users", "email", usernameOrEmail);

            JSONArray usersByUsername = new JSONArray(responseUsername);
            JSONArray usersByEmail    = new JSONArray(responseEmail);

            JSONObject userJson = null;
            if (usersByUsername.length() > 0) {
                userJson = usersByUsername.getJSONObject(0);
            } else if (usersByEmail.length() > 0) {
                userJson = usersByEmail.getJSONObject(0);
            }

            if (userJson == null) {
                showAlert(Alert.AlertType.ERROR, "ไม่พบบัญชีผู้ใช้");
                return;
            }

            if (!userJson.getString("password").equals(hashedPassword)) {
                showAlert(Alert.AlertType.ERROR, "รหัสผ่านไม่ถูกต้อง");
                return;
            }

            // ===== 2) หา role จาก profiles =====
            int userId = userJson.getInt("id");

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

            JSONObject profile = profiles.getJSONObject(0);
            String role = profile.getString("role");

            // ===== 3) สร้าง User object แล้วเก็บใน Session =====
            String username = userJson.optString("username", "");
            String email    = userJson.optString("email", "");
            String fullName = userJson.optString("full_name", username);
            String phone    = userJson.optString("phone", "-"); // <-- ดึงจาก users table

            User loggedIn;
            if ("admin".equalsIgnoreCase(role)) {
                loggedIn = new Admin(userId, username, email);
            } else {
                loggedIn = new Tenant(userId, username, email, fullName, phone);
            }

            Session.login(loggedIn);
            updateUserButton();

            // ===== 4) เปลี่ยนหน้า (polymorphism) =====
            Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
            SceneManager.switchScene(stage, loggedIn.getHomeFxml());

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
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
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

    // กันโค้ดเก่าที่เคยเรียกไว้ (ไม่จำเป็นแล้ว)
    public void setCurrentUser(String username) {
        updateUserButton();
    }
}
