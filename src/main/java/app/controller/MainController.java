package app.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;

import app.model.Art;
import app.model.Electronics;
import app.model.Item;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainController {

    @FXML
    private FlowPane itemContainer;

    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
     
        loadAuctionItems();
    }

 
    private void loadAuctionItems() {
        itemContainer.getChildren().clear();


        List<Item> itemList = new ArrayList<>();
        itemList.add(new Art("A01", "Tranh sơn dầu Phố Cổ", "Tác phẩm vẽ về Hà Nội xưa", 1200.0, "Bùi Xuân Phái", 1980));
        itemList.add(new Art("A02", "Tượng gỗ tạc tay", "Tượng điêu khắc từ gỗ lũa nguyên khối", 500.0, "Nghệ nhân nặc danh", 2023));

        itemList.add(new Electronics("E01", "iPhone 15 Pro Max", "Hàng chính hãng VN/A mới 100%", 1000.0, 12));
        itemList.add(new Electronics("E02", "MacBook Pro M3", "Cấu hình 16GB RAM, 512GB SSD", 2500.0, 24));

        for (Item item : itemList) {
            createItemCard(item);
        }
    }


    private void createItemCard(Item item) {
        VBox card = new VBox(12);
        card.getStyleClass().add("item-card");
        card.setPrefWidth(250); 


        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label priceLabel = new Label("Giá hiện tại: " + item.getStartingPrice() + " USD");
        priceLabel.setStyle("-fx-text-fill: #E67E22; -fx-font-weight: bold;");

        Label detailLabel = new Label();
        detailLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-style: italic;");
        
        if (item instanceof Art) {
            detailLabel.setText(" Họa sĩ: " + ((Art) item).getArtist());
        } else if (item instanceof Electronics) {
            detailLabel.setText(" Bảo hành: " + ((Electronics) item).getWarrantyMonths() + " tháng");
        }

        
        Button bidBtn = new Button("Đặt giá ngay");
        bidBtn.getStyleClass().add("btn-primary");
        bidBtn.setMaxWidth(Double.MAX_VALUE);
        bidBtn.setOnAction(e -> handleBidAction(item));

        card.getChildren().addAll(nameLabel, priceLabel, detailLabel, bidBtn);
        itemContainer.getChildren().add(card);
    }


    private void handleBidAction(Item item) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Đấu giá sản phẩm");
        dialog.setHeaderText("Sản phẩm: " + item.getName());
        dialog.setContentText("Nhập giá bạn muốn đặt (USD):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(bidAmountStr -> {
            try {
                double bidAmount = Double.parseDouble(bidAmountStr);
                if (bidAmount <= item.getStartingPrice()) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá đặt phải cao hơn giá hiện tại!");
                } else {
        
                    sendBidToServer(item.getId(), bidAmount);
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng chỉ nhập số tiền hợp lệ.");
            }
        });
    }


    private void sendBidToServer(String itemId, double amount) {

        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Yêu cầu đặt giá đã được gửi lên hệ thống!");
    }

    @FXML
    void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) itemContainer.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Hệ thống Đấu giá - Đăng nhập");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}