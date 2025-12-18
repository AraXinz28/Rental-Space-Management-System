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
    @FXML private HBox rentalhistoryMenu;

    private HBox[] menus;
    private HBox activeMenu;

    // เก็บเมนูที่ active ล่าสุด
    private static String lastActiveMenu = "home";

    @FXML
    private void initialize() {
        menus = new HBox[]{homeMenu, searchMenu, bookingMenu, paymentMenu, rentalhistoryMenu};

        // ตั้งค่าเมนู active ตามครั้งล่าสุด
        switch (lastActiveMenu) {
            case "home" -> setActive(homeMenu);
            case "search" -> setActive(searchMenu);
            case "booking" -> setActive(bookingMenu);
            case "payment" -> setActive(paymentMenu);
            case "history" -> setActive(rentalhistoryMenu);
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
            m.setStyle(""); // ล้างสีเก่า
        }
        menu.setStyle("-fx-background-color: #e0e0e0;");
        activeMenu = menu;

        // บันทึกเมนู active ล่าสุด
        if (menu == homeMenu) lastActiveMenu = "home";
        else if (menu == searchMenu) lastActiveMenu = "search";
        else if (menu == bookingMenu) lastActiveMenu = "booking";
        else if (menu == paymentMenu) lastActiveMenu = "payment";
        else if (menu == rentalhistoryMenu) lastActiveMenu = "history";
    }

    // =============== เมนูทั้งหมด ===============

    @FXML
    private void goToHome(MouseEvent e) {
        setActive(homeMenu);
        switchToScene(e, "/views/homepage.fxml");
    }

    @FXML
    private void goToSearch(MouseEvent e) {
        setActive(searchMenu);
        switchToScene(e, "/views/Space.fxml");
    }

    @FXML
    private void goToBooking(MouseEvent e) {
        setActive(bookingMenu);
        switchToScene(e, "/views/booking.fxml");
    }

    @FXML
    private void goToPayment(MouseEvent e) {
        setActive(paymentMenu);
        switchToScene(e, "/views/payment.fxml");
    }

    @FXML
    private void goTorentalhistory(MouseEvent e) {
        setActive(rentalhistoryMenu);
        switchToScene(e, "/views/rentalhistory.fxml");
    }

    // ฟังก์ชันช่วยสลับหน้า (ลดโค้ดซ้ำ + แก้ error method undefined)
    private void switchToScene(MouseEvent e, String fxmlPath) {
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, fxmlPath);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}