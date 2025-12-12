package com.rental;

import com.rental.util.SceneManager;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        SceneManager.switchScene(stage, "/views/booking.fxml");


        // โหลด FXML
        Parent root = FXMLLoader.load(getClass().getResource("/views/Space.fxml"));

         Font.loadFont(getClass().getResource("/fonts/Prompt-Regular.ttf").toExternalForm(), 14);
         Font.loadFont(getClass().getResource("/fonts/Prompt-Bold.ttf").toExternalForm(), 14);


        // สร้าง Scene และเชื่อม CSS
        Scene scene = new Scene(root);
        scene.getStylesheets().add(
            getClass().getResource("/css/style.css").toExternalForm()
        );

        // ตั้งค่า Stage
        stage.setTitle("ระบบจัดการพื้นที่ให้เช่าจำหน่ายสินค้า");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();

        // ถ้าอยากเปิดเต็มหน้าจอ
        // stage.setMaximized(true);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
