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
     
<<<<<<< HEAD
        SceneManager.switchScene(stage, "/views/rentalhistory.fxml");
=======

        SceneManager.switchScene(stage, "/views/payment.fxml");

       
>>>>>>> 5af1a8ab8045c2f87a3df0b7d5a566caa6d976b4
    }

    public static void main(String[] args) {
        launch(args);
    }
}
