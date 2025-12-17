package com.rental.controller;

import com.rental.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class SidebaradminController {

    @FXML private HBox zoneMenu;
    @FXML private HBox bookingMenu;
    @FXML private HBox customerMenu;
    @FXML private HBox paymentMenu;
    @FXML private HBox historyMenu;

    private HBox[] menus;
    private HBox activeMenu;

    @FXML
    private void initialize() {
        menus = new HBox[]{zoneMenu, bookingMenu, customerMenu, paymentMenu, historyMenu};

        // ✅ ตั้ง active menu ตาม SceneManager (หรือ default)
        String current = SceneManager.getCurrentPage();
        switch (current) {
            case "zone" -> setActive(zoneMenu);
            case "customer" -> setActive(customerMenu);
            case "payment" -> setActive(paymentMenu);
            case "history" -> setActive(historyMenu);
            default -> setActive(bookingMenu);
        }

        // ✅ hover effect ที่ไม่ค้าง
        for (HBox menu : menus) {
            menu.setOnMouseEntered(e -> {
                if (menu != activeMenu) {
                    menu.setStyle("-fx-background-color: #F5F5F5;");
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
        menu.setStyle("-fx-background-color: #E0E0E0;");
        activeMenu = menu;
    }

    @FXML
    private void goToZone(MouseEvent e) {
        SceneManager.setCurrentPage("zone");
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, "/views/zone_management.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void goToBooking(MouseEvent e) {
        SceneManager.setCurrentPage("booking");
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, "/views/booking_management.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void goToCustomer(MouseEvent e) {
        SceneManager.setCurrentPage("customer");
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, "/views/managetenants.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void goToPayment(MouseEvent e) {
        SceneManager.setCurrentPage("payment");
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, "/views/checkPaymentStatus.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void goToHistory(MouseEvent e) {
        SceneManager.setCurrentPage("history");
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, "/views/history.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}