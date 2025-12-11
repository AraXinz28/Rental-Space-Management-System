package com.rental.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {

    // ใช้เรียกเปลี่ยนหน้า พร้อมเชื่อม style.css ทุกครั้ง
    public static void switchScene(Stage stage, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(SceneManager.class.getResource(fxmlPath));
        Scene scene = new Scene(root);

        // เชื่อม stylesheet เดียวทุกหน้า
        scene.getStylesheets().add(SceneManager.class.getResource("/css/style.css").toExternalForm());

        stage.setScene(scene);
        stage.show();
    }
}
