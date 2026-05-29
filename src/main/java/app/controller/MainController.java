package app.controller;

import java.io.IOException;
import java.util.ArrayList; 
import java.util.List;
import java.util.stream.Collectors;


import com.google.gson.Gson;

import app.database.AppDatabase;
import app.database.Session;
import app.model.Art;
import app.model.Auction;
import app.model.AuctionProposalRequest;
import app.model.BidTransaction;
import app.model.Bidder;
import app.model.Electronics;
import app.network.ClientConnection;
import app.network.NetworkClient;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform; 
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField; 
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainController {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final double[] QUICK_BID_INCREMENTS = { 500, 1000, 2000, 5000 };
    private static List<Auction> demoAuctions = null;

    @FXML private FlowPane itemContainer;
    @FXML private Button loginButton;
    @FXML private Button signUpButton;
    @FXML private Button btnAddProduct; 
    @FXML private Button btnLogout;     
    @FXML private TextField searchField;

    private List<Timeline> activeTimelines = new ArrayList<>();

    @FXML
    private Button loginButton;

    @FXML
    private Button signUpButton;

    @FXML
    private Button requestProductButton;

    @FXML
    private Button createAuctionButton;

    @FXML private Button logoutButton;
    @FXML private Button btnAll, btnArt, btnElec, btnVeh;
    private String currentFilterType = null;

    @FXML
    private TextField searchField;

    private final AppDatabase database = AppDatabase.getInstance();

    private final Gson gson = new com.google.gson.GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new com.google.gson.TypeAdapter<LocalDateTime>() {
                @Override
                public void write(com.google.gson.stream.JsonWriter out, LocalDateTime value)
                        throws java.io.IOException {
                    out.value(value != null ? value.toString() : null);
                }

                @Override
                public LocalDateTime read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
                    return LocalDateTime.parse(in.nextString());
                }
            }).create();

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
            System.out.println("Giao diện nhận được thông báo cập nhật giá cho: " + auctionId);
            handleShowAll(null);
        });

        ClientConnection.getInstance().setMessageListener(message -> {
        
            if (message != null && message.startsWith("AUCTION_CLOSED")) {
                String[] parts = message.split("\\|");
                if (parts.length >= 4) {
                    String auctionId = parts[1];
                    String winner = parts[2];
                    String price = parts[3];

                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("🎉 Thông báo chốt đơn");
                        alert.setHeaderText("Phiên đấu giá " + auctionId + " đã kết thúc!");
                        
                        if (winner.equals("null") || winner.isEmpty()) {
                            alert.setContentText("Rất tiếc, không có ai đặt giá cho sản phẩm này.");
                        } else {
                            alert.setContentText("Người chiến thắng: " + winner + "\nVới mức giá: " + price + " USD");
                        }
                        alert.showAndWait();
                        handleShowAll(null);
                    });
                }
            }
           
            else if (message != null && message.startsWith("TIME_EXTENDED")) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("⏱️ Gia hạn chống bắn tỉa");
                    alert.setHeaderText("Phiên đấu giá đang rất nóng!");
                    alert.setContentText("Có người vừa đặt giá ở những giây cuối cùng. Hệ thống đã tự động gia hạn thêm 1 phút!");
                    alert.show(); 
                    handleShowAll(null); 
                });
            }
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
    public void login() {
        openScene("/app/Login.fxml");
    }

    @FXML
    public void signUp() {
        openScene("/app/Signup.fxml");
    }

    @FXML
    public void handleSearch(ActionEvent event) {
        String keyword = searchField.getText().toLowerCase();
        List<Auction> filtered = AppDatabase.getInstance().getAuctions().stream()
                .filter(a -> a.getItem().getName().toLowerCase().contains(keyword) ||
                             a.getItem().getDescription().toLowerCase().contains(keyword))
                .collect(Collectors.toList());
        loadAuctionsToUI(filtered);
    }

    private void loadAuctionsToUI(List<Auction> auctions) {
        for (Timeline t : activeTimelines) {
            t.stop();
        }
        activeTimelines.clear();
        itemContainer.getChildren().clear(); 

    public void handleRequestProduct() {
        User user = Session.getCurrentUser();
        if (!(user instanceof Bidder)) {
            showAlert(Alert.AlertType.WARNING, "Không đủ quyền", "Chỉ tài khoản Bidder được gửi yêu cầu sản phẩm mới.");
            return;
        }

        // Bước 1: Xác nhận thông tin cá nhân của Bidder trước khi tạo yêu cầu.
        if (!confirmPersonalInfo((Bidder) user)) {
            return;
        }

        // Bước 2: Mở form yêu cầu sản phẩm chi tiết.
        showProductRequestForm((Bidder) user);
    }

            Label timeLbl = new Label();
            timeLbl.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");

            TextField txtBidAmount = new TextField();
            txtBidAmount.setPromptText("Nhập giá bạn muốn đặt...");
            txtBidAmount.setStyle("-fx-background-radius: 5; -fx-border-radius: 5; -fx-border-color: #cbd5e1;");
            txtBidAmount.setMaxWidth(Double.MAX_VALUE);
            if (auction.getRemainingTime() <= 0) {
                txtBidAmount.setDisable(true);
            }

            Button bidBtn = new Button("Đặt giá ngay");
            bidBtn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 5;");
            bidBtn.setMaxWidth(Double.MAX_VALUE);
            bidBtn.setOnAction(e -> handleBid(auction, txtBidAmount)); 
            
            long[] remainingSeconds = { auction.getRemainingTime() };
            Runnable updateLabel = () -> {
                if (remainingSeconds[0] <= 0) {
                    timeLbl.setText("⏳ Đã kết thúc");
                    bidBtn.setDisable(true); 
                    bidBtn.setStyle("-fx-background-color: #9ca3af; -fx-text-fill: white; -fx-background-radius: 5;");
                    txtBidAmount.setDisable(true); 
                } else {
                    long hours = remainingSeconds[0] / 3600;
                    long minutes = (remainingSeconds[0] % 3600) / 60;
                    long seconds = remainingSeconds[0] % 60;
                    timeLbl.setText(String.format("⏳ Còn lại: %02d:%02d:%02d", hours, minutes, seconds));
                }
            };

            updateLabel.run(); 

            if (remainingSeconds[0] > 0) {
                Timeline timeline = new Timeline(
                    new KeyFrame(Duration.seconds(1), event -> {
                        remainingSeconds[0]--;
                        updateLabel.run();
                    })
                );
                timeline.setCycleCount(Animation.INDEFINITE);
                timeline.play();
                activeTimelines.add(timeline); 
            }
            
            Button historyBtn = new Button("Xem lịch sử đặt giá");
            historyBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 5;");
            historyBtn.setMaxWidth(Double.MAX_VALUE);
            historyBtn.setOnAction(e -> handleViewHistory(auction));

            card.getChildren().addAll(nameLbl, priceLbl, timeLbl, txtBidAmount, bidBtn, historyBtn);
            itemContainer.getChildren().add(card);
        }
    }

    private void handleBid(Auction auction, TextField txtBidAmount) {
        if (!app.database.Session.isLoggedIn()) {
            showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Cảnh báo", "Bạn cần đăng nhập để đặt giá!");
            return;
        }

        Auction latestAuction = AppDatabase.getInstance().findAuctionById(auction.getId());
        if (latestAuction == null) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy phiên đấu giá!");
            return;
        }

        String inputText = txtBidAmount.getText().trim();
        if (inputText.isEmpty()) {
            showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Thông báo", "Vui lòng nhập số tiền bạn muốn đấu giá!");
            return;
        }

        double newPrice = 0;
        try {
            newPrice = Double.parseDouble(inputText);
        } catch (NumberFormatException e) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Lỗi nhập liệu", "Số tiền nhập vào phải là số hợp lệ!");
            return;
        }

        if (newPrice <= latestAuction.getCurrentHighestPrice()) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Giá quá thấp", "Giá đặt phải lớn hơn mức giá hiện tại (" + latestAuction.getCurrentHighestPrice() + " USD)!");
            return;
        }

        String currentUsername = app.database.Session.getCurrentAccount().getUsername();

        app.model.BidTransaction bid = new app.model.BidTransaction(
                java.util.UUID.randomUUID().toString(), 
                latestAuction.getId(), 
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
            showAlert(javafx.scene.control.Alert.AlertType.INFORMATION, "Thành công", "Đã đặt giá " + newPrice + " USD cho sản phẩm: " + latestAuction.getItem().getName());
            txtBidAmount.clear(); 
        } else if ("FAIL_INVALID_BIDDER".equals(response)) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Thất bại", "Thông tin người đặt giá không hợp lệ.");
        } else if ("FAIL_NOT_RUNNING".equals(response)) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Thất bại", "Phiên đấu giá hiện tại không hoạt động.");
        } else if ("FAIL_FINISHED".equals(response)) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Thất bại", "Phiên đấu giá đã kết thúc.");
        } else if ("FAIL_TOO_LOW".equals(response)) {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Thất bại", "Giá đặt phải cao hơn giá hiện tại.");
        } else {
            showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Lỗi kết nối", "Từ chối đặt giá hoặc lỗi hệ thống mạng!");
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
                    return;
                }

           
                javafx.scene.chart.NumberAxis xAxis = new javafx.scene.chart.NumberAxis();
                xAxis.setLabel("Lượt đặt giá");
                xAxis.setForceZeroInRange(false);
                xAxis.setTickUnit(1); 
                xAxis.setMinorTickCount(0);

               
                javafx.scene.chart.NumberAxis yAxis = new javafx.scene.chart.NumberAxis();
                yAxis.setLabel("Mức giá (USD)");
                yAxis.setForceZeroInRange(false);

               
                javafx.scene.chart.LineChart<Number, Number> lineChart = new javafx.scene.chart.LineChart<>(xAxis, yAxis);
                lineChart.setTitle("📈 Biểu đồ lịch sử giá: " + auction.getItem().getName());
                lineChart.setLegendVisible(false);

                javafx.scene.chart.XYChart.Series<Number, Number> series = new javafx.scene.chart.XYChart.Series<>();

                int turn = 1;
                for (int i = history.size() - 1; i >= 0; i--) {
                    String line = history.get(i);
                    try {
                       
                        String[] parts = line.split("đã đặt");
                        if (parts.length > 1) {
                            String priceString = parts[1].trim().split(" ")[0]; // Lấy "1500.0"
                            double priceVal = Double.parseDouble(priceString);
                            series.getData().add(new javafx.scene.chart.XYChart.Data<>(turn++, priceVal));
                        }
                    } catch (Exception ex) {
                    
                    }
                }

                lineChart.getData().add(series);

             
                javafx.scene.Scene chartScene = new javafx.scene.Scene(lineChart, 700, 450);
                chartScene.getStylesheets().add("data:text/css," + 
                    ".chart-series-line { -fx-stroke: #2563eb; -fx-stroke-width: 3px; } " +
                    ".chart-line-symbol { -fx-background-color: #f59e0b, white; -fx-background-radius: 5px; }");
                
                javafx.stage.Stage chartStage = new javafx.stage.Stage();
                chartStage.setTitle("Biểu đồ biến động giá");
                chartStage.setScene(chartScene);
                chartStage.show();

            } catch (Exception e) {
                e.printStackTrace();
                showAlert(javafx.scene.control.Alert.AlertType.ERROR, "Lỗi", "Không thể hiển thị biểu đồ lịch sử.");
            }
        } else {
            showAlert(javafx.scene.control.Alert.AlertType.WARNING, "Thông báo", "Không nhận được phản hồi từ Server.");
        }
    }

    @FXML
    void handleLogout(ActionEvent event) {
        Session.clear();
        openScene("/app/Main.fxml");
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
