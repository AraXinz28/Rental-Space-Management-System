package com.rental;

import com.rental.util.SceneManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // โหลดฟอนต์
        Font.loadFont(getClass().getResource("/fonts/Prompt-Regular.ttf").toExternalForm(), 14);
        Font.loadFont(getClass().getResource("/fonts/Prompt-Bold.ttf").toExternalForm(), 14);

        // โหลดหน้าแรก (🔥 ได้ Scene กลับมาที่นี่)
        Scene scene = SceneManager.switchScene(stage, "/views/homepage.fxml");

        if (scene == null) {
            System.err.println("❌ ERROR: ไม่สามารถโหลดไฟล์ FXML ได้: /views/booking.fxml");
            return;
        }

        // ควบคุมขนาดหน้าจอให้คงที่
        stage.setWidth(1280);
        stage.setHeight(700);
        stage.setMinWidth(1280);
        stage.setMinHeight(700);
        stage.setMaxWidth(1280);
        stage.setMaxHeight(700);
        stage.setResizable(false);

        stage.setTitle("ระบบจัดการพื้นที่ให้เช่าจำหน่ายสินค้า");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
