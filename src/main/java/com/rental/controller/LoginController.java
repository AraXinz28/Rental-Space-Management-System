package com.rental.controller;

import com.rental.database.SupabaseClient;
import com.rental.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    private final SupabaseClient supabase = new SupabaseClient();

    @FXML
    private void handleLogin() {
        String email = usernameField.getText().trim();
        String pass = passwordField.getText().trim();

        if (email.isEmpty() || pass.isEmpty()) {
            errorLabel.setText("กรุณากรอกข้อมูลให้ครบ");
            return;
        }

        try {
            String response = supabase.selectWhere("user", "email", email);
            JSONArray users = new JSONArray(response);

            if (users.length() > 0) {
                JSONObject user = users.getJSONObject(0);
                String dbPassword = user.getString("password");

                if (dbPassword.equals(pass)) { // ถ้าใช้ hash ต้องเปลี่ยนวิธีตรวจสอบ
                    AlertUtil.showInfo("Login สำเร็จ", "ยินดีต้อนรับ " + user.getString("name") + "!");
                    errorLabel.setText("");
                    // TODO: ไปหน้าหลัก
                } else {
                    errorLabel.setText("รหัสผ่านไม่ถูกต้อง");
                }
            } else {
                errorLabel.setText("ไม่พบผู้ใช้นี้");
            }

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("เกิดข้อผิดพลาดในการเชื่อมต่อ Supabase");
        }
    }
}
