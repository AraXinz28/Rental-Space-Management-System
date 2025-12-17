package com.rental.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneManager {

    private static String currentPage = "booking"; // default

    public static void setCurrentPage(String pageName) {
        currentPage = pageName;
    }

    public static String getCurrentPage() {
        return currentPage;
    }

    public static void switchScene(Stage stage, String fxmlPath) throws IOException {
        Parent root = FXMLLoader.load(SceneManager.class.getResource(fxmlPath));

        Scene scene;
        if (stage.getScene() != null) {
            double width = stage.getScene().getWidth();
            double height = stage.getScene().getHeight();
            scene = new Scene(root, width, height);
        } else {
            scene = new Scene(root);
        }

        // ✅ ของเดิม (ไม่แตะ)
        scene.getStylesheets().add(
                SceneManager.class.getResource("/css/style.css").toExternalForm()
        );

        // ✅ เพิ่มเฉพาะ CSS เสริม (ไม่กระทบหน้าอื่น)
        scene.getStylesheets().addAll(
                SceneManager.class.getResource("/css/card.css").toExternalForm(),
                SceneManager.class.getResource("/css/button.css").toExternalForm(),
                SceneManager.class.getResource("/css/payment.css").toExternalForm()
        );

        stage.setScene(scene);
        stage.show();
    }
}
