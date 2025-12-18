package com.rental.controller;

import com.rental.model.RentalHistoryRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RentalHistoryAdminController {

    @FXML
    private TextField txtCustomer;
    @FXML
    private ComboBox<String> cbStatus;
    @FXML
    private DatePicker dpDate;

    @FXML
    private Button btnPrint;

    @FXML
    private TableView<RentalHistoryRow> table;
    @FXML
    private TableColumn<RentalHistoryRow, String> colCustomer;
    @FXML
    private TableColumn<RentalHistoryRow, LocalDate> colDate;
    @FXML
    private TableColumn<RentalHistoryRow, String> colStatus;

    // ✅ สำคัญ: Void (ไม่ใช้ Button generic)
    @FXML
    private TableColumn<RentalHistoryRow, Void> colDetail;

    private final ObservableList<RentalHistoryRow> masterData = FXCollections.observableArrayList();
    private FilteredList<RentalHistoryRow> filtered;

    private static final DateTimeFormatter TH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {

        // ===== status combo =====
        cbStatus.setItems(FXCollections.observableArrayList("ทั้งหมด", "เสร็จสิ้น", "ยกเลิก"));
        cbStatus.getSelectionModel().selectFirst();

        // ===== date default (เอาออกได้) =====
        dpDate.setValue(LocalDate.of(2025, 11, 20));

        // ===== columns =====
        colCustomer.setCellValueFactory(d -> d.getValue().customerNameProperty());

        colDate.setCellValueFactory(d -> d.getValue().dateProperty());
        colDate.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : TH_DATE.format(item));
                setStyle("-fx-alignment: CENTER;");
            }
        });

        colStatus.setCellValueFactory(d -> d.getValue().statusProperty());
        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }

                Label badge = new Label(item);
                badge.setStyle("""
                        -fx-text-fill: white;
                        -fx-padding: 6 16;
                        -fx-background-radius: 999;
                        -fx-font-weight: bold;
                        """);

                if ("เสร็จสิ้น".equals(item)) {
                    badge.setStyle(badge.getStyle() + "-fx-background-color: #4F6F4A;");
                } else if ("ยกเลิก".equals(item)) {
                    badge.setStyle(badge.getStyle() + "-fx-background-color: #6B3F3F;");
                } else {
                    badge.setStyle(badge.getStyle() + "-fx-background-color: #777;");
                }

                setGraphic(badge);
                setText(null);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // ===== ✅ detail button (วิธีที่ถูกต้องสุด) =====
        colDetail.setCellFactory(tc -> new TableCell<>() {
            private final Button btn = new Button("ดูรายละเอียด");

            {
                btn.setStyle("""
                        -fx-background-color: #40446A;
                        -fx-text-fill: white;
                        -fx-background-radius: 999;
                        -fx-padding: 8 16;
                        """);

                btn.setOnAction(e -> {
                    RentalHistoryRow row = getTableView().getItems().get(getIndex());
                    openDetail(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setText(null);
                setStyle("-fx-alignment: CENTER;");
            }
        });

        // ===== sample data =====
        masterData.addAll(
                new RentalHistoryRow("นายสมชาย ใจดี", LocalDate.of(2025, 11, 20), "เสร็จสิ้น"),
                new RentalHistoryRow("นางอัมพร ลองใจ", LocalDate.of(2025, 11, 20), "เสร็จสิ้น"),
                new RentalHistoryRow("นางสาวร้อนตัว เย็นใจ", LocalDate.of(2025, 11, 20), "เสร็จสิ้น"),
                new RentalHistoryRow("นายสิงหา ราสัตว์", LocalDate.of(2025, 11, 20), "ยกเลิก"),
                new RentalHistoryRow("นายก๊อ คั่วพริกเกลือ", LocalDate.of(2025, 11, 20), "เสร็จสิ้น"));

        filtered = new FilteredList<>(masterData, p -> true);
        table.setItems(filtered);

        // ===== filter listeners =====
        txtCustomer.textProperty().addListener((obs, o, n) -> applyFilter());
        cbStatus.valueProperty().addListener((obs, o, n) -> applyFilter());
        dpDate.valueProperty().addListener((obs, o, n) -> applyFilter());

        applyFilter();
    }

    // ✅ เปิดหน้ารายละเอียด + ใช้ Scene เดิม (ฟอนต์/CSS ไม่หาย)
    private void openDetail(RentalHistoryRow row) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/rentalhistorydetail.fxml"));
            Parent root = loader.load();

            RentalHistoryDetailController c = loader.getController();
            c.setData(row);

            Stage stage = (Stage) table.getScene().getWindow();
            stage.getScene().setRoot(root);

        } catch (Exception ex) {
            ex.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("เปิดหน้ารายละเอียดไม่สำเร็จ");
            a.setHeaderText("โหลด rentalhistorydetail.fxml ไม่ได้");
            a.setContentText(String.valueOf(ex));
            a.showAndWait();
        }
    }

    private void applyFilter() {
        String name = txtCustomer.getText() == null ? "" : txtCustomer.getText().trim().toLowerCase();
        String status = cbStatus.getValue();
        LocalDate date = dpDate.getValue();

        filtered.setPredicate(row -> {
            boolean okName = name.isEmpty() || row.getCustomerName().toLowerCase().contains(name);
            boolean okStatus = (status == null || "ทั้งหมด".equals(status)) || status.equals(row.getStatus());
            boolean okDate = (date == null) || date.equals(row.getDate());
            return okName && okStatus && okDate;
        });
    }

    @FXML
    private void onPrint(ActionEvent event) {
        try {
            var chooser = new javafx.stage.FileChooser();
            chooser.setTitle("บันทึกรายงานเป็น PDF");
            chooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
            chooser.setInitialFileName("รายงานประวัติการเช่า.pdf");

            java.io.File out = chooser.showSaveDialog(table.getScene().getWindow());
            if (out == null)
                return;

            // ✅ บังคับนามสกุล .pdf
            if (!out.getName().toLowerCase().endsWith(".pdf")) {
                out = new java.io.File(out.getAbsolutePath() + ".pdf");
            }

            // ✅ export
            java.util.List<com.rental.model.RentalHistoryRow> rows = new java.util.ArrayList<>(table.getItems());

            com.rental.report.RentalHistoryPdfReport.export(
                    out,
                    rows,
                    txtCustomer.getText(),
                    cbStatus.getValue(),
                    dpDate.getValue());

            System.out.println("SAVED PATH = " + out.getAbsolutePath());
            System.out.println("EXISTS=" + out.exists() + " SIZE=" + out.length());

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("สำเร็จ");
            ok.setHeaderText("สร้างไฟล์ PDF เรียบร้อย");
            ok.setContentText(out.getAbsolutePath());
            ok.showAndWait();

            // ✅ เปิดไฟล์ให้เลย (กันหาไม่เจอ)
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(out);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("ผิดพลาด");
            err.setHeaderText("สร้าง PDF ไม่สำเร็จ");
            err.setContentText(String.valueOf(e));
            err.showAndWait();
        }
    }

}
