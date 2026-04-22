package app;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        
        Label label = new Label("Chào mừng đến với Hệ thống Đấu giá!");
        StackPane root = new StackPane(label);
        
        Scene scene = new Scene(root, 800, 600);
        
        primaryStage.setTitle("Ứng dụng Đấu Giá Trực Tuyến - Client");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        // Khởi chạy giao diện JavaFX
        launch(args);
    }
}