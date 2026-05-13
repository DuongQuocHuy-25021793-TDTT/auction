package app.controller;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import app.database.AppDatabase;
import app.database.Session;
import app.model.Art;
import app.model.Auction;
import app.model.Electronics;
import app.network.NetworkClient; 
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
import javafx.scene.control.TextField; 
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainController {

    @FXML private FlowPane itemContainer;
    @FXML private Button loginButton;
    @FXML private Button signUpButton;
    @FXML private Button btnAddProduct; 
    @FXML private Button btnLogout;     
    @FXML private TextField searchField;

    @FXML
    public void initialize() {
        handleShowAll(null); 
        
        if (Session.isLoggedIn()) {
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

       
        NetworkClient.startRealtimeListener(auctionId -> {
            // Khi có thông báo từ Server, tiến hành làm mới lại toàn bộ giao diện danh sách
            System.out.println("Giao diện nhận được thông báo cập nhật giá cho: " + auctionId);
            handleShowAll(null);
        });
    }

    @FXML
    public void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().trim().toLowerCase();

        if (keyword.isEmpty()) {
            handleShowAll(null);
            return;
        }

        List<Auction> searchResults = AppDatabase.getInstance().getAuctions().stream()
                .filter(a -> a.getItem().getName().toLowerCase().contains(keyword))
                .collect(Collectors.toList());

        loadAuctionsToUI(searchResults);
        
        if (searchResults.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Kết quả tìm kiếm", "Không tìm thấy sản phẩm nào khớp với: " + keyword);
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
            historyBtn.setOnAction(e -> handleViewHistory(auction));

            card.getChildren().addAll(nameLbl, priceLbl, bidBtn, historyBtn);
            itemContainer.getChildren().add(card);
        }
    }


    private void handleBid(Auction auction) {
        if (!app.database.Session.isLoggedIn()) {
            showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Cảnh báo", "Bạn cần đăng nhập để đặt giá!");
            return;
        }

        String currentUsername = app.database.Session.getCurrentAccount().getUsername();
        double newPrice = auction.getCurrentHighestPrice() + 50.0; // Đặt giá cao hơn 50$

        app.model.BidTransaction bid = new app.model.BidTransaction(
                java.util.UUID.randomUUID().toString(), 
                auction.getId(), 
                currentUsername, 
                newPrice, 
                java.time.LocalDateTime.now()
        );
       
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
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
        
        String bidJson = gson.toJson(bid);
        app.model.Message message = new app.model.Message("BID", bidJson); 
        String finalJsonToSend = gson.toJson(message); 

        String response = app.network.NetworkClient.sendRequest(finalJsonToSend);
        
        if ("SUCCESS".equals(response)) {
            showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Thành công", "Đã đặt giá " + newPrice + " USD cho sản phẩm: " + auction.getItem().getName());
            
        } else if ("FAIL".equals(response)) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Thất bại", "Server từ chối đặt giá (Giá không hợp lệ hoặc lỗi hệ thống)!");
        } else {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến Server Đấu Giá. Hãy kiểm tra xem Server đã chạy chưa!");
        }
    }

    @FXML
    private void handleViewHistory(app.model.Auction auction) {
        app.model.Message msg = new app.model.Message("GET_HISTORY", auction.getId());
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String response = app.network.NetworkClient.sendRequest(gson.toJson(msg));

        if (response != null && !response.isEmpty() && !response.equals("ERROR")) {
            try {
                java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<String>>(){}.getType();
                java.util.List<String> history = gson.fromJson(response, listType);

                if (history.isEmpty()) {
                    showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Lịch sử đấu giá", "Chưa có ai đặt giá cho sản phẩm này.");
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("CHI TIẾT CÁC LƯỢT ĐẶT GIÁ:\n");
                    sb.append("------------------------------------------\n");
                    for (String line : history) {
                        sb.append("🔹 ").append(line).append("\n\n");
                    }
                    showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Lịch sử đấu giá", sb.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
                showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Lỗi", "Không thể đọc dữ liệu lịch sử.");
            }
        } else {
            showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Thông báo", "Không nhận được phản hồi từ Server.");
        }
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
         
            NetworkClient.stopRealtimeListener();

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
           
            NetworkClient.stopRealtimeListener();

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
            NetworkClient.stopRealtimeListener();

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
   
    @FXML
    public void handleShowHistory(ActionEvent event) {
        if (!Session.isLoggedIn()) {
            showAlert(Alert.AlertType.WARNING, "Yêu cầu đăng nhập", "Bạn cần đăng nhập để xem lịch sử đấu giá của mình!");
            return;
        }

        String currentUser = Session.getCurrentAccount().getUsername();
        StringBuilder historyText = new StringBuilder();

        List<Auction> allAuctions = AppDatabase.getInstance().getAuctions();
        for (Auction a : allAuctions) {
            List<String> bids = AppDatabase.getInstance().getBidHistory(a.getId());
            for (String bid : bids) {
                if (bid.contains(currentUser)) {
                    historyText.append("Sản phẩm '").append(a.getItem().getName()).append("': ").append(bid).append("\n");
                }
            }
        }

        if (historyText.length() == 0) {
            showAlert(Alert.AlertType.INFORMATION, "Lịch sử của bạn", "Bạn chưa tham gia đấu giá sản phẩm nào.");
        } else {
            showAlert(Alert.AlertType.INFORMATION, "Lịch sử đấu giá của: " + currentUser, historyText.toString());
        }
    }
}