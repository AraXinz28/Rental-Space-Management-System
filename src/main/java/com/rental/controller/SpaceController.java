package com.rental.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SpaceController implements Initializable {

    @FXML
    private ComboBox<String> zoneCombo;

    @FXML
    private ComboBox<String> statusCombo;

    @FXML
    private ComboBox<String> typeCombo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // ------- รายการโซน -------
        if (zoneCombo != null) {
            zoneCombo.getItems().setAll(
                    "โซน A", "โซน B", "โซน C",
                    "โซน D", "โซน E", "โซน F", "โซน G"
            );
        }

        // ------- รายการสถานะ -------
        if (statusCombo != null) {
            statusCombo.getItems().setAll(
                    "ว่าง", "ถูกเช่า", "กำลังดำเนินการ", "ปิดปรับปรุง"
            );
        }

        // ------- ประเภทสินค้า -------
        if (typeCombo != null) {
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
        }
    }

    // ================== ปุ่ม "คลิกที่นี่เพื่อดูผังพื้นที่" ==================
    @FXML
    private void handleShowMap() {

        Image image = new Image(
                getClass().getResource("/images/Market.png").toExternalForm()
        );
        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(900);

        BorderPane root = new BorderPane();
        root.setCenter(imageView);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("ผังพื้นที่ทั้งหมด");
        dialog.setScene(new Scene(root));
        dialog.sizeToScene();

        dialog.showAndWait();
    }

    // ================== เมธอดช่วยสร้างแถวรายละเอียดพร้อมไอคอน ==================
    private HBox createDetailRow(String iconFileName, String titleText, String valueText) {

        ImageView icon = new ImageView(
                new Image(getClass().getResource("/images/" + iconFileName).toExternalForm())
        );
        icon.setFitWidth(28);
        icon.setFitHeight(28);
        icon.setPreserveRatio(true);

        Label titleLabel = new Label(titleText);
        titleLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 13;");

        Label valueLabel = new Label(valueText);
        valueLabel.setStyle("-fx-text-fill: #000000; -fx-font-size: 14; -fx-font-weight: bold;");

        VBox textBox = new VBox(2, titleLabel, valueLabel);

        HBox row = new HBox(12, icon, textBox);
        row.setAlignment(Pos.CENTER_LEFT);

        return row;
    }

    // ================== เมื่อคลิกสี่เหลี่ยมพื้นที่ A01–A15 ==================
    @FXML
    private void handleSpaceClick(MouseEvent event) {

        VBox box = (VBox) event.getSource();

        String spaceName = "";
        String sizeText = "";

        if (box.getChildren().size() >= 2) {
            Label nameLabel = (Label) box.getChildren().get(0);
            Label sizeLabel = (Label) box.getChildren().get(1);
            spaceName = nameLabel.getText();
            sizeText = sizeLabel.getText();
        }

        String zone = "";
        if (!spaceName.isEmpty()) {
            zone = "โซน " + spaceName.substring(0, 1);
        }

        String style = box.getStyle();
        String statusText = "ไม่ทราบสถานะ";
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
        } else if (style.contains("#6c757d")) {
            statusText = "ปิดปรับปรุง";
            statusColor = "#6c757d";
        }

        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.TOP_LEFT);

        Label titleLabel = new Label(spaceName);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 32));

        Label statusLabel = new Label(statusText);
        statusLabel.setStyle(
                "-fx-background-color: " + statusColor + ";" +
                "-fx-text-fill: white;" +
                "-fx-padding: 4 12;" +
                "-fx-background-radius: 12;" +
                "-fx-font-weight: bold;"
        );

        HBox header = new HBox(16, titleLabel, statusLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        HBox zoneRow  = createDetailRow("iconzone.png", "โซน", zone);
        HBox sizeRow  = createDetailRow("iconarea.png", "ขนาดพื้นที่", sizeText + " เมตร");
        HBox priceRow = createDetailRow("iconprice.png", "ราคาค่าเช่า", "150 บาท/วัน");
        HBox typeRow  = createDetailRow("iconproduct.png", "ประเภทการสินค้า", "-");
        HBox dateRow  = createDetailRow("icondate.png", "วันที่เช่า", "-");

        VBox detailBox = new VBox(16, zoneRow, sizeRow, priceRow, typeRow, dateRow);

        Button closeBtn = new Button("ปิด");
        closeBtn.setPrefWidth(100);

        Button bookBtn = new Button("จอง");
        bookBtn.setPrefWidth(100);
        bookBtn.setStyle(
                "-fx-background-color: #274390ff;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;"
        );

        HBox buttonBar = new HBox(12 , closeBtn, bookBtn);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        root.getChildren().addAll(header, detailBox, buttonBar);

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("รายละเอียดพื้นที่");
        dialog.setScene(new Scene(root));
        dialog.sizeToScene();  // สำคัญมาก: ทำให้หน้าต่างขยายพอดีกับเนื้อหา

        closeBtn.setOnAction(e -> dialog.close());
        bookBtn.setOnAction(e -> {
            // ใส่โค้ดการจองในอนาคตตรงนี้
            dialog.close();
        });

        dialog.showAndWait();
    }
}