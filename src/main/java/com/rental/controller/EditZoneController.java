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
    private String originalZoneName;

    /* ================= INITIALIZE ================= */
    @FXML
    public void initialize() {
        statusComboBox.getItems().addAll(
                "เปิดให้บริการ",
                "ปิดปรับปรุง",
                "เต็ม"
        );
    }

    /* ================= RECEIVE DATA ================= */
    public void setZoneData(Zone zone) {
        this.zoneId = zone.getId();
        this.originalZoneName = zone.getZoneName();

        zoneNameField.setText(zone.getZoneName());
        totalLocksField.setText(String.valueOf(zone.getSlotCount()));
        statusComboBox.setValue(zone.getZoneStatus());
    }

    /* ================= SAVE ================= */
    @FXML
    private void handleSave() {
        try {
            String zoneName = zoneNameField.getText().trim();
            int slotCount = Integer.parseInt(totalLocksField.getText());
            String newZoneStatus = statusComboBox.getValue();

            /* ===== update zone ===== */
            JSONObject zoneBody = new JSONObject();
            zoneBody.put("zone_name", zoneName);
            zoneBody.put("slot_count", slotCount);
            zoneBody.put("zone_status", newZoneStatus);

            supabase.updateById("zone", zoneBody.toString(), zoneId);

            /* ===== composition: update stalls ===== */
            updateStallsByZone(originalZoneName, newZoneStatus);

            showInformation("สำเร็จ", "แก้ไขโซนสำเร็จ");

            // กลับหน้า zone_management
            Stage stage = (Stage) zoneNameField.getScene().getWindow();
            SceneManager.switchScene(stage, "/views/zone_management.fxml");

        } catch (NumberFormatException e) {
            showError("กรุณากรอกจำนวนล็อกเป็นตัวเลข");
        } catch (Exception e) {
            e.printStackTrace();
            showError("เกิดข้อผิดพลาดในการบันทึกข้อมูล");
        }
    }

    /* ================= COMPOSITION LOGIC ================= */
    private void updateStallsByZone(String zoneName, String zoneStatus) throws Exception {

        JSONObject stallBody = new JSONObject();

            switch (zoneStatus) {
        case "ปิดปรับปรุง" -> 
            stallBody.put("status", "maintenance");

        case "เปิดให้บริการ" -> 
            stallBody.put("status", "available");

        case "เต็ม" -> 
            stallBody.put("status", "rented");

        default -> {
            return;
        }
    }

        // กัน stall ที่เช่าอยู่ ไม่ให้โดนทับ
        supabase.updateWhere(
            "stalls",
            "zone_name=eq." + zoneName + "&status=neq.rented",
            stallBody.toString()
        );
    }

    /* ================= CANCEL ================= */
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) zoneNameField.getScene().getWindow();
        stage.close();
    }

    /* ================= ALERT ================= */
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
