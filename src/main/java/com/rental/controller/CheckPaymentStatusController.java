package com.rental.controller;

import javafx.collections.*;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.beans.property.*;

public class CheckPaymentStatusController {

    /* ===== FXML ===== */
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbMethod;
    @FXML private DatePicker dpDate;

    @FXML private TableView<PaymentRow> paymentTable;
    @FXML private TableColumn<PaymentRow, String> colShop, colLock, colMethod, colDate;
    @FXML private TableColumn<PaymentRow, Double> colDeposit, colRent, colTotal;

    @FXML private Label lblMethod, lblDeposit, lblRent, lblTotal;
    @FXML private TextArea txtNote;
    @FXML private ImageView slipImage;

    /* ===== DATA ===== */
    private final ObservableList<PaymentRow> masterData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        cbMethod.getItems().addAll("ทั้งหมด", "QR พร้อมเพย์", "โอนผ่านธนาคาร");
        cbMethod.setValue("ทั้งหมด");

        colShop.setCellValueFactory(c -> c.getValue().shopProperty());
        colLock.setCellValueFactory(c -> c.getValue().lockProperty());
        colMethod.setCellValueFactory(c -> c.getValue().methodProperty());
        colDate.setCellValueFactory(c -> c.getValue().dateProperty());

        colDeposit.setCellValueFactory(c -> c.getValue().depositProperty().asObject());
        colRent.setCellValueFactory(c -> c.getValue().rentProperty().asObject());
        colTotal.setCellValueFactory(c -> c.getValue().totalProperty().asObject());

        centerColumn(colShop, colLock, colMethod, colDate,
                     colDeposit, colRent, colTotal);

        mockData();
        paymentTable.setItems(masterData);

        clearDetail();

        
        paymentTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, row) -> showDetail(row)
        );
    }

    private void centerColumn(TableColumn<?, ?>... cols) {
        for (TableColumn<?, ?> c : cols) {
            c.setStyle("-fx-alignment: CENTER;");
        }
    }

    private void mockData() {
        masterData.addAll(
                new PaymentRow("กล้วยหอม จอมซน", "A01", "QR พร้อมเพย์", "2025-12-12"),
                new PaymentRow("หมูหมี่ อีซี่", "A02", "โอนผ่านธนาคาร", "2025-12-13"),
                new PaymentRow("ยุภาภรณ์ เสมอภพ", "B01", "QR พร้อมเพย์", "2025-12-14"),
                new PaymentRow("กล้วย อันบิ๊กใหญ่", "B02", "โอนผ่านธนาคาร", "2025-12-15"),
                new PaymentRow("แอดมิน จ๋า", "C01", "QR พร้อมเพย์", "2025-12-16"),
                new PaymentRow("สมชาย ใจดี", "C02", "โอนผ่านธนาคาร", "2025-12-17")
        );
    }

    /* ===== ACTION ===== */

    @FXML
    private void handleFilter() {
        String keyword = txtSearch.getText().trim();
        if (keyword.isEmpty()) return;

        PaymentRow found = masterData.stream()
                .filter(r -> r.getShop().contains(keyword) || r.getLock().contains(keyword))
                .findFirst()
                .orElse(null);

        if (found != null) {
            masterData.remove(found);
            masterData.add(0, found);
            paymentTable.getSelectionModel().select(0);
        }
    }

    @FXML
    private void handleReset() {
        txtSearch.clear();
        cbMethod.setValue("ทั้งหมด");
        dpDate.setValue(null);
        paymentTable.getSelectionModel().clearSelection();
        clearDetail();
    }

    /* ✅ กดปุ่มดูหลักฐาน = แสดงรายละเอียด */
    @FXML
    private void handleViewProof() {
        PaymentRow row = paymentTable.getSelectionModel().getSelectedItem();
        showDetail(row);
    }

    /* ===== DETAIL ===== */

    private void clearDetail() {
        lblMethod.setText("");
        lblDeposit.setText("");
        lblRent.setText("");
        lblTotal.setText("");
        txtNote.clear();
        slipImage.setImage(null);
    }

    private void showDetail(PaymentRow row) {
        if (row == null) return;

        lblMethod.setText("วิธีชำระเงิน: " + row.getMethod());
        lblDeposit.setText("- ค่ามัดจำ: 0.00 ฿");
        lblRent.setText("- ค่าเช่าทั้งหมด: 150.00 ฿");
        lblTotal.setText("- ยอดชำระทั้งหมด: 150.00 ฿");

        slipImage.setImage(new Image(
                getClass().getResource("/images/slip.png").toExternalForm()
        ));
    }

    @FXML private void handleApprove() {}
    @FXML private void handleReject() {}

    /* ===== MODEL ===== */
    public static class PaymentRow {
        private final StringProperty shop = new SimpleStringProperty();
        private final StringProperty lock = new SimpleStringProperty();
        private final StringProperty method = new SimpleStringProperty();
        private final StringProperty date = new SimpleStringProperty();
        private final DoubleProperty deposit = new SimpleDoubleProperty(0);
        private final DoubleProperty rent = new SimpleDoubleProperty(150);
        private final DoubleProperty total = new SimpleDoubleProperty(150);

        public PaymentRow(String s, String l, String m, String d) {
            shop.set(s);
            lock.set(l);
            method.set(m);
            date.set(d);
        }

        public StringProperty shopProperty() { return shop; }
        public StringProperty lockProperty() { return lock; }
        public StringProperty methodProperty() { return method; }
        public StringProperty dateProperty() { return date; }
        public DoubleProperty depositProperty() { return deposit; }
        public DoubleProperty rentProperty() { return rent; }
        public DoubleProperty totalProperty() { return total; }

        public String getShop() { return shop.get(); }
        public String getLock() { return lock.get(); }
        public String getMethod() { return method.get(); }
    }
}
