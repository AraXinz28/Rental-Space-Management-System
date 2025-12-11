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

    @FXML
    private void initialize() {
        menus = new HBox[]{zoneMenu, bookingMenu, customerMenu, paymentMenu, historyMenu};
        setActive(bookingMenu);
    }

    private void setActive(HBox activeMenu) {
        for (HBox m : menus) {
            m.setStyle(""); // รีเซ็ตทุกอัน
        }
        activeMenu.setStyle("-fx-background-color: #E0E0E0;");
    }

    @FXML
    private void goToZone(MouseEvent e) {
        setActive(zoneMenu);
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, "/views/zone_management.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void goToBooking(MouseEvent e) {
        setActive(bookingMenu);
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, "/views/booking_management.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void goToCustomer(MouseEvent e) {
        setActive(customerMenu);
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, "/views/customer_management.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void goToPayment(MouseEvent e) {
        setActive(paymentMenu);
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, "/views/payment_check.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void goToHistory(MouseEvent e) {
        setActive(historyMenu);
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, "/views/history.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
