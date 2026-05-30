package app.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import com.google.gson.Gson;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import app.database.AppDatabase;
import app.database.Session;
import app.model.AccountRole;
import app.model.Admin;
import app.model.Art;
import app.model.Auction;
import app.model.AuctionProposalRequest;
import app.model.BidTransaction;
import app.model.Bidder;
import app.model.Electronics;
import app.model.Item;
import app.model.Message;
import app.model.Seller;
import app.model.User;
import app.model.Vehicle;
import app.network.ClientConnection;
import app.network.NetworkConfig;
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
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainController {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final double[] QUICK_BID_INCREMENTS = { 500, 1000, 2000, 5000 };
    private static List<Auction> demoAuctions = null;

    @FXML
    private FlowPane itemContainer;

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
        ClientConnection.getInstance().connect(NetworkConfig.HOST, NetworkConfig.PORT);
        applySessionState();
        loadAuctionItems();
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
        itemContainer.getChildren().clear();
        auctions.forEach(this::createAuctionCard);
    }

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

    private boolean confirmPersonalInfo(Bidder bidder) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận thông tin cá nhân");
        confirm.setHeaderText("Vui lòng xác nhận thông tin cá nhân trước khi gửi yêu cầu sản phẩm");
        confirm.setContentText(
                "ID: " + bidder.getId() + "\n" +
                        "Tên đăng nhập: " + bidder.getUsername() + "\n\n" +
                        "Thông tin này sẽ được gửi kèm yêu cầu.");
        Optional<ButtonType> result = confirm.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    // Form yêu cầu sản phẩm
    private void showProductRequestForm(Bidder bidder) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Yêu cầu sản phẩm");
        dialog.setHeaderText("Bidder " + bidder.getUsername() + " - Nhập thông tin sản phẩm muốn đấu giá");

        ComboBox<String> typeComboBox = new ComboBox<>();
        typeComboBox.getItems().addAll("ART", "ELECTRONICS", "VEHICLE");
        typeComboBox.setValue("ART");
        typeComboBox.setMaxWidth(Double.MAX_VALUE);

        TextField nameField = new TextField();
        TextField descriptionField = new TextField();
        TextField desiredPriceField = new TextField();
        TextField startTimeField = new TextField();
        TextField durationField = new TextField();

        nameField.setPromptText("Tên sản phẩm");
        descriptionField.setPromptText("Mô tả ngắn");
        desiredPriceField.setPromptText("Số tiền mong muốn (USD)");
        startTimeField.setPromptText("yyyy-MM-dd HH:mm (Có thể để trống)");
        durationField.setPromptText("Số phút (Có thể để trống)");

        Label flexibleScheduleNote = new Label(
                "Chú thích: " + AuctionProposalRequest.FLEXIBLE_SCHEDULE_NOTE + ".");
        flexibleScheduleNote.setWrapText(true);
        flexibleScheduleNote.setStyle("-fx-font-style: italic; -fx-text-fill: #7F8C8D;");
        flexibleScheduleNote.setMaxWidth(360);

        VBox attributesContainer = new VBox();
        Map<String, Control> dynamicControls = new HashMap<>();
        setupDynamicAttributes(typeComboBox, attributesContainer, dynamicControls, dialog);

        GridPane grid = createFormGrid();
        grid.addRow(0, new Label("Loại sản phẩm:"), typeComboBox);
        grid.addRow(1, new Label("Tên sản phẩm:"), nameField);
        grid.addRow(2, new Label("Mô tả:"), descriptionField);
        grid.add(attributesContainer, 0, 3, 2, 1);
        grid.addRow(4, new Label("Số tiền mong muốn:"), desiredPriceField);
        grid.addRow(5, new Label("Thời lượng (phút):"), durationField);
        grid.addRow(6, new Label("Bắt đầu:"), startTimeField);
        grid.add(flexibleScheduleNote, 0, 7, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(createSubmitButton("Gửi yêu cầu"), ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return;
        }

        try {
            String type = typeComboBox.getValue();
            String productName = requireText(nameField, "Vui lòng nhập tên sản phẩm.");
            String productDescription = requireText(descriptionField, "Vui lòng nhập mô tả sản phẩm.");
            double desiredPrice = parsePositiveDouble(desiredPriceField.getText(), "Số tiền mong muốn phải lớn hơn 0.");

            // Cho phép Bidder để trống thời gian
            String startTimeRaw = startTimeField.getText() == null ? "" : startTimeField.getText().trim();
            String durationRaw = durationField.getText() == null ? "" : durationField.getText().trim();

            LocalDateTime startTime = startTimeRaw.isEmpty() ? null : parseStartTime(startTimeRaw);
            long durationMinutes = durationRaw.isEmpty() ? 0L
                    : parsePositiveLong(durationRaw, "Thời lượng phải là số phút lớn hơn 0.");

            if (startTime == null || durationMinutes <= 0) {
                startTime = null;
                durationMinutes = 0L;
            }

            Map<String, String> attributes = buildAttributesForType(
                    type,
                    dynamicControls);

            AuctionProposalRequest request = bidder.createProductRequest(
                    type,
                    productName,
                    productDescription,
                    attributes,
                    desiredPrice,
                    durationMinutes,
                    startTime);

            sendMessageToServer(new Message("PRODUCT_REQUEST", gson.toJson(request)));

            String confirmation = "Yêu cầu sản phẩm đã được gửi cho Seller. " +
                    "Seller sẽ kiểm tra và chỉnh sửa thông tin (Trừ thông tin cá nhân của bạn).";
            if (request.isFlexibleSchedule()) {
                confirmation += "\n\nChú thích: " + request.getScheduleNote() + ".";
            }
            showAlert(Alert.AlertType.INFORMATION, "Đã gửi", confirmation);
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Dữ liệu chưa hợp lệ", e.getMessage());
        }
    }

    private String getControlText(Map<String, Control> controls, String key) {
        Control c = controls.get(key);
        if (c instanceof TextField) return ((TextField) c).getText();
        if (c instanceof ComboBox) return (String) ((ComboBox<String>) c).getValue();
        if (c instanceof CheckBox) return ((CheckBox) c).isSelected() ? "Có" : "Không";
        return "";
    }

    private void setupDynamicAttributes(ComboBox<String> typeComboBox, VBox attributesContainer, Map<String, Control> dynamicControls, Dialog<?> dialog) {
        Runnable refreshAttributeLabels = () -> {
            String type = typeComboBox.getValue();
            attributesContainer.getChildren().clear();
            dynamicControls.clear();
            GridPane grid = createFormGrid();
            
            if ("ART".equals(type)) {
                TextField author = new TextField(); author.setPromptText("Tên tác giả");
                TextField year = new TextField(); year.setPromptText("VD: 1998");
                dynamicControls.put("art_author", author);
                dynamicControls.put("art_year", year);
                grid.addRow(0, new Label("Tác giả:"), author);
                grid.addRow(1, new Label("Năm sáng tác:"), year);
            } else if ("ELECTRONICS".equals(type)) {
                TextField warranty = new TextField(); warranty.setPromptText("VD: 12");
                ComboBox<String> condition = new ComboBox<>(); condition.getItems().addAll("Mới", "Cũ"); condition.setValue("Mới");
                TextField purchaseDate = new TextField(); purchaseDate.setPromptText("VD: 2023");
                TextField desc = new TextField(); desc.setPromptText("Mô tả tình trạng");
                CheckBox isRepaired = new CheckBox("Đã sửa chữa?"); isRepaired.getStyleClass().add("custom-checkbox");
                TextField repairDate = new TextField(); repairDate.setPromptText("VD: 01/2024");
                TextField repairParts = new TextField(); repairParts.setPromptText("VD: Màn hình, Pin");
                
                dynamicControls.put("elec_warranty", warranty);
                dynamicControls.put("condition", condition);
                dynamicControls.put("purchaseDate", purchaseDate);
                dynamicControls.put("elec_desc", desc);
                dynamicControls.put("isRepaired", isRepaired);
                dynamicControls.put("repairDate", repairDate);
                dynamicControls.put("repairParts", repairParts);
                
                grid.addRow(0, new Label("Bảo hành (tháng):"), warranty);
                grid.addRow(1, new Label("Tình trạng:"), condition);
                
                Runnable updateElec = () -> {
                    grid.getChildren().removeIf(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) >= 2);
                    if ("Cũ".equals(condition.getValue())) {
                        grid.addRow(2, new Label("Thời gian mua:"), purchaseDate);
                        grid.addRow(3, new Label("Mô tả tình trạng:"), desc);
                        grid.addRow(4, new Label(""), isRepaired);
                        if (isRepaired.isSelected()) {
                            grid.addRow(5, new Label("Ngày sửa chữa:"), repairDate);
                            grid.addRow(6, new Label("Phụ tùng sửa:"), repairParts);
                        }
                    }
                    if (dialog != null && dialog.getDialogPane() != null && dialog.getDialogPane().getScene() != null) dialog.getDialogPane().getScene().getWindow().sizeToScene();
                };
                condition.setOnAction(e -> updateElec.run());
                isRepaired.setOnAction(e -> updateElec.run());
                updateElec.run();
            } else if ("VEHICLE".equals(type)) {
                TextField brand = new TextField(); brand.setPromptText("VD: Toyota");
                ComboBox<String> condition = new ComboBox<>(); condition.getItems().addAll("Mới", "Cũ"); condition.setValue("Mới");
                TextField purchaseDate = new TextField(); purchaseDate.setPromptText("VD: 2023");
                TextField mileage = new TextField(); mileage.setPromptText("VD: 25000");
                CheckBox isRepaired = new CheckBox("Đã sửa chữa?"); isRepaired.getStyleClass().add("custom-checkbox");
                TextField repairDate = new TextField(); repairDate.setPromptText("VD: 01/2024");
                TextField repairParts = new TextField(); repairParts.setPromptText("VD: Lốp, Ắc quy");
                
                dynamicControls.put("veh_brand", brand);
                dynamicControls.put("condition", condition);
                dynamicControls.put("purchaseDate", purchaseDate);
                dynamicControls.put("veh_mileage", mileage);
                dynamicControls.put("isRepaired", isRepaired);
                dynamicControls.put("repairDate", repairDate);
                dynamicControls.put("repairParts", repairParts);
                
                grid.addRow(0, new Label("Hãng:"), brand);
                grid.addRow(1, new Label("Tình trạng:"), condition);
                
                Runnable updateVeh = () -> {
                    grid.getChildren().removeIf(node -> GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) >= 2);
                    if ("Cũ".equals(condition.getValue())) {
                        grid.addRow(2, new Label("Thời gian mua:"), purchaseDate);
                        grid.addRow(3, new Label("Số km đã đi:"), mileage);
                        grid.addRow(4, new Label(""), isRepaired);
                        if (isRepaired.isSelected()) {
                            grid.addRow(5, new Label("Ngày sửa chữa:"), repairDate);
                            grid.addRow(6, new Label("Phụ tùng sửa:"), repairParts);
                        }
                    }
                    if (dialog != null && dialog.getDialogPane() != null && dialog.getDialogPane().getScene() != null) dialog.getDialogPane().getScene().getWindow().sizeToScene();
                };
                condition.setOnAction(e -> updateVeh.run());
                isRepaired.setOnAction(e -> updateVeh.run());
                updateVeh.run();
            }
            attributesContainer.getChildren().add(grid);
        };
        typeComboBox.setOnAction(e -> refreshAttributeLabels.run());
        refreshAttributeLabels.run();
    }

    private Map<String, String> buildAttributesForType(String type, Map<String, Control> controls) {
        Map<String, String> attributes = new HashMap<>();
        switch (type) {
            case "ART":
                String artist = getControlText(controls, "art_author");
                String year = getControlText(controls, "art_year");
                if (artist.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập tên tác giả.");
                attributes.put("artist", artist);
                if (!year.isEmpty()) {
                    parseInteger(year, "Năm sáng tác phải là số.");
                    attributes.put("year", year);
                }
                break;
            case "ELECTRONICS":
                String warranty = getControlText(controls, "elec_warranty");
                if (warranty.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập số tháng bảo hành.");
                parseInteger(warranty, "Bảo hành phải là số tháng.");
                attributes.put("warrantyMonths", warranty);
                String elecCond = getControlText(controls, "condition");
                String elecPurch = getControlText(controls, "purchaseDate");
                String elecDesc = getControlText(controls, "elec_desc");
                attributes.put("condition", "Mới".equals(elecCond) ? "Mới" : ("Cũ" + (elecDesc.isEmpty() ? "" : " - " + elecDesc)));
                attributes.put("purchaseDate", "Mới".equals(elecCond) ? "" : elecPurch);
                String elecIsRepaired = getControlText(controls, "isRepaired");
                attributes.put("isRepaired", "Cũ".equals(elecCond) ? elecIsRepaired : "Không");
                if ("Cũ".equals(elecCond) && "Có".equals(elecIsRepaired)) {
                    attributes.put("repairDate", getControlText(controls, "repairDate"));
                    attributes.put("repairedParts", getControlText(controls, "repairParts"));
                }
                break;
            case "VEHICLE":
                String brand = getControlText(controls, "veh_brand");
                if (brand.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập hãng xe.");
                attributes.put("brand", brand);
                String vehCond = getControlText(controls, "condition");
                attributes.put("condition", vehCond);
                if ("Cũ".equals(vehCond)) {
                    String mileage = getControlText(controls, "veh_mileage");
                    if (mileage.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập số km.");
                    parseInteger(mileage, "Số km phải là số.");
                    attributes.put("mileage", mileage);
                    attributes.put("purchaseDate", getControlText(controls, "purchaseDate"));
                    String isRepaired = getControlText(controls, "isRepaired");
                    attributes.put("isRepaired", isRepaired);
                    if ("Có".equals(isRepaired)) {
                        attributes.put("repairDate", getControlText(controls, "repairDate"));
                        attributes.put("repairedParts", getControlText(controls, "repairParts"));
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ.");
        }
        return attributes;
    }

    @FXML
    public void handleCreateAuction() {
        User user = Session.getCurrentUser();
        if (!(user instanceof Seller)) {
            showAlert(Alert.AlertType.WARNING, "Không đủ quyền",
                    "Chỉ tài khoản Seller được tạo sản phẩm và phiên đấu giá mới.");
            return;
        }
        openScene("/app/CreateAuction.fxml");
    }

    private void applySessionState() {
        User user = Session.getCurrentUser();
        boolean loggedIn = user != null;
        boolean isBidder = loggedIn && user.getRole() == AccountRole.BIDDER;
        boolean isSeller = loggedIn && user.getRole() == AccountRole.SELLER;

        setButtonVisible(loginButton, !loggedIn);
        setButtonVisible(signUpButton, !loggedIn);
        setButtonVisible(logoutButton, loggedIn);
        setButtonVisible(requestProductButton, isBidder);
        setButtonVisible(createAuctionButton, isSeller);
    }

    private void setButtonVisible(Button button, boolean visible) {
        if (button != null) {
            button.setVisible(visible);
            button.setManaged(visible);
        }
    }

    @FXML void filterAll(ActionEvent event) { setActiveFilter(btnAll, null); }
    @FXML void filterArt(ActionEvent event) { setActiveFilter(btnArt, "ART"); }
    @FXML void filterElectronics(ActionEvent event) { setActiveFilter(btnElec, "ELEC"); }
    @FXML void filterVehicle(ActionEvent event) { setActiveFilter(btnVeh, "VEHICLE"); }

    private void setActiveFilter(Button activeBtn, String type) {
        if (btnAll != null) btnAll.getStyleClass().setAll("button", "sidebar-btn");
        if (btnArt != null) btnArt.getStyleClass().setAll("button", "sidebar-btn");
        if (btnElec != null) btnElec.getStyleClass().setAll("button", "sidebar-btn");
        if (btnVeh != null) btnVeh.getStyleClass().setAll("button", "sidebar-btn");
        if (activeBtn != null) activeBtn.getStyleClass().setAll("button", "sidebar-btn-active");
        currentFilterType = type;
        loadAuctionItems();
    }

    private void loadAuctionItems() {
        itemContainer.getChildren().clear();
        User user = Session.getCurrentUser();

        if (user == null) {
            if (demoAuctions == null) {
                demoAuctions = new ArrayList<>();
                demoAuctions.add(new Auction("DEMO_A01",
                        new Art("I_A01", "Tranh phố cổ", "Tranh sơn dầu Hà Nội", 1200.0, "Nguyễn Xuân Phái", 1980),
                        LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusHours(2), 1200.0, "RUNNING"));
                demoAuctions.add(new Auction("DEMO_A02",
                        new Art("I_A02", "Tượng Gỗ Lũa", "Tượng nghệ thuật điêu khắc", 500.0, "Nghệ nhân Việt", 2023),
                        LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusHours(1), 500.0, "RUNNING"));
                demoAuctions.add(new Auction("DEMO_E01",
                        new Electronics("I_E01", "iPhone 15 Pro", "8/128", 1000.0, 12, "Mới", "", "Không", "", ""),
                        LocalDateTime.now().minusMinutes(20), LocalDateTime.now().plusHours(3), 1000.0, "RUNNING"));
                demoAuctions.add(new Auction("DEMO_E02",
                        new Electronics("I_E02", "MacBook M4", "8/512", 2600.0, 24, "Mới", "", "Không", "", ""),
                        LocalDateTime.now().minusMinutes(15), LocalDateTime.now().plusHours(3), 2600.0, "RUNNING"));
            }
            demoAuctions.stream()
                .filter(a -> currentFilterType == null || (
                    (currentFilterType.equals("ART") && a.getItem() instanceof Art) ||
                    (currentFilterType.equals("ELEC") && a.getItem() instanceof Electronics) ||
                    (currentFilterType.equals("VEHICLE") && a.getItem() instanceof Vehicle)
                ))
                .forEach(this::createAuctionCard);
        } else {
            database.getAuctions().stream()
                .filter(a -> currentFilterType == null || (
                    (currentFilterType.equals("ART") && a.getItem() instanceof Art) ||
                    (currentFilterType.equals("ELEC") && a.getItem() instanceof Electronics) ||
                    (currentFilterType.equals("VEHICLE") && a.getItem() instanceof Vehicle)
                ))
                .forEach(this::createAuctionCard);
        }
    }

    private void createAuctionCard(Auction auction) {
        Item item = auction.getItem();

        VBox card = new VBox(8);
        card.getStyleClass().add("item-card");
        card.setPrefWidth(250);

        Label nameLabel = new Label(item.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        String typeStr = "Sản phẩm";
        String extraInfo = "";
        if (item instanceof Art) {
            typeStr = "Loại: Nghệ thuật";
            extraInfo = "Mô tả: " + item.getDescription();
        } else if (item instanceof Electronics) {
            typeStr = "Loại: Điện tử";
            extraInfo = "Tình trạng: " + ((Electronics) item).getCondition();
        } else if (item instanceof Vehicle) {
            typeStr = "Loại: Phương tiện";
            extraInfo = "Tình trạng: " + ((Vehicle) item).getCondition();
        }
        
        Label typeLabel = new Label(typeStr);
        Label extraLabel = new Label(extraInfo);
        Label startLabel = new Label("Bắt đầu: " + auction.getStartTime().format(DATE_TIME_FORMATTER));

        Label timerLabel = new Label();
        timerLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            long remaining = auction.getRemainingTime();
            if (remaining > 0) {
                long hours = remaining / 3600;
                long minutes = (remaining % 3600) / 60;
                long seconds = remaining % 60;
                timerLabel.setText(String.format("Còn lại: %02d:%02d:%02d", hours, minutes, seconds));
            } else {
                timerLabel.setText("Phiên đấu giá đã kết thúc");
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        card.getChildren().addAll(nameLabel, typeLabel, extraLabel, startLabel, timerLabel);
        
        card.setOnMouseClicked(e -> showAuctionDetails(auction));

        itemContainer.getChildren().add(card);
    }

    private void showAuctionDetails(Auction auction) {
        Item item = auction.getItem();
        User user = Session.getCurrentUser();
        boolean isBidder = user != null && user.getRole() == AccountRole.BIDDER;
        boolean isGuest = user == null;
        boolean canBid = isBidder || isGuest;
        boolean isAdmin = user != null && user.getRole() == AccountRole.ADMIN;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết phiên đấu giá");
        
        VBox content = new VBox(10);
        content.setPadding(new javafx.geometry.Insets(15));
        
        Label nameLabel = new Label("Tên SP: " + item.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Label descLabel = new Label("Mô tả: " + item.getDescription());
        Label priceLabel = new Label("Giá hiện tại: " + auction.getCurrentHighestPrice() + " USD");
        priceLabel.setStyle("-fx-text-fill: #E67E22; -fx-font-weight: bold;");
        Label statusLabel = new Label("Trạng thái: " + auction.getStatus());
        Label startLabel = new Label("Bắt đầu: " + auction.getStartTime().format(DATE_TIME_FORMATTER));
        Label durationLabel = new Label("Thời lượng: " + java.time.Duration.between(auction.getStartTime(), auction.getStopTime()).toMinutes() + " phút");
        
        VBox specificDetails = new VBox(5);
        if (item instanceof Art) {
            Art art = (Art) item;
            specificDetails.getChildren().addAll(new Label("Nghệ sĩ: " + art.getArtist()), new Label("Năm sáng tác: " + art.getCreationYear()));
        } else if (item instanceof Electronics) {
            Electronics elec = (Electronics) item;
            specificDetails.getChildren().addAll(
                new Label("Bảo hành: " + elec.getWarrantyMonths() + " tháng"),
                new Label("Tình trạng: " + elec.getCondition())
            );
            if ("Cũ".equals(elec.getCondition())) {
                specificDetails.getChildren().add(new Label("Thời gian mua: " + elec.getPurchaseDate()));
                specificDetails.getChildren().add(new Label("Đã sửa chữa: " + elec.getIsRepaired()));
                if ("Có".equals(elec.getIsRepaired())) {
                    specificDetails.getChildren().addAll(
                        new Label("Ngày sửa chữa: " + elec.getRepairDate()),
                        new Label("Phụ tùng thay thế: " + elec.getRepairedParts())
                    );
                }
            }
        } else if (item instanceof Vehicle) {
            Vehicle veh = (Vehicle) item;
            specificDetails.getChildren().addAll(
                new Label("Hãng: " + veh.getBrand()),
                new Label("Tình trạng: " + veh.getCondition())
            );
            if ("Cũ".equals(veh.getCondition())) {
                specificDetails.getChildren().addAll(
                    new Label("Thời gian mua: " + veh.getPurchaseDate()),
                    new Label("Số km đã đi: " + veh.getMileage() + " km"),
                    new Label("Đã sửa chữa: " + veh.getIsRepaired())
                );
                if ("Có".equals(veh.getIsRepaired())) {
                    specificDetails.getChildren().addAll(
                        new Label("Ngày sửa chữa: " + veh.getRepairDate()),
                        new Label("Phụ tùng thay thế: " + veh.getRepairedParts())
                    );
                }
            }
        }

        Button bidBtn = new Button(canBid ? "Đặt giá" : "Chỉ Bidder được đặt giá");
        bidBtn.getStyleClass().add("btn-primary");
        bidBtn.setMaxWidth(Double.MAX_VALUE);
        bidBtn.setDisable(!canBid);
        bidBtn.setOnAction(e -> {
            dialog.setResult(ButtonType.CANCEL);
            dialog.close();
            handleBidAction(auction, priceLabel);
        });

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Thời gian");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Giá (USD)");
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Lịch sử đấu giá");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Giá đấu");
        for (BidTransaction b : auction.getBidHistory()) {
            series.getData().add(new XYChart.Data<>(b.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")), b.getBidAmount()));
        }
        lineChart.getData().add(series);
        lineChart.setPrefHeight(200);

        Label timerLabel = new Label();
        timerLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-font-size: 14px;");
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            long remaining = auction.getRemainingTime();
            if (remaining > 0) {
                long hours = remaining / 3600;
                long minutes = (remaining % 3600) / 60;
                long seconds = remaining % 60;
                timerLabel.setText(String.format("Thời gian còn lại: %02d:%02d:%02d", hours, minutes, seconds));
            } else {
                timerLabel.setText("Phiên đấu giá đã kết thúc");
                bidBtn.setDisable(true);
            }
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        content.getChildren().addAll(nameLabel, descLabel, priceLabel, statusLabel, startLabel, durationLabel, specificDetails, lineChart, timerLabel, bidBtn);

        if (isAdmin) {
            Button stopBtn = new Button("Ngưng phiên");
            stopBtn.setMaxWidth(Double.MAX_VALUE);
            stopBtn.setOnAction(e -> {
                dialog.setResult(ButtonType.CANCEL);
                dialog.close();
                handleStopAuction(auction);
            });

            Button deleteBtn = new Button("Xóa phiên");
            deleteBtn.setMaxWidth(Double.MAX_VALUE);
            deleteBtn.setOnAction(e -> {
                dialog.setResult(ButtonType.CANCEL);
                dialog.close();
                handleDeleteAuction(auction);
            });

            content.getChildren().addAll(stopBtn, deleteBtn);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.show();
    }

    private void handleBidAction(Auction auction, Label priceLabel) {
        User user = Session.getCurrentUser();
        boolean isDemo = user == null;

        if (!isDemo && !(user instanceof Bidder)) {
            showAlert(Alert.AlertType.WARNING, "Không đủ quyền", "Chỉ tài khoản Bidder được đặt giá.");
            return;
        }

        Item item = auction.getItem();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Đấu giá" + (isDemo ? " (Bản Dùng Thử)" : ""));
        dialog.setHeaderText(
                "Sản phẩm: " + item.getName() + "\nGiá hiện tại: " + auction.getCurrentHighestPrice() + " USD");
        TextField bidAmountField = new TextField();
        bidAmountField.setPromptText("Nhập giá đấu (USD)");
        bidAmountField.setPrefColumnCount(14);

        bidAmountField.setText(String.valueOf(auction.getCurrentHighestPrice()));

        Label quickBidLabel = new Label("Tăng nhanh:");

        HBox quickBidBox = new HBox(8);
        for (double increment : QUICK_BID_INCREMENTS) {
            Button quickBtn = new Button("+" + ((long) increment));
            quickBtn.getStyleClass().add("btn-outline");
            quickBtn.setOnAction(e -> {
                double base;
                try {
                    String current = bidAmountField.getText() == null ? "" : bidAmountField.getText().trim();
                    base = current.isEmpty() ? auction.getCurrentHighestPrice() : Double.parseDouble(current);
                } catch (NumberFormatException ex) {
                    base = auction.getCurrentHighestPrice();
                }
                double next = base + increment;
                if (next <= auction.getCurrentHighestPrice()) {
                    next = auction.getCurrentHighestPrice() + increment;
                }
                bidAmountField.setText(formatBidAmount(next));
            });
            quickBidBox.getChildren().add(quickBtn);
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().addAll(new Label("Giá đấu (USD):"), bidAmountField, quickBidLabel, quickBidBox);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(createSubmitButton("Đặt giá"), ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return;
        }

        try {
            double bidAmount = Double.parseDouble(bidAmountField.getText().trim());
            if (bidAmount <= auction.getCurrentHighestPrice()) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá đặt phải cao hơn hiện tại!");
                return;
            }

            String bidderId = isDemo ? "GUEST" : user.getId();
            BidTransaction transaction = new BidTransaction(
                    UUID.randomUUID().toString(),
                    auction.getId(),
                    bidderId,
                    bidAmount,
                    LocalDateTime.now());

            boolean success = auction.placeBid(transaction);

            if (success) {
                priceLabel.setText("Giá hiện tại: " + auction.getCurrentHighestPrice() + " USD");
                if (!isDemo) {
                    sendBidToServer(transaction);
                }
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đặt giá: " + bidAmount + " USD"
                        + (isDemo ? "\n\nĐây là phiên dùng thử, dữ liệu không được lưu." : ""));
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đặt giá cho phiên này!");
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Chú ý", "Vui lòng nhập số tiền hợp lệ.");
        }
    }

    private void handleStopAuction(Auction auction) {
        if (!isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Không đủ quyền", "Chỉ Admin được ngưng phiên đấu giá.");
            return;
        }

        if (database.stopAuction(auction.getId())) {
            loadAuctionItems();
            showAlert(Alert.AlertType.INFORMATION, "Đã ngưng", "Phiên đấu giá đã được ngưng.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể ngưng phiên đấu giá.");
        }
    }

    private void handleDeleteAuction(Auction auction) {
        if (!isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Không đủ quyền", "Chỉ Admin được xóa phiên đấu giá.");
            return;
        }

        if (database.deleteAuction(auction.getId())) {
            loadAuctionItems();
            showAlert(Alert.AlertType.INFORMATION, "Đã xóa", "Phiên đấu giá đã được xóa.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể xóa phiên đấu giá.");
        }
    }

    private boolean isAdmin() {
        User user = Session.getCurrentUser();
        return user instanceof Admin;
    }

    private Item createItemFromForm(String type, String name, String description, double startingPrice, Map<String, Control> controls) {
        String itemId = "I_" + UUID.randomUUID();
        switch (type) {
            case "ART":
                String artist = getControlText(controls, "art_author");
                String year = getControlText(controls, "art_year");
                if (artist.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập tác giả.");
                return new Art(itemId, name, description, startingPrice, artist, year.isEmpty() ? 0 : parseInteger(year, "Năm sáng tác phải là số."));
            case "ELECTRONICS":
                String warranty = getControlText(controls, "elec_warranty");
                String elecCond = getControlText(controls, "condition");
                String elecPurch = getControlText(controls, "purchaseDate");
                String elecDesc = getControlText(controls, "elec_desc");
                String elecIsRepaired = getControlText(controls, "isRepaired");
                String elecRepairDate = getControlText(controls, "repairDate");
                String elecRepairParts = getControlText(controls, "repairParts");
                if (warranty.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập số tháng bảo hành.");
                String finalElecCond = "Mới".equals(elecCond) ? "Mới" : ("Cũ" + (elecDesc.isEmpty() ? "" : " - " + elecDesc));
                return new Electronics(itemId, name, description, startingPrice, parseInteger(warranty, "Bảo hành phải là số tháng."), finalElecCond, "Mới".equals(elecCond) ? "" : elecPurch, "Cũ".equals(elecCond) ? elecIsRepaired : "Không", ("Cũ".equals(elecCond) && "Có".equals(elecIsRepaired)) ? elecRepairDate : "", ("Cũ".equals(elecCond) && "Có".equals(elecIsRepaired)) ? elecRepairParts : "");
            case "VEHICLE":
                String brand = getControlText(controls, "veh_brand");
                String vehCond = getControlText(controls, "condition");
                String vehPurch = getControlText(controls, "purchaseDate");
                String mileage = getControlText(controls, "veh_mileage");
                String isRepaired = getControlText(controls, "isRepaired");
                String repairDate = getControlText(controls, "repairDate");
                String repairParts = getControlText(controls, "repairParts");
                if (brand.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập hãng xe.");
                if ("Cũ".equals(vehCond) && mileage.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập số km.");
                return new Vehicle(itemId, name, description, startingPrice, brand, "Cũ".equals(vehCond) ? parseInteger(mileage, "Số km phải là số.") : 0, vehCond, "Cũ".equals(vehCond) ? vehPurch : "", "Cũ".equals(vehCond) ? isRepaired : "Không", ("Cũ".equals(vehCond) && "Có".equals(isRepaired)) ? repairDate : "", ("Cũ".equals(vehCond) && "Có".equals(isRepaired)) ? repairParts : "");
            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ.");
        }
    }

    private GridPane createFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        return grid;
    }

    private ButtonType createSubmitButton(String text) {
        return new ButtonType(text, ButtonBar.ButtonData.OK_DONE);
    }

    private String requireText(TextField field, String errorMessage) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    private LocalDateTime parseStartTime(String value) {
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Thời gian bắt đầu phải theo định dạng yyyy-MM-dd HH:mm.");
        }
    }

    private double parsePositiveDouble(String value, String errorMessage) {
        try {
            double parsed = Double.parseDouble(value.trim());
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private long parsePositiveLong(String value, String errorMessage) {
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private int parseInteger(String value, String errorMessage) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private String formatBidAmount(double amount) {
        if (amount == Math.rint(amount)) {
            return String.valueOf((long) amount);
        }
        return String.valueOf(amount);
    }

    private void sendBidToServer(BidTransaction transaction) {
        String jsonData = gson.toJson(transaction);
        sendMessageToServer(new Message("BID", jsonData));
    }

    private void sendMessageToServer(Message message) {
        ClientConnection.getInstance().sendMessage(message);
    }

    @FXML
    void handleLogout(ActionEvent event) {
        Session.clear();
        openScene("/app/Main.fxml");
    }

    private void openScene(String resource) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(resource));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) itemContainer.getScene().getWindow();
            stage.setScene(scene);
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
}
