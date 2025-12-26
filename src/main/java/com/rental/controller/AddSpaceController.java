package com.rental.controller;

import com.rental.database.SupabaseClient;
import com.rental.util.SceneManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class AddSpaceController {

    @FXML private ComboBox<String> zoneCombo;
    @FXML private TextField stallIdField;
    @FXML private TextField sizeField;
    @FXML private TextField priceField;
    @FXML private TextArea amenitiesField;

    private final SupabaseClient supabase = new SupabaseClient();

    @FXML
    private void initialize() {
        zoneCombo.getItems().clear();
        zoneCombo.getItems().addAll("A", "B", "C", "D", "E", "F", "G");
    }

    @FXML
private void handleAddSpace(ActionEvent event) {

    String zone = zoneCombo.getValue();
    String stallId = stallIdField.getText();
    String size = sizeField.getText();
    String price = priceField.getText();

        if (zone == null || stallId.isBlank() || size.isBlank() || price.isBlank()) {
            showAlert("กรุณากรอกข้อมูลให้ครบ");
            return;
        }

        double priceValue;
        try {
            priceValue = Double.parseDouble(price);
        } catch (NumberFormatException e) {
            showAlert("กรุณากรอกราคาเป็นตัวเลข");
            return;
        }

        try {
            String jsonBody = String.format("""
            {
                "zone_name": "%s",
                "stall_id": "%s",
                "size": "%s",
                "daily_rate": %s,
                "status": "available"
            }
            """, zone, stallId, size, priceValue);

            supabase.insert("stalls", jsonBody);

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene().getWindow();
            SceneManager.switchScene(stage, "/views/space_management.fxml");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("เพิ่มพื้นที่ไม่สำเร็จ (อาจมีรหัสซ้ำ)");
        }
    }
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("แจ้งเตือน");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
}
