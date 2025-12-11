package com.rental;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        Parent root = FXMLLoader.load(getClass().getResource("/views/booking_management.fxml"));

        Scene scene = new Scene(root); // ไม่ fix ขนาด

        stage.setTitle("ระบบจัดการพื้นที่ให้เช่าจำหน่ายสินค้า");
        stage.setScene(scene);
        stage.setResizable(true);  // อนุญาตให้ขยาย/ย่อหน้าต่าง
        stage.show();

        // ขยายเต็มหน้าจอ (ถ้าอยาก)
        // stage.setMaximized(true);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
