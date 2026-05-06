package app.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.Gson;

import app.database.AppDatabase;
import app.database.Session;
import app.model.Account;
import app.model.Art;
import app.model.Auction;
import app.model.BidTransaction;
import app.model.Electronics;
import app.model.Item;
import app.model.Message;
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

public class MainController {

    @FXML
    private FlowPane itemContainer;
    @FXML
    private Button loginButton;
    @FXML
    private Button signUpButton;

    private final AppDatabase database = AppDatabase.getInstance();

    private final Gson gson = new com.google.gson.GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new com.google.gson.TypeAdapter<LocalDateTime>() {
                @Override
                public void write(com.google.gson.stream.JsonWriter out, LocalDateTime value) throws java.io.IOException {
                    out.value(value != null ? value.toString() : null);
                }

                @Override
                public LocalDateTime read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
                    return LocalDateTime.parse(in.nextString());
                }
            }).create();

    @FXML
    public void initialize() {
        ClientConnection.getInstance().connect("localhost", 8888);
        loadAuctionItems();
    }

    @FXML
    public void login() {
        try {
            FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/app/Login.fxml"));
            Scene loginScene = new Scene(loginLoader.load());

            Stage loginStage = (Stage) loginButton.getScene().getWindow();
            loginStage.setScene(loginScene);
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
    
    @FXML
    public void signUp() {
        try {
            FXMLLoader loginLoader = new FXMLLoader(getClass().getResource("/app/Signup.fxml"));
            Scene signUpScene = new Scene(loginLoader.load());

            Stage signUpStage = (Stage) signUpButton.getScene().getWindow();
            signUpStage.setScene(signUpScene);
        }
        catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void loadAuctionItems() {
        itemContainer.getChildren().clear();

        for (Auction auction : database.getAuctions()) {
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
            detailLabel.setText("Nghệ sĩ: " + ((Art) item).getArtist());
        } else if (item instanceof Electronics) {
            detailLabel.setText("Bảo hành: " + ((Electronics) item).getWarrantyMonths() + " tháng");
        }

        Button bidBtn = new Button("Đặt giá");
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
        dialog.setContentText("Nhập giá đấu (USD):");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(bidAmountStr -> {
            try {
                double bidAmount = Double.parseDouble(bidAmountStr);
                if (bidAmount <= auction.getCurrentHighestPrice()) {
                    showAlert(Alert.AlertType.ERROR, "LỖI", "Giá đặt phải cao hơn hiện tại!");
                    return;
                }

                BidTransaction transaction = new BidTransaction(
                        UUID.randomUUID().toString(),
                        auction.getId(),
                        getCurrentUserId(),
                        bidAmount,
                        LocalDateTime.now()
                );

                boolean success = auction.placeBid(transaction);

                if (success) {
                    priceLabel.setText("Giá hiện tại: " + auction.getCurrentHighestPrice() + " USD");
                    sendBidToServer(transaction);
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đặt giá: " + bidAmount + " USD");
                } else {
                    showAlert(Alert.AlertType.ERROR, "LỖI", "Không thể đặt giá cho phiên này!");
                }
            } catch (NumberFormatException e) {
                showAlert(Alert.AlertType.WARNING, "CHÚ Ý", "Vui lòng nhập số tiền hợp lệ.");
            }
        });
    }

    private void sendBidToServer(BidTransaction transaction) {
        String jsonData = gson.toJson(transaction);
        Message message = new Message("BID", jsonData);

        ClientConnection.getInstance().sendMessage(message);
    }

    private String getCurrentUserId() {
        Account currentAccount = Session.getCurrentAccount();
        return currentAccount == null ? "U_GUEST" : currentAccount.getId();
    }

    @FXML
    void handleLogout(ActionEvent event) {
        Session.clear();
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
