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

    @FXML
    private void initialize() {
        menus = new HBox[] { zoneMenu, bookingMenu, customerMenu, paymentMenu, historyMenu };

        String current = SceneManager.getCurrentPage();
        switch (current) {
            case "booking" -> setActive(bookingMenu);
            case "customer" -> setActive(customerMenu);
            case "payment" -> setActive(paymentMenu);
            case "history" -> setActive(historyMenu);
            default -> setActive(zoneMenu);
        }
    }

    private void setActive(HBox menu) {
        for (HBox m : menus)
            m.getStyleClass().remove("active");
        menu.getStyleClass().add("active");
    }

    @FXML
    private void goToZone(MouseEvent e) {
        SceneManager.setCurrentPage("zone");
        setActive(zoneMenu);
        switchTo(e, "/views/zone_management.fxml");
    }

    @FXML
    private void goToBooking(MouseEvent e) {
        SceneManager.setCurrentPage("booking");
        setActive(bookingMenu);
        switchTo(e, "/views/booking_management.fxml");
    }

    @FXML
    private void goToCustomer(MouseEvent e) {
        SceneManager.setCurrentPage("customer");
        setActive(customerMenu);
        switchTo(e, "/views/managetenants.fxml");
    }

    @FXML
    private void goToPayment(MouseEvent e) {
        SceneManager.setCurrentPage("payment");
        setActive(paymentMenu);
        switchTo(e, "/views/checkPaymentStatus.fxml");
    }

    @FXML
    private void goToHistory(MouseEvent e) {
        SceneManager.setCurrentPage("history");
        setActive(historyMenu);
        switchTo(e, "/views/rentalhistorymanage.fxml");
    }

    private void switchTo(MouseEvent e, String fxml) {
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, fxml);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
