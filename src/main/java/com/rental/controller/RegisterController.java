package com.rental.controller;

import com.rental.database.SupabaseClient;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private Button cancelButton;
    @FXML private Button registerButton;
    @FXML private Button userButton; 

    private SupabaseClient supabaseClient;

    // เก็บผู้ใช้งานปัจจุบัน (null = ยังไม่ login)
    private String currentUser = null;

    public RegisterController() {
        this.supabaseClient = new SupabaseClient();
    }

    @FXML
    public void initialize() {
        setupValidation();
        updateUserButton();
    }

    private void setupValidation() {
        usernameField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV.contains(" ")) usernameField.setText(oldV);
        });
        phoneField.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*")) phoneField.setText(oldV);
        });
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
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
                Stage stage = (Stage) userButton.getScene().getWindow();
                stage.getScene().setRoot(loader.load());
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "เกิดข้อผิดพลาด", "ไม่สามารถไปหน้า Login ได้");
            }
        } else {
        }
    }

    @FXML
    private void handleCancel() {
        handleUserButton();
    }

    @FXML
    private void handleRegister() {
        if (!validateInput()) return;

        try {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();
            String fullName = nameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();

            if (isUsernameExists(username)) {
                showAlert(Alert.AlertType.ERROR, "ชื่อผู้ใช้ซ้ำ", "ชื่อผู้ใช้นี้มีในระบบแล้ว");
                return;
            }
            if (isEmailExists(email)) {
                showAlert(Alert.AlertType.ERROR, "อีเมลซ้ำ", "อีเมลนี้มีในระบบแล้ว");
                return;
            }

            String hashedPassword = hashPassword(password);

            JSONObject jsonData = new JSONObject();
            jsonData.put("username", username);
            jsonData.put("password", hashedPassword);
            jsonData.put("full_name", fullName);
            jsonData.put("email", email);
            jsonData.put("phone", phone);

            String response = supabaseClient.insert("users", jsonData.toString());

            if (response != null && !response.contains("error")) {
                showAlert(Alert.AlertType.INFORMATION, "สำเร็จ", "ลงทะเบียนสำเร็จ! คุณสามารถเข้าสู่ระบบได้แล้ว");
                clearFields();
                handleUserButton(); 
            } else {
                showAlert(Alert.AlertType.ERROR, "ผิดพลาด", "ไม่สามารถลงทะเบียนได้ กรุณาลองใหม่");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "ข้อผิดพลาด", "เกิดข้อผิดพลาด: " + e.getMessage());
        }
    }

    // --- ฟังก์ชันช่วยเหลือ ---
    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (usernameField.getText().trim().isEmpty()) {
            errors.append("- กรุณากรอก Username\n");
        } else if (usernameField.getText().trim().length() < 4) {
            errors.append("- Username ต้องมีอย่างน้อย 4 ตัวอักษร\n");
        }

        if (passwordField.getText().isEmpty()) {
            errors.append("- กรุณากรอก Password\n");
        } else if (passwordField.getText().length() < 6) {
            errors.append("- Password ต้องมีอย่างน้อย 6 ตัวอักษร\n");
        }

        if (nameField.getText().trim().isEmpty()) {
            errors.append("- กรุณากรอกชื่อ-สกุล\n");
        }

        if (emailField.getText().trim().isEmpty()) {
            errors.append("- กรุณากรอก E-mail\n");
        } else if (!isValidEmail(emailField.getText().trim())) {
            errors.append("- รูปแบบ E-mail ไม่ถูกต้อง\n");
        }

        if (phoneField.getText().trim().isEmpty()) {
            errors.append("- กรุณากรอกเบอร์โทร\n");
        } else if (phoneField.getText().trim().length() < 9 || phoneField.getText().trim().length() > 10) {
            errors.append("- เบอร์โทรต้องมี 9-10 หลัก\n");
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "ข้อมูลไม่ครบถ้วน", errors.toString());
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    private boolean isUsernameExists(String username) throws Exception {
        String response = supabaseClient.selectWhere("users", "username", username);
        JSONArray jsonArray = new JSONArray(response);
        return jsonArray.length() > 0;
    }

    private boolean isEmailExists(String email) throws Exception {
        String response = supabaseClient.selectWhere("users", "email", email);
        JSONArray jsonArray = new JSONArray(response);
        return jsonArray.length() > 0;
    }

    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(password.getBytes());

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }

        return hexString.toString();
    }

    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearFields() {
        usernameField.clear();
        passwordField.clear();
        nameField.clear();
        emailField.clear();
        phoneField.clear();
    }

    // เรียกเมื่อ login สำเร็จ
    public void setCurrentUser(String username) {
        this.currentUser = username;
        updateUserButton();
    }
}
