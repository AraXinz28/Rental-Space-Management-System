package com.rental.controller;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ResourceBundle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SpaceController implements Initializable {

    // ================= FILTER =================
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private DatePicker rentDate;

    // ================= ZONE =================
    @FXML private ToggleGroup zoneGroup;

    // ================= GRID =================
    @FXML private GridPane spaceGrid;

    private char currentZone = 'A';
    private String highlightStallId = null;

    // ================= SUPABASE =================
    private static final String SUPABASE_URL =
            "https://sdmipxsxkquuyxvvqpho.supabase.co";

    private static final String SUPABASE_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNkbWlweHN4a3F1dXl4dnZxcGhvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQ3NTM1NDcsImV4cCI6MjA4MDMyOTU0N30.AG8XwFmTuMPXZe5bjv2YqeIcfvKFRf95CJLDhfDHp0E";

    // =========================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        statusCombo.getItems().setAll(
                "ว่าง", "ถูกเช่า", "กำลังดำเนินการ", "ปิดปรับปรุง"
        );

        typeCombo.getItems().setAll(
                "1) อาหาร / เครื่องดื่ม",
                "2) แฟชั่น / เสื้อผ้า",
                "3) เครื่องประดับ / กระเป๋า / รองเท้า",
                "4) ของใช้ในบ้าน / ของตกแต่ง",
                "5) เบ็ดเตล็ด / สินค้าทั่วไป",
                "6) ของสด / ผัก / ผลไม้",
                "7) ความงาม / สกินแคร์",
                "8) ของเล่น / โมเดล",
                "9) งานแฮนด์เมด / งานคราฟต์",
                "10) สินค้าสัตว์เลี้ยง"
        );

        if (zoneGroup != null) {
            zoneGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
                if (newT != null) {
                    ToggleButton btn = (ToggleButton) newT;
                    currentZone = btn.getText().charAt(btn.getText().length() - 1);
                    highlightStallId = null;
                    loadFromSupabase();
                }
            });
        }

        loadFromSupabase();
    }

    // ================= SEARCH (กดปุ่มเท่านั้น) =================
    @FXML
    private void handleSearch() {

        String text = searchField.getText();
        if (text == null || text.isBlank()) {
            return;
        }

        text = text.trim().toUpperCase();

        // รองรับ "โซนA3" / "โซน A3"
        if (text.startsWith("โซน")) {
            text = text.replace("โซน", "").trim();
        }

        if (text.length() == 0) return;

        char zone = text.charAt(0);
        currentZone = zone;

        if (text.length() > 1) {
            highlightStallId = text;
        } else {
            highlightStallId = null;
        }

        // เปลี่ยน Toggle โซน
        for (Toggle t : zoneGroup.getToggles()) {
            ToggleButton b = (ToggleButton) t;
            if (b.getText().endsWith(String.valueOf(zone))) {
                b.setSelected(true);
                break;
            }
        }

        loadFromSupabase();
    }

    // ================= CLEAR =================
    @FXML
    private void handleClearFilter() {
        searchField.clear();
        statusCombo.setValue(null);
        typeCombo.setValue(null);
        rentDate.setValue(null);
        highlightStallId = null;
        loadFromSupabase();
    }

    // ================= LOAD DATA =================
    private void loadFromSupabase() {

        spaceGrid.getChildren().clear();

        try {
            String url = SUPABASE_URL +
                    "/rest/v1/stalls?select=stall_id,size,status" +
                    "&zone_name=eq." + currentZone +
                    "&order=stall_id.asc";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + SUPABASE_KEY)
                    .GET()
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(
                            request, HttpResponse.BodyHandlers.ofString()
                    );

            JsonArray arr = JsonParser.parseString(response.body()).getAsJsonArray();

            int index = 0;
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();

                String stallId = o.get("stall_id").getAsString();
                String size = o.get("size").isJsonNull()
                        ? "-" : o.get("size").getAsString();
                String status = o.get("status").isJsonNull()
                        ? null : o.get("status").getAsString();

                VBox box = createBox(stallId, size, status);
                spaceGrid.add(box, index % 5, index / 5);

                if (highlightStallId != null &&
                        stallId.equalsIgnoreCase(highlightStallId)) {

                    Platform.runLater(() -> {
                        box.setStyle(
                                box.getStyle() +
                                "; -fx-border-color:black; -fx-border-width:3;"
                        );
                        box.requestFocus();
                    });
                }

                index++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= CREATE BOX =================
    private VBox createBox(String id, String size, String status) {

        Label idLabel = new Label(id);
        idLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        Label sizeLabel = new Label(size);

        VBox box = new VBox(6, idLabel, sizeLabel);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(18));
        box.setMaxWidth(Double.MAX_VALUE);

        box.setStyle(
                "-fx-background-radius:14;" +
                "-fx-background-color:" + colorByStatus(status) + ";"
        );

        box.setOnMouseClicked(this::handleSpaceClick);
        return box;
    }

    private String colorByStatus(String s) {
        if (s == null) return "#6c757d";
        return switch (s) {
            case "available" -> "#2e8b61ff";
            case "rented" -> "#982d2dff";
            case "processing" -> "#bac04dff";
            default -> "#6c757d";
        };
    }

    // ================= SHOW MAP =================
    @FXML
    private void handleShowMap() {

        ImageView iv = new ImageView(
                new Image(
                        getClass().getResource("/images/Market.png").toExternalForm()
                )
        );
        iv.setPreserveRatio(true);
        iv.setFitWidth(900);

        Stage st = new Stage();
        st.initModality(Modality.APPLICATION_MODAL);
        st.setTitle("ผังพื้นที่ทั้งหมด");
        st.setScene(new Scene(new BorderPane(iv)));
        st.showAndWait();
    }

    // ================= CLICK SPACE =================
    @FXML
    private void handleSpaceClick(MouseEvent event) {
        VBox box = (VBox) event.getSource();
        Label id = (Label) box.getChildren().get(0);
        System.out.println("คลิกแผง " + id.getText());
    }
}
