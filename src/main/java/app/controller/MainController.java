package app.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.model.*;
import com.google.gson.Gson;

import app.network.ClientConnection;
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

import app.model.Auction;
import app.model.Art;
import app.model.BidTransaction;
import app.model.Electronics;
import app.model.Item;
import app.model.Message;

public class MainController {

    @FXML
    private FlowPane itemContainer;

    private final Gson gson = new com.google.gson.GsonBuilder()
    .registerTypeAdapter(java.time.LocalDateTime.class, new com.google.gson.TypeAdapter<java.time.LocalDateTime>() {
        @Override
        public void write(com.google.gson.stream.JsonWriter out, java.time.LocalDateTime value) throws java.io.IOException {
            out.value(value != null ? value.toString() : null);
        }
        @Override
        public java.time.LocalDateTime read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
            return java.time.LocalDateTime.parse(in.nextString());
        }
    }).create();

    @FXML
    public void initialize() {

        ClientConnection.getInstance().connect("localhost", 8888);

  
        loadAuctionItems();
    }

  
    private void loadAuctionItems() {
        itemContainer.getChildren().clear();

        List<Auction> auctionlist = new ArrayList<>();//Lưu dưới dạng Item bọc Auction để cập nhật giá theo phiên
        
        auctionlist.add(new Auction("A01",
                new Art("A01",
                        "Tranh phố cổ",
                        "Trang sơn dầu Hà Nội",
                        1200.0,
                        "Nguyễn Xuân Phái",
                        1980),
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().plusHours(2),
        1200.0,
                "RUNNING"));

        auctionlist.add(new Auction("A02",
                new Art("A02",
                        "Tượng Gỗ Lũa",
                        "Tượng nghệ thuật điêu khắc",
                        500.0,
                        "Nghệ nhân Việt",
                        2023),
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(1),
                500.0,
                "RUNNING"));

        auctionlist.add(new Auction("E01",
                new Electronics("E01",
                        "Iphone 15 Pro",
                        "8/128",
                        1000.0,
                        12),
                LocalDateTime.now().minusMinutes(20),
                LocalDateTime.now().plusHours(3),
                1000.0,
                "RUNNING"));

        auctionlist.add(new Auction("E02",
                new Electronics("E02", "Macbook M4",
                        "8/512",
                        2600.0,
                        24),
                LocalDateTime.now().minusMinutes(15),
                LocalDateTime.now().plusHours(3),
                2600.0,
                "RUNNING"));

        for (Auction auction : auctionlist) {
            createAuctionCard(auction);
        }
    }


    private void createAuctionCard(Auction auction) {
        Item item = auction.getItem();

        VBox card = new VBox(12);
        card.getStyleClass().add("item-card");
        card.setPrefWidth(250);

        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label priceLabel = new Label("Giá hiện tại: " + auction.getCurrentHighestPrice() + " USD");
        priceLabel.setStyle("-fx-text-fill: #E67E22; -fx-font-weight: bold;");

        Label detailLabel = new Label();
        detailLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-style: italic;");
        

        if (item instanceof Art) {
            detailLabel.setText("🎨 Họa sĩ: " + ((Art) item).getArtist());
        } else if (item instanceof Electronics) {
            detailLabel.setText("💻 Bảo hành: " + ((Electronics) item).getWarrantyMonths() + " tháng");
        }

        Button bidBtn = new Button("Đặt giá ngay");
        bidBtn.getStyleClass().add("btn-primary");
        bidBtn.setMaxWidth(Double.MAX_VALUE);
        bidBtn.setOnAction(e -> handleBidAction(auction, priceLabel));

        card.getChildren().addAll(nameLabel, priceLabel, detailLabel, bidBtn);
        itemContainer.getChildren().add(card);
    }


    private void handleBidAction(Auction auction, Label priceLabel) {
        Item item = auction.getItem();

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Đấu giá");
        dialog.setHeaderText("Sản phẩm: " + item.getName());
        dialog.setContentText("Nhập giá bạn muốn đặt (USD):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(bidAmountStr -> {
            try {
                double bidAmount = Double.parseDouble(bidAmountStr);
                if (bidAmount <= auction.getCurrentHighestPrice()) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá đặt phải cao hơn giá hiện tại!");
                } else {
                    // GỬI LÊN SERVER
                BidTransaction transaction = new BidTransaction(
                        UUID.randomUUID().toString(),
                        auction.getId(),
                        "User001",
                        bidAmount,
                        LocalDateTime.now()
                );

                boolean success = auction.placeBid(transaction);

                if (success) {
                    priceLabel.setText("Giá hiện tại: " + auction.getCurrentHighestPrice() + " USD");
                    sendBidToServer(transaction);
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đặt giá: " + bidAmount + "USD");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đặt giá cho phiên này");
                }
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng nhập số tiền hợp lệ.");
            }
        });
    }

    private void sendBidToServer(BidTransaction transaction) {
        String jsonData = gson.toJson(transaction);
        Message message = new Message("BID", jsonData);

        ClientConnection.getInstance().sendMessage(message);
    }

    @FXML
    void handleLogout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) itemContainer.getScene().getWindow();
            stage.setScene(scene);
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