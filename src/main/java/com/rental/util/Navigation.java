package com.rental.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Navigation {

    /**
     * ใช้สำหรับเปลี่ยน Scene ไปยังหน้าใหม่
     */
    public static void goTo(String fxml, Node currentNode) {
        try {
            Parent root = FXMLLoader.load(Navigation.class.getResource("/views/" + fxml));

            Stage stage = (Stage) currentNode.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("❌ ไม่สามารถโหลดหน้า: " + fxml);
        }
    }
}
