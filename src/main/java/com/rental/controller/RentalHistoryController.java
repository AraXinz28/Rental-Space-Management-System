package com.rental.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class RentalHistoryController {

    // ============================
    //  LABELS (User Info)
    // ============================
    @FXML private Label lblName;
    @FXML private Label lblPhone;
    @FXML private Label lblZone;
    @FXML private Label lblLock;
    @FXML private Label lblProduct;
    @FXML private Label lblDate;
    @FXML private Label lblPayment;
    @FXML private Label lblCategory;
    @FXML private Label lblApproval;

    // ============================
    //  BUTTONS
    // ============================
    @FXML private Button btnCancel;
    @FXML private Button btnPrint;

    // ============================
    //  TABLE
    // ============================
    @FXML private TableView<RentalHistory> historyTable;
    @FXML private TableColumn<RentalHistory, String> colItem;
    @FXML private TableColumn<RentalHistory, String> colDate;
    @FXML private TableColumn<RentalHistory, String> colStatus;
    @FXML private TableColumn<RentalHistory, Void> colDetail;

    @FXML
    public void initialize() { 
 // ============================
//  BUTTON HOVER EFFECTS
// ===========================

    // ===== Hover ปุ่มยกเลิก =====
    btnCancel.setOnMouseEntered(e -> btnCancel.setStyle(
            "-fx-background-color: #ff0000ff;" +   // สีเข้มขึ้น
            "-fx-text-fill: white;" +
            "-fx-padding: 8 20;" +
            "-fx-background-radius: 5;" +
            "-fx-scale-x: 1.05;" +
            "-fx-scale-y: 1.05;"
    ));

    btnCancel.setOnMouseExited(e -> btnCancel.setStyle(
            "-fx-background-color: #930000ff;" +   // กลับเป็นสีเดิม
            "-fx-text-fill: white;" +
            "-fx-padding: 8 20;" +
            "-fx-background-radius: 5;" +
            "-fx-scale-x: 1;" +
            "-fx-scale-y: 1;"
    ));

    // ===== Hover ปุ่มปริ้นใบเสร็จ =====
    btnPrint.setOnMouseEntered(e -> btnPrint.setStyle(
            "-fx-background-color: #0073ffff;" +   // สีเข้มขึ้น
            "-fx-text-fill: white;" +
            "-fx-padding: 8 20;" +
            "-fx-background-radius: 5;" +
            "-fx-scale-x: 1.05;" +
            "-fx-scale-y: 1.05;"
    ));

    btnPrint.setOnMouseExited(e -> btnPrint.setStyle(
            "-fx-background-color: #00438aff;" +   // กลับเป็นสีเดิม
            "-fx-text-fill: white;" +
            "-fx-padding: 8 20;" +
            "-fx-background-radius: 5;" +
            "-fx-scale-x: 1;" +
            "-fx-scale-y: 1;"
    ));


        // ============================
        //  MAP COLUMNS
        // ============================
        colItem.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // ============================
        //  STATUS COLOR
        // ============================
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(status);

                switch (status) {
                    case "เสร็จสิ้น" -> setStyle("-fx-text-fill: #009e0b;");
                    case "ยกเลิก" -> setStyle("-fx-text-fill: #c80000;");
                    default -> setStyle("");
                }
            }
        });

        // ============================
        //  DETAIL BUTTON
        // ============================
        colDetail.setCellFactory(col -> new TableCell<>() {

            private final Button btn = new Button("ดูรายละเอียด");

            {
                btn.setStyle(
                        "-fx-background-color: #7c7c7cff;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 5;" +
                        "-fx-padding: 5 15;" +
                        "-fx-cursor: hand;"
                );

                btn.setOnMouseEntered(e -> btn.setStyle(
                        "-fx-background-color: #ff7626ff;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 5;" +
                        "-fx-padding: 5 15;" +
                        "-fx-scale-x: 1.05;" +
                        "-fx-scale-y: 1.05;"
                ));

                btn.setOnMouseExited(e -> btn.setStyle(
                        "-fx-background-color: #7c7c7cff;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 5;" +
                        "-fx-padding: 5 15;" +
                        "-fx-scale-x: 1;" +
                        "-fx-scale-y: 1;"
                ));

                btn.setOnAction(e -> {
                    RentalHistory data = getTableView().getItems().get(getIndex());
                    showDetailPopup(data);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                } else {
                    setAlignment(Pos.CENTER);
                    setGraphic(btn);
                }
            }
        });

        // ============================
        //  BUTTON ACTIONS
        // ============================
        btnCancel.setOnAction(e -> handleCancel());
        btnPrint.setOnAction(e -> handlePrint());

        // ============================
        //  MOCK DATA
        // ============================
        loadMockData();
    }

    // ============================
    //  MOCK DATA
    // ============================
    private void loadMockData() {


        lblName.setText("นายสมชาย ใจดี");
        lblPhone.setText("097-231-8564");
        lblZone.setText("A");
        lblLock.setText("01");
        lblProduct.setText("อาหาร");
        lblDate.setText("20/11/2025");
        lblPayment.setText("โอนผ่านธนาคาร");

        // APPROVAL STATUS (mock)
        String approvalStatus = "approved"; // mock

        lblApproval.setText(switch (approvalStatus) {
            case "approved" -> "อนุมัติ";
            case "rejected" -> "ไม่อนุมัติ";
            default -> "รอดำเนินการ";
        });
        
// ตั้งค่าสีตามสถานะ
switch (approvalStatus) {
    case "approved" ->
        lblApproval.setStyle("-fx-text-fill: #009e0b; -fx-font-weight: bold;"); // เขียว
    case "rejected" ->
        lblApproval.setStyle("-fx-text-fill: #c80000; -fx-font-weight: bold;"); // แดง
    default ->
        lblApproval.setStyle("-fx-text-fill: #d68a00; -fx-font-weight: bold;"); // ส้ม
}


        historyTable.getItems().addAll(
                new RentalHistory("เข้าพื้นที่ขายอาหาร", "20/11/2025", "เสร็จสิ้น"),
                new RentalHistory("เข้าพื้นที่ขายอาหาร", "01/11/2025", "เสร็จสิ้น"),
                new RentalHistory("เข้าพื้นที่ขายอาหาร", "15/10/2025", "ยกเลิก"),
                new RentalHistory("เข้าพื้นที่ขายอาหาร", "14/09/2025", "เสร็จสิ้น"),
                new RentalHistory("เข้าพื้นที่ขายอาหาร", "05/08/2025", "เสร็จสิ้น")
        );
    }

    // ============================
    //  POPUP DETAIL
    // ============================
    private void showDetailPopup(RentalHistory data) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("รายละเอียด");
        alert.setHeaderText(data.getItemName());
        alert.setContentText(
                "วันที่ทำรายการ: " + data.getCreatedDate() +
                "\nสถานะ: " + data.getStatus()
        );
        alert.showAndWait();
    }

    // ============================
    //  CANCEL RENT
    // ============================
    private void handleCancel() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("ยกเลิกการเช่า");
        confirm.setHeaderText("คุณต้องการยกเลิกการเช่าพื้นที่ใช่หรือไม่");
        confirm.setContentText("หากยกเลิกแล้วจะไม่สามารถย้อนกลับได้");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                Alert done = new Alert(Alert.AlertType.INFORMATION);
                done.setTitle("สำเร็จ");
                done.setHeaderText("ยกเลิกการเช่าสำเร็จ");
                done.setContentText("ระบบได้ทำการยกเลิกการเช่าเรียบร้อยแล้ว");
                done.showAndWait();
            }
        });
    }

    // ============================
    //  PRINT RECEIPT
    // ============================
    private void handlePrint() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ปริ้นใบเสร็จ");
        alert.setHeaderText("ระบบกำลังเตรียมใบเสร็จ");
        alert.setContentText("ฟีเจอร์นี้จะเชื่อมต่อกับระบบจริงในภายหลัง");
        alert.showAndWait();
    }

    // ============================
    //  INNER CLASS
    // ============================
    public static class RentalHistory {

        private final String itemName;
        private final String createdDate;
        private final String status;

        public RentalHistory(String itemName, String createdDate, String status) {
            this.itemName = itemName;
            this.createdDate = createdDate;
            this.status = status;
        }

        public String getItemName() { return itemName; }
        public String getCreatedDate() { return createdDate; }
        public String getStatus() { return status; }
    }
}
