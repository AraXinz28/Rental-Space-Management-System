package com.rental;

import com.rental.util.SceneManager;
import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {

        // ✅ โหลดฟอนต์ Prompt
        Font.loadFont(getClass().getResource("/fonts/Prompt-Regular.ttf").toExternalForm(), 14);
        Font.loadFont(getClass().getResource("/fonts/Prompt-Bold.ttf").toExternalForm(), 14);

        
        primaryStage.setTitle("ระบบจัดการพื้นที่ให้เช่า");

        primaryStage.setMaximized(true);

   
        SceneManager.switchScene(primaryStage, "/views/checkPaymentStatus.fxml");
    }
       

    public static void main(String[] args) {
        launch(args);
    }
}
