package com.rental.controller;

import com.rental.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class SidebaradminController {

    @FXML
    private HBox zoneMenu;
    @FXML
    private HBox bookingMenu;
    @FXML
    private HBox customerMenu;
    @FXML
    private HBox paymentMenu;
    @FXML
    private HBox historyMenu;

    private HBox[] menus;
    private HBox activeMenu;

    @FXML
    private void initialize() {
        menus = new HBox[] { zoneMenu, bookingMenu, customerMenu, paymentMenu, historyMenu };

        String current = SceneManager.getCurrentPage();
        switch (current) {
            case "zone" -> setActive(zoneMenu);
            case "booking" -> setActive(bookingMenu);
            case "customer" -> setActive(customerMenu);
            case "payment" -> setActive(paymentMenu);
            case "history" -> setActive(historyMenu);
            default -> setActive(zoneMenu);
        }

        // Hover effect (สวย ไม่บั๊ค)
        for (HBox menu : menus) {
            menu.setOnMouseEntered(e -> {
                if (menu != activeMenu) {
                    menu.setStyle("-fx-background-color: #f0f0f0; -fx-cursor: hand;");
                }
            });

            menu.setOnMouseExited(e -> {
                if (menu != activeMenu) {
                    menu.setStyle("");
                }
            });
        }
    }

    private void setActive(HBox menu) {
        for (HBox m : menus) {
            m.setStyle("");
        }
        menu.setStyle("-fx-background-color: #e0e0e0;");
        activeMenu = menu;
    }

    // =============== เมนูทั้งหมด ===============

    @FXML
    private void goToZone(MouseEvent e) {
        setActive(zoneMenu);
        SceneManager.setCurrentPage("zone");
        switchToScene(e, "/views/zone_management.fxml"); // แก้ path ให้ตรงกับของคุณ
    }

    @FXML
    private void goToBooking(MouseEvent e) {
        setActive(bookingMenu);
        SceneManager.setCurrentPage("booking");
        switchToScene(e, "/views/booking_management.fxml");
    }

    @FXML
    private void goToCustomer(MouseEvent e) {
        setActive(customerMenu);
        SceneManager.setCurrentPage("customer");
        switchToScene(e, "/views/managetenants.fxml"); // หรือ path ที่ถูกต้อง
    }

    @FXML
    private void goToPayment(MouseEvent e) {
        setActive(paymentMenu);
        SceneManager.setCurrentPage("payment");
        switchToScene(e, "/views/checkPaymentStatus.fxml");
    }

    @FXML
    private void goToHistory(MouseEvent e) {
        setActive(historyMenu);
        SceneManager.setCurrentPage("history");
        switchToScene(e, "/views/rentalhistorymanage.fxml");
    }

    // ฟังก์ชันช่วยสลับหน้า (ลดโค้ดซ้ำ)
    private void switchToScene(MouseEvent e, String fxmlPath) {
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, fxmlPath);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}