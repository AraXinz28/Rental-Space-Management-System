package com.rental.controller;

import com.rental.database.SupabaseClient;
import com.rental.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.json.JSONObject;

public class BookingController {

    @FXML
    private Label spaceIdLabel, zoneLabel, priceLabel, sizeLabel;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private TextField daysField;

    @FXML
    private Label errorLabel;

    private JSONObject selectedSpace;
    private SupabaseClient supabase = new SupabaseClient();

    /**
     * รับข้อมูลพื้นที่จากหน้า "ค้นหา"
     */
    public void setSelectedSpace(JSONObject space) {
        this.selectedSpace = space;

        spaceIdLabel.setText(space.get("id").toString());
        zoneLabel.setText(space.getString("zone"));
        priceLabel.setText(space.get("price").toString());
        sizeLabel.setText(space.getString("size"));
    }

    @FXML
    private void handleBooking() {
        try {
            // ตรวจสอบข้อมูล
            if (startDatePicker.getValue() == null) {
                errorLabel.setText("กรุณาเลือกวันที่เริ่มเช่า");
                return;
            }
            if (daysField.getText().isEmpty() || !daysField.getText().matches("\\d+")) {
                errorLabel.setText("จำนวนวันต้องเป็นตัวเลข");
                return;
            }

            int days = Integer.parseInt(daysField.getText());

            // สร้าง JSON ส่งไป Supabase
            JSONObject body = new JSONObject();
            body.put("space_id", selectedSpace.getInt("id"));
            body.put("start_date", startDatePicker.getValue().toString());
            body.put("days", days);
            body.put("status", "pending"); // ผู้ดูแลต้องอนุมัติ
            body.put("user_id", 1); // TODO: เปลี่ยนเป็น user ที่ล็อกอินจริง

            // ส่งคำขอจองไปฐานข้อมูล
            String response = supabase.insert("booking", body.toString());

            AlertUtil.showInfo("สำเร็จ", "ส่งคำขอจองเรียบร้อยแล้ว!");
            errorLabel.setText("");

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("เกิดข้อผิดพลาดในการบันทึกข้อมูล");
        }
    }
}
