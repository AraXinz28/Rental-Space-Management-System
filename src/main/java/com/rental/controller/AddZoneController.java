package com.rental.controller;

import com.rental.database.SupabaseClient;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import org.json.JSONObject;

public class AddZoneController {

    @FXML private TextField zoneNameField;
    @FXML private TextField slotCountField;

    private final SupabaseClient supabase = new SupabaseClient();

    private boolean isZoneExists(String zoneName) throws Exception {

    // select * from zone where zone_name = 'A'
    String response = supabase.selectWhere("zone", "zone_name", zoneName);

    // ถ้ามีข้อมูลกลับมา แปลว่าซ้ำ
    return response != null && !response.equals("[]");
    }

    @FXML
    private void handleAddZone() {

    String zoneName = zoneNameField.getText().trim().toUpperCase();
    String slotText = slotCountField.getText().trim();

    // 1. ตรวจสอบค่าว่าง
    if (zoneName.isEmpty() || slotText.isEmpty()) {
        showAlert("กรุณากรอกข้อมูลให้ครบ");
        return;
    }

    // 2. ตรวจสอบตัวเลข
    int slotCount;
    try {
        slotCount = Integer.parseInt(slotText);
        if (slotCount <= 0) {
            showAlert("จำนวนล็อกต้องมากกว่า 0");
            return;
        }
    } catch (NumberFormatException e) {
        showAlert("จำนวนล็อกต้องเป็นตัวเลข");
        return;
    }

    try {
        // ⭐ 3. ตรวจสอบโซนซ้ำ
        if (isZoneExists(zoneName)) {
            showAlert("มีโซนนี้อยู่แล้ว");
            return;
        }

        // 4. INSERT ZONE
        JSONObject zoneBody = new JSONObject();
        zoneBody.put("zone_name", zoneName);
        zoneBody.put("slot_count", slotCount);
        zoneBody.put("zone_status", "เปิดให้บริการ");

        supabase.insert("zone", zoneBody.toString());

        // 5. CREATE STALLS
        createStalls(zoneName, slotCount);

        showSuccess("เพิ่มโซนและสร้างพื้นที่เรียบร้อย");

        zoneNameField.clear();
        slotCountField.clear();

    } catch (Exception e) {
        e.printStackTrace();
        showAlert("เพิ่มโซนไม่สำเร็จ");
    }
    }


    // ===== สร้างพื้นที่อัตโนมัติ =====
    private void createStalls(String zoneName, int slotCount) throws Exception {

        for (int i = 1; i <= slotCount; i++) {

            String stallId = String.format("%s%02d", zoneName, i);
            // A01, A02, A03 ...

            String jsonBody = String.format("""
            {
                "zone_name": "%s",
                "stall_id": "%s",
                "size": "",
                "daily_rate": 0,
                "status": "available",
                "amenities": ""
            }
            """, zoneName, stallId);

            supabase.insert("stalls", jsonBody);
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }

    private void showSuccess(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.show();
    }
}
