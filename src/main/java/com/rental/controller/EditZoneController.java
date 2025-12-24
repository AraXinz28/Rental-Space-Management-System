package com.rental.controller;

import com.rental.database.SupabaseClient;
import com.rental.model.Zone;
import com.rental.util.SceneManager;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.json.JSONObject;

public class EditZoneController {

    @FXML private TextField zoneNameField;
    @FXML private TextField totalLocksField;
    @FXML private ComboBox<String> statusComboBox;

    private final SupabaseClient supabase = new SupabaseClient();
    private int zoneId;

    @FXML
    public void initialize() {
        statusComboBox.getItems().addAll(
                "เปิดให้บริการ",
                "ปิดปรับปรุง",
                "เต็ม"
        );
    }

    // รับข้อมูลจากหน้า ZoneManagement
    public void setZoneData(Zone zone) {
        this.zoneId = zone.getId();
        zoneNameField.setText(zone.getZoneName());
        totalLocksField.setText(String.valueOf(zone.getSlotCount()));
        statusComboBox.setValue(zone.getZoneStatus());
    }

    @FXML
    private void handleSave() {
    try {
        JSONObject body = new JSONObject();
        body.put("zone_name", zoneNameField.getText().trim());
        body.put("slot_count", Integer.parseInt(totalLocksField.getText()));
        body.put("zone_status", statusComboBox.getValue());

        supabase.updateById("zone", body.toString(), zoneId);

        showInformation("สำเร็จ", "แก้ไขโซนสำเร็จ");

        // ⭐ กลับไปหน้า zone_management
        Stage stage = (Stage) zoneNameField.getScene().getWindow();
        SceneManager.switchScene(stage, "/views/zone_management.fxml");

    } catch (NumberFormatException e) {
        showError("กรุณากรอกจำนวนล็อกเป็นตัวเลข");
    } catch (Exception e) {
        e.printStackTrace();
        showError("เกิดข้อผิดพลาดในการบันทึกข้อมูล");
    }
    }


    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) zoneNameField.getScene().getWindow();
        stage.close();
    }

    private void showInformation(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
