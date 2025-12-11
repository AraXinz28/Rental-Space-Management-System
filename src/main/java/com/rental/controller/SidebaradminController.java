package com.rental.controller;

import com.rental.util.Navigation;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

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
        Navigation.goTo("zone_management.fxml", (Node) e.getSource());
    }

    @FXML
    private void goToBooking(MouseEvent e) {
        setActive(bookingMenu);
        Navigation.goTo("booking_management.fxml", (Node) e.getSource());
    }

    @FXML
    private void goToCustomer(MouseEvent e) {
        setActive(customerMenu);
        Navigation.goTo("customer_management.fxml", (Node) e.getSource());
    }

    @FXML
    private void goToPayment(MouseEvent e) {
        setActive(paymentMenu);
        Navigation.goTo("payment_check.fxml", (Node) e.getSource());
    }

    @FXML
    private void goToHistory(MouseEvent e) {
        setActive(historyMenu);
        Navigation.goTo("history.fxml", (Node) e.getSource());
    }
}
