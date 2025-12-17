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

    // Static variable เก็บเมนู active ล่าสุด
    private static String lastActiveMenu = "home"; // เริ่มต้นเป็น home

    @FXML
    private void initialize() {
        // รวมเมนูทั้งหมด
        menus = new HBox[]{homeMenu, searchMenu, bookingMenu, paymentMenu, rentalhistoryMenu};

        // เซ็ตเมนู active ตามค่า lastActiveMenu
        switch (lastActiveMenu) {
            case "home": setActive(homeMenu); break;
            case "search": setActive(searchMenu); break;
            case "booking": setActive(bookingMenu); break;
            case "payment": setActive(paymentMenu); break;
            case "history": setActive(rentalhistoryMenu); break;
        }

        // เพิ่ม hover effect ให้แต่ละเมนู
        for (HBox m : menus) {
            m.setOnMouseEntered(e -> {
                if (m != activeMenu) {
                    m.setStyle("-fx-background-color: #E0E0E0;");
                }
            });

            m.setOnMouseExited(e -> {
                if (m != activeMenu) {
                    m.setStyle("");
                }
            });
        }
    }

    /**
     * ฟังก์ชันเซ็ต active menu
     */
    private void setActive(HBox menu) {
        // รีเซ็ตสีทุกเมนู
        for (HBox m : menus) {
            m.setStyle("");
        }

        // เซ็ตสี active
        menu.setStyle("-fx-background-color: #E0E0E0;");
        activeMenu = menu;

        // อัพเดต lastActiveMenu
        if (menu == homeMenu) lastActiveMenu = "home";
        else if (menu == searchMenu) lastActiveMenu = "search";
        else if (menu == bookingMenu) lastActiveMenu = "booking";
        else if (menu == paymentMenu) lastActiveMenu = "payment";
        else if (menu == rentalhistoryMenu) lastActiveMenu = "history";
    }

    // ------------------- Action ของแต่ละเมนู -------------------

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
    private void goTorentalhistory(MouseEvent e) {
        setActive(rentalhistoryMenu);
        Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
        try {
            SceneManager.switchScene(stage, "/views/rentalhistory.fxml");
        } catch (Exception ex) {
            ex.printStackTrace();
   
        }
    }   
}
