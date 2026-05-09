package app.controller;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import app.database.AppDatabase;
import app.database.Session; // Đã thêm import Session
import app.model.Art;
import app.model.Auction;
import app.model.Electronics;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainController {

    @FXML
    private FlowPane itemContainer;


    @FXML private Button loginButton;
    @FXML private Button signUpButton;
    @FXML private Button btnAddProduct; 
    @FXML private Button btnLogout;     

    @FXML
    public void initialize() {
        handleShowAll(null); 
    
        if (Session.isLoggedIn()) {
            // Đã đăng nhập: Ẩn Đăng nhập/Đăng ký, Hiện Thêm SP/Đăng xuất
            if (loginButton != null) { loginButton.setVisible(false); loginButton.setManaged(false); }
            if (signUpButton != null) { signUpButton.setVisible(false); signUpButton.setManaged(false); }
            if (btnAddProduct != null) { btnAddProduct.setVisible(true); btnAddProduct.setManaged(true); }
            if (btnLogout != null) { btnLogout.setVisible(true); btnLogout.setManaged(true); }
        } else {
            
            if (loginButton != null) { loginButton.setVisible(true); loginButton.setManaged(true); }
            if (signUpButton != null) { signUpButton.setVisible(true); signUpButton.setManaged(true); }
            if (btnAddProduct != null) { btnAddProduct.setVisible(false); btnAddProduct.setManaged(false); }
            if (btnLogout != null) { btnLogout.setVisible(false); btnLogout.setManaged(false); }
        }
    }

    @FXML
    public void handleShowAll(ActionEvent event) {
        loadAuctionsToUI(AppDatabase.getInstance().getAuctions());
    }

    @FXML
    public void handleShowArt(ActionEvent event) {
        List<Auction> arts = AppDatabase.getInstance().getAuctions().stream()
                .filter(a -> a.getItem() instanceof Art).collect(Collectors.toList());
        loadAuctionsToUI(arts);
    }

    @FXML
    public void handleShowElectronics(ActionEvent event) {
        List<Auction> elecs = AppDatabase.getInstance().getAuctions().stream()
                .filter(a -> a.getItem() instanceof Electronics).collect(Collectors.toList());
        loadAuctionsToUI(elecs);
    }

    private void loadAuctionsToUI(List<Auction> auctions) {
        itemContainer.getChildren().clear(); 

        for (Auction auction : auctions) {
            VBox card = new VBox(10);
            card.setPadding(new Insets(15));
            card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
            card.setPrefWidth(250);

            Label nameLbl = new Label(auction.getItem().getName());
            nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

            Label priceLbl = new Label("Giá hiện tại: " + auction.getCurrentHighestPrice() + " USD");
            priceLbl.setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold;");

            Button bidBtn = new Button("Đặt giá ngay");
            bidBtn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 5;");
            bidBtn.setMaxWidth(Double.MAX_VALUE);
            bidBtn.setOnAction(e -> handleBid(auction));

            Button historyBtn = new Button("Xem lịch sử đặt giá");
            historyBtn.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-background-radius: 5;");
            historyBtn.setMaxWidth(Double.MAX_VALUE);
            historyBtn.setOnAction(e -> showBidHistory(auction));

            card.getChildren().addAll(nameLbl, priceLbl, bidBtn, historyBtn);
            itemContainer.getChildren().add(card);
        }
    }

    private void handleBid(Auction auction) {
 
        if (!Session.isLoggedIn()) {
            showAlert(Alert.AlertType.WARNING, "Cảnh báo", "Bạn cần đăng nhập để đặt giá!");
            return;
        }
       
        String currentUsername = Session.getCurrentAccount().getUsername();
        
        double newPrice = auction.getCurrentHighestPrice() + 50.0;
        boolean success = AppDatabase.getInstance().placeBid(auction.getId(), currentUsername, newPrice);
        
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã gửi yêu cầu đặt giá " + newPrice + " USD!");
            handleShowAll(null); 
        }
    }

    private void showBidHistory(Auction auction) {
        List<String> history = AppDatabase.getInstance().getBidHistory(auction.getId());
        String content = history.isEmpty() ? "Chưa có ai đặt giá." : String.join("\n", history);
        showAlert(Alert.AlertType.INFORMATION, "Lịch sử đấu giá: " + auction.getItem().getName(), content);
    }

    @FXML
    public void handleOpenAddProduct(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/AddProduct.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Thêm Sản Phẩm Mới");
            stage.setScene(new Scene(loader.load()));
            stage.showAndWait(); 
            handleShowAll(null); 
        } catch (IOException e) { e.printStackTrace(); }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        try {
         
            Session.setCurrentAccount(null);
     
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/Login.fxml"));
            Parent root = loader.load();
       
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("Hệ thống Đấu giá - Đăng nhập");
            stage.show();
   
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể quay lại màn hình đăng nhập!");
        }
    }

    @FXML
    public void login(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("Hệ thống Đấu giá - Đăng nhập");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở màn hình đăng nhập!");
        }
    }

    @FXML
    public void signUp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/Signup.fxml"));
            javafx.scene.Parent root = loader.load();

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
     
            stage.setScene(new Scene(root));
            stage.setTitle("Hệ thống Đấu giá - Đăng ký tài khoản");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể mở màn hình đăng ký!");
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