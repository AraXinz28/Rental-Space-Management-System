package com.rental.controller;

import com.rental.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class SidebartController {

    @FXML
    private HBox homeMenu;
    @FXML
    private HBox searchMenu;
    @FXML
    private HBox bookingMenu;
    @FXML
    private HBox paymentMenu;
    @FXML
    private HBox rentalhistoryMenu;

    private HBox[] menus;
    private static String lastActiveMenu = "home";

    @FXML
    private void initialize() {
        menus = new HBox[] { homeMenu, searchMenu, bookingMenu, paymentMenu, rentalhistoryMenu };

        switch (lastActiveMenu) {
            case "search" -> setActive(searchMenu);
            case "booking" -> setActive(bookingMenu);
            case "payment" -> setActive(paymentMenu);
            case "history" -> setActive(rentalhistoryMenu);
            default -> setActive(homeMenu);
        }
    }

    private void setActive(HBox menu) {
        for (HBox m : menus)
            m.getStyleClass().remove("active");
        menu.getStyleClass().add("active");

        if (menu == homeMenu)
            lastActiveMenu = "home";
        else if (menu == searchMenu)
            lastActiveMenu = "search";
        else if (menu == bookingMenu)
            lastActiveMenu = "booking";
        else if (menu == paymentMenu)
            lastActiveMenu = "payment";
        else if (menu == rentalhistoryMenu)
            lastActiveMenu = "history";
    }

    @FXML
    private void goToHome(MouseEvent e) {
        setActive(homeMenu);
        switchTo(e, "/views/homepage.fxml");
    }

    @FXML
    private void goToSearch(MouseEvent e) {
        setActive(searchMenu);
        switchTo(e, "/views/Space.fxml");
    }

    @FXML
    private void goToBooking(MouseEvent e) {
        setActive(bookingMenu);
        switchTo(e, "/views/booking.fxml");
    }

    @FXML
    private void goToPayment(MouseEvent e) {
        setActive(paymentMenu);
        switchTo(e, "/views/payment.fxml");
    }

    @FXML
    private void goTorentalhistory(MouseEvent e) {
        setActive(rentalhistoryMenu);
        switchTo(e, "/views/rentalhistory.fxml");
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