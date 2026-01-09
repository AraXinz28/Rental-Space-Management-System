package com.rental.controller;

import com.rental.database.SupabaseClient;
import com.rental.model.Stall;
import com.rental.util.SceneManager;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class EditSpaceController {

    @FXML private ComboBox<String> zoneCombo;
    @FXML private TextField stallIdField;
    @FXML private TextField sizeField;
    @FXML private TextField priceField;
    @FXML private ToggleGroup statusGroup;
    @FXML private RadioButton statusAvailable;
    @FXML private RadioButton statusRented;
    @FXML private RadioButton statusMaintenance;
    @FXML private RadioButton statusProcessing;


    private Stall originalStall;

    @FXML
    private void initialize() {
        zoneCombo.setDisable(true);
        stallIdField.setDisable(true);
        for (char c = 'A'; c <= 'G'; c++) {
            zoneCombo.getItems().add(String.valueOf(c));
        }
    }

    /* ===== รับข้อมูลจากหน้าก่อน ===== */
    public void setStallData(Stall stall) {
        this.originalStall = stall;

        zoneCombo.setValue(stall.getZoneName());
        stallIdField.setText(stall.getStallId());
        sizeField.setText(stall.getSize());
        priceField.setText(String.valueOf(stall.getDailyRate()));
       

        switch (stall.getStatus()) {
            case "available" -> statusAvailable.setSelected(true);
            case "rented" -> statusRented.setSelected(true);
            case "maintenance" -> statusMaintenance.setSelected(true);
            case "processing" -> statusProcessing.setSelected(true);
        }
    }

    @FXML
    private void handleSave() {
        try {
            double price = Double.parseDouble(priceField.getText());
            String status = getSelectedStatus();
            
            if (status == null) {
                showAlert("กรุณาเลือกสถานะพื้นที่");
            return;
            }
            SupabaseClient supabase = new SupabaseClient();
            
            String jsonBody = String.format("""
                {
                  "zone_name": "%s",
                  "size": "%s",
                  "daily_rate": %f,
                  "status": "%s"
                  
                }
                """,
                zoneCombo.getValue(),
                sizeField.getText(),
                price,
                status

            );
            
            supabase.update(
                "stalls",
                "stall_id",
                originalStall.getStallId(),
                jsonBody
            );
            
            Stage stage = (Stage) stallIdField.getScene().getWindow();
            SceneManager.switchScene(stage, "/views/space_management.fxml");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("บันทึกไม่สำเร็จ");
        }
    }

    private String getSelectedStatus() {
    RadioButton selected = (RadioButton) statusGroup.getSelectedToggle();
    if (selected == null) return null;

    if (selected == statusAvailable) return "available";
    if (selected == statusRented) return "rented";
    if (selected == statusMaintenance) return "maintenance";
    if (selected == statusProcessing) return "processing";
    return null;
    }


    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
