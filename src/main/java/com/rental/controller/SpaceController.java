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
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private ComboBox<String> typeCombo;
    @FXML private DatePicker rentDate;

    @FXML private ToggleGroup zoneGroup;
    @FXML private GridPane spaceGrid;

    /* ===== เพิ่มตามที่ขอ ===== */
    @FXML private CheckBox onlyAvailableCheck;
    @FXML private Label noResultLabel;
    /* ========================= */

    private char currentZone = 'A';
    private String highlightStallId = null;

    private static final String SUPABASE_URL =
            "https://sdmipxsxkquuyxvvqpho.supabase.co";

    private static final String SUPABASE_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNkbWlweHN4a3F1dXl4dnZxcGhvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQ3NTM1NDcsImV4cCI6MjA4MDMyOTU0N30.AG8XwFmTuMPXZe5bjv2YqeIcfvKFRf95CJLDhfDHp0E";

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
            zoneGroup.selectedToggleProperty().addListener((obs, o, n) -> {
                if (n != null) {
                    ToggleButton btn = (ToggleButton) n;
                    currentZone = btn.getText().charAt(btn.getText().length() - 1);
                    highlightStallId = null;
                    loadFromSupabase();
                }
            });
        }

        if (noResultLabel != null) {
            noResultLabel.setVisible(false);
        }

        loadFromSupabase();
    }

    @FXML
    private void handleSearch() {

        String text = searchField.getText();

        if (text != null && !text.isBlank()) {

            text = text.trim().toUpperCase();

            if (text.startsWith("โซน")) {
                text = text.replace("โซน", "").trim();
            }

            currentZone = text.charAt(0);

            if (text.length() > 1) {
                highlightStallId = text;
            } else {
                highlightStallId = null;
            }

            for (Toggle t : zoneGroup.getToggles()) {
                ToggleButton b = (ToggleButton) t;
                if (b.getText().endsWith(String.valueOf(currentZone))) {
                    b.setSelected(true);
                    break;
                }
            }
        }

        loadFromSupabase();
    }

    @FXML
    private void handleClearFilter() {
        searchField.clear();
        statusCombo.setValue(null);
        typeCombo.setValue(null);
        rentDate.setValue(null);
        highlightStallId = null;

        if (onlyAvailableCheck != null) {
            onlyAvailableCheck.setSelected(false);
        }

        loadFromSupabase();
    }

    private void loadFromSupabase() {

        spaceGrid.getChildren().clear();

        if (noResultLabel != null) {
            noResultLabel.setVisible(false);
        }

        try {

            String dbStatus = mapStatusToDb(statusCombo.getValue());

            String url = SUPABASE_URL +
                    "/rest/v1/stalls?select=stall_id,size,status" +
                    "&zone_name=eq." + currentZone;

            if (highlightStallId != null) {
                url += "&stall_id=eq." + highlightStallId;
            }

            if (onlyAvailableCheck != null && onlyAvailableCheck.isSelected()) {
                url += "&status=eq.available";
            } else if (dbStatus != null) {
                url += "&status=eq." + dbStatus;
            }

            url += "&order=stall_id.asc";

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

            if (arr.size() == 0) {
                if (noResultLabel != null) {
                    noResultLabel.setVisible(true);
                }
                return;
            }

            int index = 0;
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();

                String stallId = o.get("stall_id").getAsString();
                String size = o.get("size").isJsonNull() ? "-" : o.get("size").getAsString();
                String status = o.get("status").isJsonNull() ? null : o.get("status").getAsString();

                VBox box = createBox(stallId, size, status);
                spaceGrid.add(box, index % 5, index / 5);

                if (highlightStallId != null &&
                        stallId.equalsIgnoreCase(highlightStallId)) {

                    Platform.runLater(() -> {
                        box.setStyle(
                                box.getStyle() +
                                "; -fx-border-color:black; -fx-border-width:3;"
                        );
                    });
                }

                index++;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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

    private String mapStatusToDb(String thaiStatus) {
        if (thaiStatus == null) return null;
        return switch (thaiStatus) {
            case "ว่าง" -> "available";
            case "ถูกเช่า" -> "rented";
            case "กำลังดำเนินการ" -> "processing";
            case "ปิดปรับปรุง" -> "maintenance";
            default -> null;
        };
    }

    @FXML
    private void handleShowMap() {

        ImageView iv = new ImageView(
                new Image(getClass().getResource("/images/Market.png").toExternalForm())
        );
        iv.setFitWidth(900);
        iv.setPreserveRatio(true);

        Stage st = new Stage();
        st.initModality(Modality.APPLICATION_MODAL);
        st.setScene(new Scene(new BorderPane(iv)));
        st.showAndWait();
    }

    @FXML
    private void handleSpaceClick(MouseEvent event) {

        VBox box = (VBox) event.getSource();
        Label idLabel = (Label) box.getChildren().get(0);
        Label sizeLabel = (Label) box.getChildren().get(1);

        String stallId = idLabel.getText();
        String size = sizeLabel.getText();

        String style = box.getStyle();
        String statusText = "ปิดปรับปรุง";
        String statusColor = "#6c757d";

        if (style.contains("#2e8b61")) {
            statusText = "ว่าง";
            statusColor = "#2e8b61";
        } else if (style.contains("#982d2d")) {
            statusText = "ถูกเช่า";
            statusColor = "#982d2d";
        } else if (style.contains("#bac04d")) {
            statusText = "กำลังดำเนินการ";
            statusColor = "#bac04d";
        }

        Label title = new Label(stallId);
        title.setFont(Font.font("System", FontWeight.BOLD, 32));

        Label status = new Label(statusText);
        status.setStyle(
                "-fx-background-color:" + statusColor + ";" +
                "-fx-text-fill:white;" +
                "-fx-padding:4 12;" +
                "-fx-background-radius:12;" +
                "-fx-font-weight:bold;"
        );

        HBox header = new HBox(16, title, status);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox detail = new VBox(16,
                createDetailRow("iconzone.png", "โซน", "โซน " + stallId.charAt(0)),
                createDetailRow("iconarea.png", "ขนาดพื้นที่", size + " เมตร"),
                createDetailRow("iconprice.png", "ราคาค่าเช่า", "300 บาท/วัน"),
                createDetailRow("iconproduct.png", "ประเภทสินค้า", "-"),
                createDetailRow("icondate.png", "วันที่เช่า", "-")
        );

        Button btnClose = new Button("ปิด");
        Button btnReserve = new Button("จอง");

        btnReserve.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/booking.fxml"));
                Parent root = loader.load();
                BookingController controller = loader.getController();
                controller.setStallData(stallId);

                Stage stage = (Stage) btnReserve.getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.centerOnScreen();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        HBox buttonBar = new HBox(20, btnClose, btnReserve);
        buttonBar.setAlignment(Pos.CENTER);

        VBox root = new VBox(24, header, detail, buttonBar);
        root.setPadding(new Insets(24));

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setScene(new Scene(root));

        btnClose.setOnAction(e -> dialog.close());

        if (!statusText.equals("ว่าง")) {
            btnReserve.setDisable(true);
            btnReserve.setOpacity(0.5);
        }

        dialog.showAndWait();
    }

    private HBox createDetailRow(String iconFile, String title, String value) {

        ImageView icon = new ImageView(
                new Image(getClass().getResource("/images/" + iconFile).toExternalForm())
        );
        icon.setFitWidth(26);
        icon.setFitHeight(26);
        icon.setPreserveRatio(true);

        Label t = new Label(title);
        Label v = new Label(value);

        VBox text = new VBox(2, t, v);
        HBox row = new HBox(14, icon, text);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
