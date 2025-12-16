package com.rental;

import com.rental.util.SceneManager;
import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {

        // ✅ โหลดฟอนต์ Prompt
        Font.loadFont(getClass().getResource("/fonts/Prompt-Regular.ttf").toExternalForm(), 14);
        Font.loadFont(getClass().getResource("/fonts/Prompt-Bold.ttf").toExternalForm(), 14);

        // ✅ โหลดหน้าแรกโดยใช้ SceneManager
     
        SceneManager.switchScene(stage, "/views/payment.fxml");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
