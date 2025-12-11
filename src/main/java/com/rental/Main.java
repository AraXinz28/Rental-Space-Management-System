package com.rental;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/booking.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("ระบบจัดการพื้นที่ให้เช่าจำหน่ายสินค้า");
        stage.setScene(scene);
        stage.show();

    }

    public static void main(String[] args) {
        launch();
    }

}
