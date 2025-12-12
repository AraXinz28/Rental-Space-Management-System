package com.rental.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    public static Scene switchScene(Stage stage, String fxmlPath) throws Exception {
        Parent root = FXMLLoader.load(SceneManager.class.getResource(fxmlPath));
        Scene scene = new Scene(root);

        // ใส่ CSS ทุกหน้า
        scene.getStylesheets().add(
                SceneManager.class.getResource("/css/style.css").toExternalForm());

        stage.setScene(scene);
        return scene; // ⬅️ ส่ง Scene กลับไปให้ Main
    }
}
