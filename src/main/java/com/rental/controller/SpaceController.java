package com.rental.controller;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
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
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SpaceController implements Initializable {

    /* ================= FXML ================= */
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private DatePicker rentDate;

    @FXML private ToggleGroup zoneGroup;
    @FXML private GridPane spaceGrid;

    @FXML private CheckBox onlyAvailableCheck;
    @FXML private Label noResultLabel;

    /* ================= STATE ================= */
    private char currentZone = 'A';
    private String highlightStallId = null;

    private static final String SUPABASE_URL =
            "https://sdmipxsxkquuyxvvqpho.supabase.co";

    private static final String SUPABASE_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNkbWlweHN4a3F1dXl4dnZxcGhvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQ3NTM1NDcsImV4cCI6MjA4MDMyOTU0N30.AG8XwFmTuMPXZe5bjv2YqeIcfvKFRf95CJLDhfDHp0E";

    /* ================= INIT ================= */
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        statusCombo.getItems().setAll(
                "ว่าง", "ถูกเช่า", "กำลังดำเนินการ", "ปิดปรับปรุง"
        );

        zoneGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n != null) {
                ToggleButton btn = (ToggleButton) n;
                currentZone = btn.getText().charAt(btn.getText().length() - 1);
                highlightStallId = null;
                runLoad();
            }
        });

        noResultLabel.setVisible(false);
        runLoad();
    }

    /* ================= THREAD ================= */
    private void runLoad() {
        new Thread(this::loadFromSupabase).start();
    }

    /* ================= SEARCH ================= */
    @FXML
    private void handleSearch() {

        highlightStallId = null;
        String text = searchField.getText();

        if (text != null && !text.isBlank()) {
            text = text.trim().toUpperCase();

            if (text.startsWith("โซน")) {
                text = text.replace("โซน", "").trim();
            }

            if (text.length() >= 1 && Character.isLetter(text.charAt(0))) {
                currentZone = text.charAt(0);
                if (text.length() > 1) {
                    highlightStallId = text;
                }
            }
        }

        runLoad();
    }

    @FXML
    private void handleClearFilter() {
        Platform.runLater(() -> {
            searchField.clear();
            statusCombo.setValue(null);
            rentDate.setValue(null);
            onlyAvailableCheck.setSelected(false);
            highlightStallId = null;
        });
        runLoad();
    }

    /* ================= DATA ================= */
    private void loadFromSupabase() {

        Platform.runLater(() -> {
            spaceGrid.getChildren().clear();
            noResultLabel.setVisible(false);
        });

        try {
            Map<String, String> paymentStatusMap = loadPaymentStatusByStall();

            String url = SUPABASE_URL +
                    "/rest/v1/stalls?select=stall_id,size,status" +
                    "&zone_name=eq." + currentZone;

            if (highlightStallId != null) {
                url += "&stall_id=eq." + highlightStallId;
            }

            if (onlyAvailableCheck.isSelected()) {
                url += "&status=eq.available";
            }

            url += "&order=stall_id.asc";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + SUPABASE_KEY)
                    .GET()
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            JsonArray arr = JsonParser.parseString(response.body()).getAsJsonArray();

            if (arr.isEmpty()) {
                Platform.runLater(() -> noResultLabel.setVisible(true));
                return;
            }

            int index = 0;
            for (JsonElement el : arr) {
                JsonObject o = el.getAsJsonObject();

                String stallId = o.get("stall_id").getAsString();
                String size = o.get("size").isJsonNull() ? "-" : o.get("size").getAsString();

                String finalStatus;
                if (onlyAvailableCheck.isSelected()) {
                    finalStatus = "available";
                } else {
                    finalStatus = paymentStatusMap.containsKey(stallId)
                            ? paymentStatusMap.get(stallId)
                            : (o.get("status").isJsonNull() ? null : o.get("status").getAsString());
                }

                VBox box = createBox(stallId, size, finalStatus);

                int col = index % 5;
                int row = index / 5;
                index++;

                Platform.runLater(() -> spaceGrid.add(box, col, row));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* ================= PAYMENT STATUS ================= */
    private Map<String, String> loadPaymentStatusByStall() throws Exception {

        Map<String, String> map = new HashMap<>();

        String url = SUPABASE_URL +
                "/rest/v1/payments?select=status,booking:bookings(stall_id)" +
                "&order=created_at.desc";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("apikey", SUPABASE_KEY)
                .header("Authorization", "Bearer " + SUPABASE_KEY)
                .GET()
                .build();

        HttpResponse<String> response =
                HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        JsonArray arr = JsonParser.parseString(response.body()).getAsJsonArray();

        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            if (o.get("booking").isJsonNull()) continue;

            String stallId = o.getAsJsonObject("booking").get("stall_id").getAsString();
            String paymentStatus = o.get("status").getAsString();

            if (!map.containsKey(stallId)) {
                map.put(stallId, mapPaymentToStallStatus(paymentStatus));
            }
        }
        return map;
    }

    private String mapPaymentToStallStatus(String paymentStatus) {
        if (paymentStatus == null) return null;

        switch (paymentStatus) {
            case "approved": return "rented";
            case "rejected": return "available";
            case "pending": return "processing";
            default: return null;
        }
    }

    /* ================= BOOKING INFO ================= */
    private Map<String, String> loadBookingInfoByStall(String stallId) {

        try {
            String url = SUPABASE_URL +
                    "/rest/v1/bookings?select=product_type,start_date,end_date" +
                    "&stall_id=eq." + stallId +
                    "&order=created_at.desc&limit=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("apikey", SUPABASE_KEY)
                    .header("Authorization", "Bearer " + SUPABASE_KEY)
                    .GET()
                    .build();

            HttpResponse<String> response =
                    HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            JsonArray arr = JsonParser.parseString(response.body()).getAsJsonArray();

            if (arr.isEmpty()) return null;

            JsonObject o = arr.get(0).getAsJsonObject();

            Map<String, String> result = new HashMap<>();
            result.put("product_type",
                    o.get("product_type").isJsonNull() ? "-" : o.get("product_type").getAsString());

            result.put("date_range",
                    (!o.get("start_date").isJsonNull() && !o.get("end_date").isJsonNull())
                            ? o.get("start_date").getAsString() + " - " + o.get("end_date").getAsString()
                            : "-");

            return result;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /* ================= UI ================= */
    private VBox createBox(String id, String size, String status) {

        Label idLabel = new Label(id);
        idLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        Label sizeLabel = new Label(size);

        VBox box = new VBox(6, idLabel, sizeLabel);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(18));
        box.setStyle(
                "-fx-background-radius:14;" +
                "-fx-background-color:" + colorByStatus(status) + ";"
        );

        box.setOnMouseClicked(e -> handleSpaceClick(e, status));
        return box;
    }

    private String colorByStatus(String s) {
        switch (s) {
            case "available": return "#2e8b61";
            case "rented": return "#982d2d";
            case "processing": return "#bac04d";
            case "maintenance": return "#6c757d";
        }
        return "#6c757d";
    }

    /* ================= MAP ================= */
    @FXML
    private void handleShowMap() {
        ImageView iv = new ImageView(
                new Image(getClass().getResource("/images/Market.png").toExternalForm())
        );
        iv.setFitWidth(900);
        iv.setPreserveRatio(true);

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(new Scene(new VBox(iv), 920, 600));
        stage.setTitle("ผังพื้นที่ตลาด");
        stage.showAndWait();
    }

    /* ================= DETAIL ================= */
    private void handleSpaceClick(MouseEvent event, String statusDb) {

        VBox box = (VBox) event.getSource();
        Label idLabel = (Label) box.getChildren().get(0);
        Label sizeLabel = (Label) box.getChildren().get(1);

        String stallId = idLabel.getText();
        String size = sizeLabel.getText();

        String statusText =
                "rented".equals(statusDb) ? "ถูกเช่า" :
                "processing".equals(statusDb) ? "กำลังดำเนินการ" :
                "maintenance".equals(statusDb) ? "ปิดปรับปรุง" : "ว่าง";

        String productType = "-";
        String dateRange = "-";

        Map<String, String> bookingInfo = loadBookingInfoByStall(stallId);
        if (bookingInfo != null) {
            productType = bookingInfo.get("product_type");
            dateRange = bookingInfo.get("date_range");
        }

        Label title = new Label(stallId);
        title.setFont(Font.font("System", FontWeight.BOLD, 32));

        Label status = new Label(statusText);
        status.setStyle(
                "-fx-background-color:" + colorByStatus(statusDb) + ";" +
                "-fx-text-fill:white;" +
                "-fx-padding:4 12;" +
                "-fx-background-radius:12;"
        );

        VBox detail = new VBox(18,
                createDetailRow("iconzone.png", "โซน", "โซน " + stallId.charAt(0)),
                createDetailRow("iconarea.png", "ขนาดพื้นที่", size + " เมตร"),
                createDetailRow("iconprice.png", "ราคาค่าเช่า", "300 บาท/วัน"),
                createDetailRow("iconproduct.png", "ประเภทสินค้า", productType),
                createDetailRow("icondate.png", "วันที่เช่า", dateRange)
        );
        detail.setAlignment(Pos.CENTER);

        Button btnClose = new Button("ปิด");
        Button btnReserve = new Button("จอง");

        btnClose.setPrefWidth(120);
        btnReserve.setPrefWidth(120);

        btnClose.setStyle("-fx-background-color:#e0e0e0; -fx-background-radius:12;");
        btnReserve.setStyle("-fx-background-color:#3498db; -fx-text-fill:white; -fx-background-radius:12;");

        if (!"available".equals(statusDb)) {
            btnReserve.setDisable(true);
            btnReserve.setOpacity(0.5);
        }

        btnReserve.setOnAction(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/views/booking.fxml")
                );
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

        VBox root = new VBox(26, title, status, detail, buttonBar);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(28));

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setScene(new Scene(root, 360, 540));

        btnClose.setOnAction(e -> dialog.close());
        dialog.showAndWait();
    }

    private HBox createDetailRow(String iconFile, String title, String value) {

        ImageView icon = new ImageView(
                new Image(getClass().getResource("/images/" + iconFile).toExternalForm())
        );
        icon.setFitWidth(26);
        icon.setPreserveRatio(true);

        Label t = new Label(title);
        t.setStyle("-fx-font-weight:bold;");
        Label v = new Label(value);

        VBox text = new VBox(2, t, v);
        HBox row = new HBox(16, icon, text);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
