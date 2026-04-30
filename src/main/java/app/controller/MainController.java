package app.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainController {

    @FXML
    private FlowPane itemContainer;

    @FXML
    public void initialize() {
       
        addSampleItems();
    }

    private void addSampleItems() {
  
        for (int i = 1; i <= 6; i++) {
            VBox card = new VBox(10);
            card.getStyleClass().add("item-card");
            card.setPrefWidth(220);

            Label title = new Label("Sản phẩm mẫu " + i);
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
            
            Label price = new Label("Giá hiện tại: " + (100 * i) + " USD");
            price.setStyle("-fx-text-fill: #E67E22; -fx-font-weight: bold;");
            
            Button bidBtn = new Button("Đặt giá ngay");
            bidBtn.getStyleClass().add("btn-primary");
            bidBtn.setMaxWidth(Double.MAX_VALUE);

            card.getChildren().addAll(title, price, bidBtn);
            itemContainer.getChildren().add(card);
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) itemContainer.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Đăng nhập");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}