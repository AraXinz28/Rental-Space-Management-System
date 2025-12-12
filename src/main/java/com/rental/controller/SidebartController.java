package com.rental.controller;

import com.rental.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class SidebartController {

    @FXML private HBox homeMenu;
    @FXML private HBox searchMenu;
    @FXML private HBox bookingMenu;
    @FXML private HBox paymentMenu;
    @FXML private HBox historyMenu;

    private HBox[] menus;

    @FXML
    private void initialize() {
        menus = new HBox[]{homeMenu, searchMenu, bookingMenu, paymentMenu, historyMenu};
        setActive(homeMenu);
    }

    private void setActive(HBox activeMenu) {
        for (HBox m : menus) {
            m.setStyle(""); // รีเซ็ตทุกอัน
        }
        activeMenu.setStyle("-fx-background-color: #E0E0E0;");
    }

    @FXML
    private void goToHome(MouseEvent e) {
        setActive(homeMenu);
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, "/views/homepage.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void goToSearch(MouseEvent e) {
        setActive(searchMenu);
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, "/views/Space.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void goToBooking(MouseEvent e) {
        setActive(bookingMenu);
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, "/views/booking.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void goToPayment(MouseEvent e) {
        setActive(paymentMenu);
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, "/views/payment.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void goToHistory(MouseEvent e) {
        setActive(historyMenu);
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, "/views/history1.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
