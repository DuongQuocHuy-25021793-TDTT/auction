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
import app.model.SuspensionLog;
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

    // --- Biến giao diện in-place detail ---
    @FXML private VBox normalSidebar, auctionSidebar;
    @FXML private VBox normalCenterContent, auctionDetailContent;
    @FXML private VBox adminUsersContent, userListContainer;
    @FXML private Button btnManageAccounts, btnHistory, btnBackHistory;
    
    // --- Biến giao diện History ---
    @FXML private VBox historyCenterContent;
    @FXML private HBox adminHistoryTabs;
    @FXML private Button tabAuctionHistory, tabSuspensionHistory;
    @FXML private ComboBox<String> adminSellerComboBox, adminRoleComboBox;
    @FXML private TextField historySearchField;
    @FXML private FlowPane historyContainer;
    @FXML private VBox detailWinnerBox;
    @FXML private Label detailWinnerText, detailBidderHistoryText;
    
    private boolean isHistoryMode = false;
    private int adminHistoryTabMode = 1; // 1: Auction, 2: Suspension

    @FXML private Label detailNameLabel, detailStatusLabel, detailDescLabel, detailPriceLabel, detailBidderLabel, detailTimerLabel;
    @FXML private VBox detailSpecificVBox;
    @FXML private HBox detailActionBox;
    @FXML private LineChart<String, Number> detailChart;
    @FXML private CategoryAxis detailChartX;
    @FXML private NumberAxis detailChartY;

    // --- Biến Auto-Bid & Sidebar ---
    @FXML private VBox bidderSidebar, sellerSidebar;
    @FXML private VBox topBiddersContainer;
    @FXML private TextField customBidField;
    @FXML private Label bidSuccessLabel;
    @FXML private Button btnAutoBid;
    @FXML private Label autoBidStatusLabel;
    @FXML private TextField maxBidField, bidStepField, autoBidDelayField;

    public static class AutoBidConfig {
        public boolean isActivated;
        public double maxBid;
        public double bidStep;
        public int delaySeconds;
        public boolean isWaitingToBid;
        public AutoBidConfig(boolean isActivated, double maxBid, double bidStep, int delaySeconds) {
            this.isActivated = isActivated;
            this.maxBid = maxBid;
            this.bidStep = bidStep;
            this.delaySeconds = delaySeconds;
            this.isWaitingToBid = false;
        }
    }

    private Map<String, AutoBidConfig> autoBidConfigs = new HashMap<>();
    private Auction activeAuction = null;
    private Timeline detailUpdateTimeline = null;
    private Timeline autoBidDelayTimeline = null;
    private int lastAuctionCount = -1;

    @FXML private Button btnTabRunning;
    @FXML private Button btnTabUpcoming;
    private boolean isUpcomingTab = false;

    @FXML private Button btnBackAdmin;
    @FXML private Button btnAdminFilterAll;
    @FXML private Button btnAdminFilterSeller;
    @FXML private Button btnAdminFilterBidder;
    private String adminUserFilter = "ALL";

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

        Timeline mainRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), ev -> {
            if (activeAuction == null) {
                User user = Session.getCurrentUser();
                int currentDbCount = (user == null) ? (demoAuctions != null ? demoAuctions.size() : 0) : database.getAuctions().size();
                if (currentDbCount != lastAuctionCount) {
                    loadAuctionItems();
                }
            }
        }));
        mainRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        mainRefreshTimeline.play();
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
        boolean isAdmin = loggedIn && user.getRole() == AccountRole.ADMIN;

        setButtonVisible(loginButton, !loggedIn);
        setButtonVisible(signUpButton, !loggedIn);
        setButtonVisible(logoutButton, loggedIn);
        setButtonVisible(requestProductButton, isBidder);
        setButtonVisible(createAuctionButton, isSeller);
        setButtonVisible(btnManageAccounts, isAdmin);
        
        if (!isHistoryMode && !adminUsersContent.isVisible()) {
            setButtonVisible(btnHistory, loggedIn);
            setButtonVisible(btnBackHistory, false);
            setButtonVisible(btnAll, true);
            setButtonVisible(btnArt, true);
            setButtonVisible(btnElec, true);
            setButtonVisible(btnVeh, true);
        }
        setButtonVisible(btnBackAdmin, false);
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

    @FXML
    public void showAdminUsers(ActionEvent event) {
        isHistoryMode = false;
        if (detailUpdateTimeline != null) detailUpdateTimeline.stop();
        activeAuction = null;
        
        normalSidebar.setVisible(true); normalSidebar.setManaged(true);
        auctionSidebar.setVisible(false); auctionSidebar.setManaged(false);
        
        normalCenterContent.setVisible(false); normalCenterContent.setManaged(false);
        auctionDetailContent.setVisible(false); auctionDetailContent.setManaged(false);
        historyCenterContent.setVisible(false); historyCenterContent.setManaged(false);
        adminUsersContent.setVisible(true); adminUsersContent.setManaged(true);
        
        setButtonVisible(btnAll, false);
        setButtonVisible(btnArt, false);
        setButtonVisible(btnElec, false);
        setButtonVisible(btnVeh, false);
        setButtonVisible(btnHistory, false);
        setButtonVisible(btnManageAccounts, false);
        setButtonVisible(btnBackHistory, false);
        setButtonVisible(btnBackAdmin, true);
        
        setButtonVisible(btnAdminFilterAll, true);
        setButtonVisible(btnAdminFilterSeller, true);
        setButtonVisible(btnAdminFilterBidder, true);
        
        loadUsersToAdminView();
    }

    @FXML
    public void hideAdminUsers(ActionEvent event) {
        adminUsersContent.setVisible(false); adminUsersContent.setManaged(false);
        normalCenterContent.setVisible(true); normalCenterContent.setManaged(true);
        
        setButtonVisible(btnBackAdmin, false);
        setButtonVisible(btnAdminFilterAll, false);
        setButtonVisible(btnAdminFilterSeller, false);
        setButtonVisible(btnAdminFilterBidder, false);
        applySessionState();
        setActiveFilter(btnAll, null);
    }

    @FXML void adminFilterAll(ActionEvent event) { setAdminUserFilter("ALL", btnAdminFilterAll); }
    @FXML void adminFilterSeller(ActionEvent event) { setAdminUserFilter("SELLER", btnAdminFilterSeller); }
    @FXML void adminFilterBidder(ActionEvent event) { setAdminUserFilter("BIDDER", btnAdminFilterBidder); }

    private void setAdminUserFilter(String filter, Button activeBtn) {
        if (btnAdminFilterAll != null) btnAdminFilterAll.getStyleClass().setAll("button", "sidebar-btn");
        if (btnAdminFilterSeller != null) btnAdminFilterSeller.getStyleClass().setAll("button", "sidebar-btn");
        if (btnAdminFilterBidder != null) btnAdminFilterBidder.getStyleClass().setAll("button", "sidebar-btn");
        if (activeBtn != null) activeBtn.getStyleClass().setAll("button", "sidebar-btn-active");
        adminUserFilter = filter;
        loadUsersToAdminView();
    }

    private void loadUsersToAdminView() {
        userListContainer.getChildren().clear();
        List<User> users = AppDatabase.getInstance().getUsers();
        
        users.stream()
            .filter(u -> u.getRole() != AccountRole.ADMIN)
            .filter(u -> "ALL".equals(adminUserFilter) || adminUserFilter.equals(u.getRole().name()))
            .sorted(Comparator.comparing(User::getId))
            .forEach(u -> userListContainer.getChildren().add(createUserCard(u)));
        
        if (userListContainer.getChildren().isEmpty()) {
            Label emptyLabel = new Label("Không có");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7F8C8D; -fx-padding: 20;");
            userListContainer.getChildren().add(emptyLabel);
        }
    }

    private HBox createUserCard(User u) {
        HBox card = new HBox(15);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox info = new VBox(5);
        Label nameLbl = new Label(u.getFullName() != null && !u.getFullName().isEmpty() ? u.getFullName() : u.getUsername());
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        Label roleLbl = new Label("Vai trò: " + u.getRole() + " | Trạng thái: " + u.getStatus());
        
        String suspensionInfo = "Đình chỉ: " + u.getSuspensionCount() + " lần";
        if ("SUSPENDED".equals(u.getStatus())) {
            if (u.getSuspendedUntil() == -1) {
                suspensionInfo += " (Vĩnh viễn)";
            } else {
                java.time.LocalDateTime date = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(u.getSuspendedUntil()), java.time.ZoneId.systemDefault());
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                suspensionInfo += " (Đến " + date.format(formatter) + ")";
            }
        }
        Label suspLbl = new Label(suspensionInfo);
        info.getChildren().addAll(nameLbl, roleLbl, suspLbl);

        HBox actions = new HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        HBox.setHgrow(actions, javafx.scene.layout.Priority.ALWAYS);

        if (u.getRole() == AccountRole.SELLER && "PENDING".equals(u.getStatus())) {
            Button approveBtn = new Button("Duyệt");
            approveBtn.getStyleClass().add("btn-primary");
            approveBtn.setOnAction(e -> {
                u.setStatus("ACTIVE");
                AppDatabase.getInstance().updateUserStatus(u);
                loadUsersToAdminView();
            });
            Button rejectBtn = new Button("Từ chối");
            rejectBtn.getStyleClass().add("btn-outline");
            rejectBtn.setOnAction(e -> {
                u.setStatus("REJECTED");
                AppDatabase.getInstance().updateUserStatus(u);
                loadUsersToAdminView();
            });
            actions.getChildren().addAll(approveBtn, rejectBtn);
        } else {
            if (!"SUSPENDED".equals(u.getStatus())) {
                Button suspendBtn = new Button("Đình chỉ");
                suspendBtn.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 15; -fx-background-radius: 5;");
                suspendBtn.setOnAction(e -> suspendUser(u));
                actions.getChildren().add(suspendBtn);
            } else {
                Button restoreBtn = new Button("Khôi phục");
                restoreBtn.setStyle("-fx-background-color: #2ECC71; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 5 15; -fx-background-radius: 5;");
                restoreBtn.setOnAction(e -> {
                    u.setStatus("ACTIVE");
                    u.setSuspendedUntil(0);
                    AppDatabase.getInstance().updateUserStatus(u);
                    loadUsersToAdminView();
                });
                actions.getChildren().add(restoreBtn);
            }
        }

        card.getChildren().addAll(info, actions);
        return card;
    }

    private void suspendUser(User u) {
        // Reset count if > 1 year
        long oneYearMillis = 365L * 24 * 60 * 60 * 1000;
        if (u.getLastSuspensionTime() > 0 && System.currentTimeMillis() - u.getLastSuspensionTime() > oneYearMillis) {
            if (u.getSuspensionCount() < 3) {
                u.setSuspensionCount(0); // Reset
            }
        }

        u.setSuspensionCount(u.getSuspensionCount() + 1);
        u.setLastSuspensionTime(System.currentTimeMillis());
        u.setStatus("SUSPENDED");

        if (u.getSuspensionCount() == 1) {
            u.setSuspendedUntil(System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000); // 3 days
        } else if (u.getSuspensionCount() == 2) {
            u.setSuspendedUntil(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000); // 7 days
        } else {
            u.setSuspendedUntil(-1); // Permanent
        }

        AppDatabase.getInstance().updateUserStatus(u);
        
        SuspensionLog log = new SuspensionLog(
            java.util.UUID.randomUUID().toString(),
            u.getId(),
            u.getSuspensionCount() > 2 ? 3 : u.getSuspensionCount(),
            LocalDateTime.now(),
            "Đang hiệu lực"
        );
        AppDatabase.getInstance().addSuspensionLog(log);

        loadUsersToAdminView();
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đình chỉ tài khoản " + u.getUsername() + " (Lần " + u.getSuspensionCount() + ")");
    }

    @FXML
    public void showHistoryMode(ActionEvent event) {
        if (detailUpdateTimeline != null) detailUpdateTimeline.stop();
        activeAuction = null;
        isHistoryMode = true;
        
        setButtonVisible(btnHistory, false);
        setButtonVisible(btnBackHistory, true);
        setButtonVisible(btnManageAccounts, false);
        
        if (btnAll != null) btnAll.getStyleClass().setAll("button", "sidebar-btn");
        if (btnArt != null) btnArt.getStyleClass().setAll("button", "sidebar-btn");
        if (btnElec != null) btnElec.getStyleClass().setAll("button", "sidebar-btn");
        if (btnVeh != null) btnVeh.getStyleClass().setAll("button", "sidebar-btn");
        if (btnManageAccounts != null) btnManageAccounts.getStyleClass().setAll("button", "sidebar-btn");
        if (btnHistory != null) btnHistory.getStyleClass().setAll("button", "sidebar-btn-active");
        if (btnBackHistory != null) btnBackHistory.getStyleClass().setAll("button", "sidebar-btn-active");
        
        normalSidebar.setVisible(true); normalSidebar.setManaged(true);
        auctionSidebar.setVisible(false); auctionSidebar.setManaged(false);
        
        normalCenterContent.setVisible(false); normalCenterContent.setManaged(false);
        auctionDetailContent.setVisible(false); auctionDetailContent.setManaged(false);
        adminUsersContent.setVisible(false); adminUsersContent.setManaged(false);
        historyCenterContent.setVisible(true); historyCenterContent.setManaged(true);
        
        User user = Session.getCurrentUser();
        if (user instanceof Admin) {
            adminHistoryTabs.setVisible(true); adminHistoryTabs.setManaged(true);
            
            if (adminSellerComboBox != null) {
                adminSellerComboBox.getItems().clear();
                AppDatabase.getInstance().getUsers().stream()
                    .filter(u -> u instanceof Seller)
                    .forEach(u -> adminSellerComboBox.getItems().add(u.getUsername()));
                adminSellerComboBox.setOnAction(e -> loadHistoryItems());
            }
            
            if (adminRoleComboBox != null) {
                adminRoleComboBox.getItems().addAll("Tất cả", "Seller", "Bidder");
                adminRoleComboBox.setValue("Tất cả");
                adminRoleComboBox.setOnAction(e -> loadHistoryItems());
            }

            adminSellerComboBox.setVisible(true); adminSellerComboBox.setManaged(true);
            adminRoleComboBox.setVisible(false); adminRoleComboBox.setManaged(false);
            
            // Populate sellers
            adminSellerComboBox.getItems().clear();
            AppDatabase.getInstance().getUsers().stream()
                .filter(u -> u instanceof Seller)
                .forEach(u -> adminSellerComboBox.getItems().add(u.getUsername()));
                
            adminSellerComboBox.setOnAction(e -> loadHistoryItems());
            switchAdminHistoryTab(new ActionEvent(tabAuctionHistory, null));
        } else {
            adminHistoryTabs.setVisible(false); adminHistoryTabs.setManaged(false);
            adminSellerComboBox.setVisible(false); adminSellerComboBox.setManaged(false);
            if (adminRoleComboBox != null) {
                adminRoleComboBox.setVisible(false); adminRoleComboBox.setManaged(false);
            }
            btnAll.setText("Tất cả");
            loadHistoryItems();
        }
    }

    @FXML
    public void hideHistoryMode(ActionEvent event) {
        isHistoryMode = false;
        setButtonVisible(btnHistory, true);
        setButtonVisible(btnBackHistory, false);
        
        historyCenterContent.setVisible(false); historyCenterContent.setManaged(false);
        normalCenterContent.setVisible(true); normalCenterContent.setManaged(true);
        
        // Restore category buttons text if changed
        btnAll.setText("Trang chủ");
        btnArt.setText("Nghệ thuật (Art)");
        btnElec.setText("Điện tử (Electronics)");
        btnVeh.setText("Phương tiện (Vehicle)");
        btnAll.setVisible(true); btnAll.setManaged(true);
        btnArt.setVisible(true); btnArt.setManaged(true);
        btnElec.setVisible(true); btnElec.setManaged(true);
        btnVeh.setVisible(true); btnVeh.setManaged(true);
        
        loadAuctionItems();
    }
    
    @FXML
    public void switchAdminHistoryTab(ActionEvent event) {
        if (event.getSource() == tabAuctionHistory) {
            adminHistoryTabMode = 1;
            tabAuctionHistory.getStyleClass().setAll("button", "btn-primary");
            tabSuspensionHistory.getStyleClass().setAll("button", "btn-outline");
            
            adminSellerComboBox.setVisible(true); adminSellerComboBox.setManaged(true);
            adminRoleComboBox.setVisible(false); adminRoleComboBox.setManaged(false);
            
            btnAll.setText("Tất cả");
            btnArt.setText("Nghệ thuật (Art)");
            btnElec.setText("Điện tử (Electronics)");
            btnVeh.setText("Phương tiện (Vehicle)");
            btnAll.setVisible(true); btnAll.setManaged(true);
            btnArt.setVisible(true); btnArt.setManaged(true);
            btnElec.setVisible(true); btnElec.setManaged(true);
            btnVeh.setVisible(true); btnVeh.setManaged(true);
            historySearchField.setPromptText("Tìm kiếm lịch sử phiên...");
            setActiveFilter(btnAll, null);
        } else if (event.getSource() == tabSuspensionHistory) {
            adminHistoryTabMode = 2;
            tabAuctionHistory.getStyleClass().setAll("button", "btn-outline");
            tabSuspensionHistory.getStyleClass().setAll("button", "btn-primary");
            
            adminSellerComboBox.setVisible(false); adminSellerComboBox.setManaged(false);
            adminRoleComboBox.setVisible(true); adminRoleComboBox.setManaged(true);
            
            btnAll.setText("Lần 1");
            btnArt.setText("Lần 2");
            btnElec.setText("Vĩnh viễn");
            btnVeh.setVisible(false); btnVeh.setManaged(false);
            historySearchField.setPromptText("Tìm kiếm người dùng...");
            setActiveFilter(btnAll, "L1");
        }
    }
    
    @FXML
    public void handleSearchHistory(ActionEvent event) {
        loadHistoryItems();
    }

    private void setActiveFilter(Button activeBtn, String type) {
        if (btnAll != null) btnAll.getStyleClass().setAll("button", "sidebar-btn");
        if (btnArt != null) btnArt.getStyleClass().setAll("button", "sidebar-btn");
        if (btnElec != null) btnElec.getStyleClass().setAll("button", "sidebar-btn");
        if (btnVeh != null) btnVeh.getStyleClass().setAll("button", "sidebar-btn");
        if (btnHistory != null) btnHistory.getStyleClass().setAll("button", "sidebar-btn");
        if (btnBackHistory != null) btnBackHistory.getStyleClass().setAll("button", "sidebar-btn");
        if (btnManageAccounts != null) btnManageAccounts.getStyleClass().setAll("button", "sidebar-btn");
        if (activeBtn != null) activeBtn.getStyleClass().setAll("button", "sidebar-btn-active");
        currentFilterType = type;
        if (isHistoryMode) {
            loadHistoryItems();
        } else {
            loadAuctionItems();
        }
    }

    private void loadHistoryItems() {
        historyContainer.getChildren().clear();
        User user = Session.getCurrentUser();
        if (user == null) return;
        
        String keyword = historySearchField.getText().trim().toLowerCase();

        if (user instanceof Admin) {
            if (adminHistoryTabMode == 2) {
                // Lịch sử vi phạm
                int filterLevel = 0;
                if ("L1".equals(currentFilterType)) filterLevel = 1;
                else if ("L2".equals(currentFilterType)) filterLevel = 2;
                else if ("L3".equals(currentFilterType)) filterLevel = 3;
                
                List<SuspensionLog> logs = AppDatabase.getInstance().getSuspensionHistory(filterLevel, keyword);
                
                String roleFilter = adminRoleComboBox.getValue();
                
                logs.stream()
                    .filter(log -> {
                        if ("Tất cả".equals(roleFilter) || roleFilter == null) return true;
                        User u = AppDatabase.getInstance().findUserById(log.getUserId());
                        if (u == null) return false;
                        if ("Seller".equals(roleFilter) && u instanceof Seller) return true;
                        if ("Bidder".equals(roleFilter) && u instanceof Bidder) return true;
                        return false;
                    })
                    .sorted(Comparator.comparing(SuspensionLog::getUserId))
                    .forEach(log -> historyContainer.getChildren().add(createSuspensionCard(log)));
            } else {
                // Lịch sử phiên của seller được chọn
                String selectedSeller = adminSellerComboBox.getValue();
                if (selectedSeller == null || selectedSeller.isEmpty()) return;
                
                User seller = AppDatabase.getInstance().findUserById(selectedSeller);
                if (seller instanceof Seller) {
                    List<Auction> history = AppDatabase.getInstance().getSellerHistory(seller.getId());
                    history.stream()
                        .filter(a -> currentFilterType == null || (
                            (currentFilterType.equals("ART") && a.getItem() instanceof Art) ||
                            (currentFilterType.equals("ELEC") && a.getItem() instanceof Electronics) ||
                            (currentFilterType.equals("VEHICLE") && a.getItem() instanceof Vehicle)
                        ))
                        .filter(a -> keyword.isEmpty() || a.getItem().getName().toLowerCase().contains(keyword))
                        .forEach(a -> historyContainer.getChildren().add(createHistoryCard(a, false)));
                }
            }
        } else if (user instanceof Seller) {
            List<Auction> history = AppDatabase.getInstance().getSellerHistory(user.getId());
            history.stream()
                .filter(a -> currentFilterType == null || (
                    (currentFilterType.equals("ART") && a.getItem() instanceof Art) ||
                    (currentFilterType.equals("ELEC") && a.getItem() instanceof Electronics) ||
                    (currentFilterType.equals("VEHICLE") && a.getItem() instanceof Vehicle)
                ))
                .filter(a -> keyword.isEmpty() || a.getItem().getName().toLowerCase().contains(keyword))
                .forEach(a -> historyContainer.getChildren().add(createHistoryCard(a, false)));
        } else if (user instanceof Bidder) {
            List<Auction> history = AppDatabase.getInstance().getBidderHistory(user.getId());
            history.stream()
                .filter(a -> currentFilterType == null || (
                    (currentFilterType.equals("ART") && a.getItem() instanceof Art) ||
                    (currentFilterType.equals("ELEC") && a.getItem() instanceof Electronics) ||
                    (currentFilterType.equals("VEHICLE") && a.getItem() instanceof Vehicle)
                ))
                .filter(a -> keyword.isEmpty() || a.getItem().getName().toLowerCase().contains(keyword))
                .forEach(a -> historyContainer.getChildren().add(createHistoryCard(a, true)));
        }
        
        if (historyContainer.getChildren().isEmpty()) {
            Label emptyLabel = new Label("Không có");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7F8C8D; -fx-padding: 20;");
            historyContainer.getChildren().add(emptyLabel);
        }
    }
    
    private VBox createSuspensionCard(SuspensionLog log) {
        VBox card = new VBox(8);
        card.getStyleClass().add("item-card");
        card.setPrefWidth(250);
        card.setStyle("-fx-border-color: #E74C3C; -fx-border-radius: 5;");
        
        User u = AppDatabase.getInstance().findUserById(log.getUserId());
        String name = u != null ? u.getUsername() : log.getUserId();
        
        Label nameLabel = new Label("User: " + name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label levelLabel = new Label("Mức độ: " + (log.getSuspensionLevel() == 3 ? "Vĩnh viễn" : "Lần " + log.getSuspensionLevel()));
        Label timeLabel = new Label("Thời gian: " + log.getTimestamp().format(DATE_TIME_FORMATTER));
        Label statusLabel = new Label("Trạng thái: " + log.getStatus());
        
        if ("Đã xóa vĩnh viễn".equals(log.getStatus())) {
            statusLabel.setStyle("-fx-text-fill: #7F8C8D;");
        } else if ("Đã được khôi phục".equals(log.getStatus())) {
            statusLabel.setStyle("-fx-text-fill: #2ECC71;");
            card.setStyle("-fx-border-color: #2ECC71; -fx-border-radius: 5;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #E74C3C; -fx-font-weight: bold;");
        }
        
        card.getChildren().addAll(nameLabel, levelLabel, timeLabel, statusLabel);
        return card;
    }

    private VBox createHistoryCard(Auction auction, boolean isBidderView) {
        VBox card = new VBox(8);
        card.getStyleClass().add("item-card");
        card.setPrefWidth(250);

        boolean isWinner = false;
        User currentUser = Session.getCurrentUser();
        if (isBidderView && currentUser != null && currentUser.getId().equals(auction.getHighestBidderId()) && "STOPPED".equals(auction.getStatus())) {
            isWinner = true;
            card.setStyle("-fx-background-color: #E8F8F5; -fx-border-color: #2ECC71; -fx-border-radius: 5; -fx-background-radius: 5;");
        }

        Label nameLabel = new Label(auction.getItem().getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label timeLabel = new Label(auction.getStartTime().format(DATE_TIME_FORMATTER) + " - " + auction.getStopTime().format(DATE_TIME_FORMATTER));
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7F8C8D;");

        Label statusLabel = new Label();
        if (isBidderView) {
            // Find max bid of this bidder
            double maxBid = 0;
            if (currentUser != null) {
                maxBid = auction.getBidHistory().stream()
                    .filter(b -> b.getBidderId().equals(currentUser.getId()))
                    .mapToDouble(BidTransaction::getBidAmount)
                    .max().orElse(0);
            }
            statusLabel.setText("Giá bạn đặt: " + String.format("%.0f", maxBid) + " USD");
            statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2980B9;");
        } else {
            if ("STOPPED".equals(auction.getStatus())) {
                statusLabel.setText("Giá chốt: " + String.format("%.0f", auction.getCurrentHighestPrice()) + " USD");
                statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #E67E22;");
                
                String winnerName = "Không có";
                if (auction.getHighestBidderId() != null) {
                    User w = AppDatabase.getInstance().findUserById(auction.getHighestBidderId());
                    if (w != null) winnerName = w.getUsername();
                }
                Label winnerLabel = new Label("Người thắng: " + winnerName);
                card.getChildren().add(winnerLabel);
            } else {
                statusLabel.setText("Giá hiện tại: " + String.format("%.0f", auction.getCurrentHighestPrice()) + " USD");
                statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #3498DB;");
                Label statusInfo = new Label("Đang diễn ra/Sắp tới");
                card.getChildren().add(statusInfo);
            }
        }

        card.getChildren().addAll(nameLabel, timeLabel, statusLabel);
        card.setOnMouseClicked(e -> showHistoryDetails(auction));
        return card;
    }

    @FXML
    private void showRunningAuctions(ActionEvent event) {
        isUpcomingTab = false;
        if (btnTabRunning != null) btnTabRunning.getStyleClass().setAll("button", "btn-primary");
        if (btnTabUpcoming != null) btnTabUpcoming.getStyleClass().setAll("button", "btn-outline");
        loadAuctionItems();
    }

    @FXML
    private void showUpcomingAuctions(ActionEvent event) {
        isUpcomingTab = true;
        if (btnTabUpcoming != null) btnTabUpcoming.getStyleClass().setAll("button", "btn-primary");
        if (btnTabRunning != null) btnTabRunning.getStyleClass().setAll("button", "btn-outline");
        loadAuctionItems();
    }

    private void loadAuctionItems() {
        if (isHistoryMode) return;
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
            lastAuctionCount = demoAuctions.size();
            demoAuctions.stream()
                .filter(a -> !a.getStatus().equals("STOPPED"))
                .filter(a -> isUpcomingTab ? LocalDateTime.now().isBefore(a.getStartTime()) : !LocalDateTime.now().isBefore(a.getStartTime()))
                .filter(a -> currentFilterType == null || (
                    (currentFilterType.equals("ART") && a.getItem() instanceof Art) ||
                    (currentFilterType.equals("ELEC") && a.getItem() instanceof Electronics) ||
                    (currentFilterType.equals("VEHICLE") && a.getItem() instanceof Vehicle)
                ))
                .forEach(this::createAuctionCard);
        } else {
            lastAuctionCount = database.getAuctions().size();
            database.getAuctions().stream()
                .filter(a -> !a.getStatus().equals("STOPPED"))
                .filter(a -> isUpcomingTab ? LocalDateTime.now().isBefore(a.getStartTime()) : !LocalDateTime.now().isBefore(a.getStartTime()))
                .filter(a -> currentFilterType == null || (
                    (currentFilterType.equals("ART") && a.getItem() instanceof Art) ||
                    (currentFilterType.equals("ELEC") && a.getItem() instanceof Electronics) ||
                    (currentFilterType.equals("VEHICLE") && a.getItem() instanceof Vehicle)
                ))
                .forEach(this::createAuctionCard);
        }
        
        if (itemContainer.getChildren().isEmpty()) {
            Label emptyLabel = new Label("Không có");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7F8C8D; -fx-padding: 20;");
            itemContainer.getChildren().add(emptyLabel);
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

        Label priceLabel = new Label();
        priceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #E67E22; -fx-font-size: 14px;");

        Label timerLabel = new Label();
        timerLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        
        Runnable updateTimerLabel = () -> {
            priceLabel.setText("Giá cao nhất: " + String.format("%.0f", auction.getCurrentHighestPrice()) + " $");
            if (LocalDateTime.now().isBefore(auction.getStartTime())) {
                long waitSeconds = java.time.Duration.between(LocalDateTime.now(), auction.getStartTime()).getSeconds();
                long h = waitSeconds / 3600;
                long m = (waitSeconds % 3600) / 60;
                long s = waitSeconds % 60;
                timerLabel.setText(String.format("Sắp bắt đầu: %02d:%02d:%02d", h, m, s));
                if (!"-fx-text-fill: green; -fx-font-weight: bold;".equals(timerLabel.getStyle())) {
                    timerLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                }
            } else {
                long remaining = auction.getRemainingTime();
                if (remaining > 0) {
                    long hours = remaining / 3600;
                    long minutes = (remaining % 3600) / 60;
                    long seconds = remaining % 60;
                    timerLabel.setText(String.format("Còn lại: %02d:%02d:%02d", hours, minutes, seconds));
                    if (!"-fx-text-fill: red; -fx-font-weight: bold;".equals(timerLabel.getStyle())) {
                        timerLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    }
                } else {
                    timerLabel.setText("Phiên đấu giá đã kết thúc");
                    if (!"-fx-text-fill: red; -fx-font-weight: bold;".equals(timerLabel.getStyle())) {
                        timerLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    }
                }
            }
        };
        
        updateTimerLabel.run();
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> updateTimerLabel.run()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        card.getChildren().addAll(nameLabel, typeLabel, extraLabel, startLabel, priceLabel, timerLabel);
        
        card.setOnMouseClicked(e -> showAuctionDetails(auction));

        itemContainer.getChildren().add(card);
    }

    private void showAuctionDetails(Auction auction) {
        this.activeAuction = auction;

        // Cập nhật dữ liệu cho giao diện chi tiết
        Item item = auction.getItem();
        detailNameLabel.setText(item.getName());
        detailStatusLabel.setText(auction.getStatus());
        detailDescLabel.setText("Mô tả: " + item.getDescription());

        detailSpecificVBox.getChildren().clear();
        if (item instanceof Art) {
            Art art = (Art) item;
            detailSpecificVBox.getChildren().addAll(new Label("Nghệ sĩ: " + art.getArtist()), new Label("Năm sáng tác: " + art.getCreationYear()));
        } else if (item instanceof Electronics) {
            Electronics elec = (Electronics) item;
            detailSpecificVBox.getChildren().addAll(new Label("Bảo hành: " + elec.getWarrantyMonths() + " tháng"), new Label("Tình trạng: " + elec.getCondition()));
            if ("Cũ".equals(elec.getCondition())) {
                detailSpecificVBox.getChildren().add(new Label("Thời gian mua: " + elec.getPurchaseDate()));
                detailSpecificVBox.getChildren().add(new Label("Đã sửa chữa: " + elec.getIsRepaired()));
                if ("Có".equals(elec.getIsRepaired())) {
                    detailSpecificVBox.getChildren().addAll(new Label("Ngày sửa chữa: " + elec.getRepairDate()), new Label("Phụ tùng thay thế: " + elec.getRepairedParts()));
                }
            }
        } else if (item instanceof Vehicle) {
            Vehicle veh = (Vehicle) item;
            detailSpecificVBox.getChildren().addAll(new Label("Hãng: " + veh.getBrand()), new Label("Tình trạng: " + veh.getCondition()));
            if ("Cũ".equals(veh.getCondition())) {
                detailSpecificVBox.getChildren().addAll(new Label("Thời gian mua: " + veh.getPurchaseDate()), new Label("Số km đã đi: " + veh.getMileage() + " km"), new Label("Đã sửa chữa: " + veh.getIsRepaired()));
                if ("Có".equals(veh.getIsRepaired())) {
                    detailSpecificVBox.getChildren().addAll(new Label("Ngày sửa chữa: " + veh.getRepairDate()), new Label("Phụ tùng thay thế: " + veh.getRepairedParts()));
                }
            }
        }

        updateAuctionDynamicInfo(auction);

        // Hiển thị nút hành động nếu là Admin
        detailActionBox.getChildren().clear();
        if (isAdmin()) {
            Button stopBtn = new Button("Ngưng phiên"); stopBtn.getStyleClass().add("btn-outline");
            stopBtn.setOnAction(e -> handleStopAuction(auction));
            Button deleteBtn = new Button("Xóa phiên"); deleteBtn.getStyleClass().add("btn-outline");
            deleteBtn.setOnAction(e -> handleDeleteAuction(auction));
            detailActionBox.getChildren().addAll(stopBtn, deleteBtn);
        }

        User user = Session.getCurrentUser();
        boolean isSeller = user != null && user.getRole() == AccountRole.SELLER;
        
        if (bidderSidebar != null) {
            bidderSidebar.setVisible(!isSeller); 
            bidderSidebar.setManaged(!isSeller);
        }
        if (sellerSidebar != null) {
            sellerSidebar.setVisible(isSeller); 
            sellerSidebar.setManaged(isSeller);
        }
        
        if (isSeller) {
            loadTopBidders();
        }

        // Tải cấu hình Auto-Bid cho phiên hiện tại
        AutoBidConfig config = autoBidConfigs.get(auction.getId());
        if (config != null) {
            maxBidField.setText(String.valueOf((long)config.maxBid));
            bidStepField.setText(String.valueOf((long)config.bidStep));
            autoBidDelayField.setText(String.valueOf(config.delaySeconds));
            if (config.isActivated) {
                btnAutoBid.setText("Hủy tự động đấu giá");
                btnAutoBid.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white;");
                if (autoBidStatusLabel != null) autoBidStatusLabel.setText("Đang tự động đấu giá");
            } else {
                btnAutoBid.setText("Tự động đấu giá");
                btnAutoBid.getStyleClass().setAll("button", "btn-primary");
                if (autoBidStatusLabel != null) autoBidStatusLabel.setText("");
            }
        } else {
            if (maxBidField != null) maxBidField.clear();
            if (bidStepField != null) bidStepField.clear();
            if (autoBidDelayField != null) autoBidDelayField.clear();
            if (btnAutoBid != null) {
                btnAutoBid.setText("Tự động đấu giá");
                btnAutoBid.getStyleClass().setAll("button", "btn-primary");
            }
            if (autoBidStatusLabel != null) autoBidStatusLabel.setText("");
        }

        // Khởi động Timeline cập nhật liên tục mỗi 1 giây
        if (detailUpdateTimeline != null) detailUpdateTimeline.stop();
        detailUpdateTimeline = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            Auction latest = database.findAuctionById(activeAuction.getId());
            if (latest != null) {
                activeAuction = latest;
                updateAuctionDynamicInfo(latest);
                checkAutoBidLogic(latest);
            }
        }));
        detailUpdateTimeline.setCycleCount(Animation.INDEFINITE);
        detailUpdateTimeline.play();

        // Chuyển đổi trạng thái giao diện
        normalSidebar.setVisible(false); normalSidebar.setManaged(false);
        normalCenterContent.setVisible(false); normalCenterContent.setManaged(false);
        auctionSidebar.setVisible(true); auctionSidebar.setManaged(true);
        auctionDetailContent.setVisible(true); auctionDetailContent.setManaged(true);
        if (detailWinnerBox != null) {
            detailWinnerBox.setVisible(false); detailWinnerBox.setManaged(false);
        }
    }

    private void showHistoryDetails(Auction auction) {
        this.activeAuction = auction;
        if (detailUpdateTimeline != null) detailUpdateTimeline.stop();
        
        Item item = auction.getItem();
        detailNameLabel.setText(item.getName());
        detailStatusLabel.setText("ĐÃ KẾT THÚC");
        detailStatusLabel.setStyle("-fx-background-color: #7F8C8D; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 12px;");
        detailDescLabel.setText("Mô tả: " + item.getDescription());
        
        detailSpecificVBox.getChildren().clear();
        if (item instanceof Art) {
            Art art = (Art) item;
            detailSpecificVBox.getChildren().addAll(new Label("Nghệ sĩ: " + art.getArtist()), new Label("Năm sáng tác: " + art.getCreationYear()));
        } else if (item instanceof Electronics) {
            Electronics elec = (Electronics) item;
            detailSpecificVBox.getChildren().addAll(new Label("Bảo hành: " + elec.getWarrantyMonths() + " tháng"), new Label("Tình trạng: " + elec.getCondition()));
        } else if (item instanceof Vehicle) {
            Vehicle veh = (Vehicle) item;
            detailSpecificVBox.getChildren().addAll(new Label("Hãng: " + veh.getBrand()), new Label("Tình trạng: " + veh.getCondition()));
        }
        
        detailPriceLabel.setText("Giá chốt: " + auction.getCurrentHighestPrice() + " USD");
        
        String winnerName = "Không có";
        if (auction.getHighestBidderId() != null) {
            User w = AppDatabase.getInstance().findUserById(auction.getHighestBidderId());
            if (w != null) winnerName = w.getUsername();
        }
        detailBidderLabel.setText("Người chiến thắng: " + winnerName);
        detailTimerLabel.setText("Trạng thái: " + ("STOPPED".equals(auction.getStatus()) ? "Đã kết thúc" : "Đang diễn ra/Sắp tới"));
        detailTimerLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-weight: bold;");
        
        detailActionBox.getChildren().clear();
        
        if (bidderSidebar != null) {
            bidderSidebar.setVisible(false); bidderSidebar.setManaged(false);
        }
        if (sellerSidebar != null) {
            sellerSidebar.setVisible(false); sellerSidebar.setManaged(false);
        }
        
        // Show Winner Box
        if (detailWinnerBox != null) {
            detailWinnerBox.setVisible(true); detailWinnerBox.setManaged(true);
            detailWinnerText.setText(winnerName + " - " + String.format("%.0f", auction.getCurrentHighestPrice()) + " USD");
            
            User currentUser = Session.getCurrentUser();
            if (currentUser instanceof Bidder) {
                double maxBid = auction.getBidHistory().stream()
                    .filter(b -> b.getBidderId().equals(currentUser.getId()))
                    .mapToDouble(BidTransaction::getBidAmount)
                    .max().orElse(0);
                detailBidderHistoryText.setText("Bạn đã đặt giá cao nhất: " + String.format("%.0f", maxBid) + " USD");
                detailBidderHistoryText.setVisible(true); detailBidderHistoryText.setManaged(true);
            } else {
                detailBidderHistoryText.setVisible(false); detailBidderHistoryText.setManaged(false);
            }
        }
        
        // Vẽ lại biểu đồ
        List<BidTransaction> history = AppDatabase.getInstance().getBidHistory(auction.getId());
        int newSize = history != null ? history.size() : 0;
        detailChart.setAnimated(false);
        detailChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (history != null) {
            for (BidTransaction b : history) {
                series.getData().add(new XYChart.Data<>(b.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")), b.getBidAmount()));
            }
        }
        detailChart.getData().add(series);
        detailChart.setUserData(newSize);

        historyCenterContent.setVisible(false); historyCenterContent.setManaged(false);
        auctionSidebar.setVisible(false); auctionSidebar.setManaged(false); // Hide sidebar in history
        normalSidebar.setVisible(true); normalSidebar.setManaged(true);
        auctionDetailContent.setVisible(true); auctionDetailContent.setManaged(true);
    }

    private void updateAuctionDynamicInfo(Auction auction) {
        detailPriceLabel.setText("Giá hiện tại: " + auction.getCurrentHighestPrice() + " USD");
        
        List<BidTransaction> history = AppDatabase.getInstance().getBidHistory(auction.getId());
        if (history != null && !history.isEmpty()) {
            String bidderId = history.get(history.size()-1).getBidderId();
            String bidderUsername = bidderId.startsWith("U_") ? bidderId.substring(2) : bidderId;
            User bidder = AppDatabase.getInstance().findUserByUsername(bidderUsername);
            String displayName = (bidder != null && bidder.getFullName() != null && !bidder.getFullName().isEmpty()) ? bidder.getFullName() : bidderUsername;
            detailBidderLabel.setText("Người đặt giá cao nhất: " + displayName);
        } else {
            detailBidderLabel.setText("Người đặt giá cao nhất: Chưa có");
        }

        if (LocalDateTime.now().isBefore(auction.getStartTime())) {
            long waitSeconds = java.time.Duration.between(LocalDateTime.now(), auction.getStartTime()).getSeconds();
            long h = waitSeconds / 3600;
            long m = (waitSeconds % 3600) / 60;
            long s = waitSeconds % 60;
            detailTimerLabel.setText(String.format("Sắp bắt đầu: %02d:%02d:%02d", h, m, s));
            if (!"-fx-text-fill: green; -fx-font-weight: bold;".equals(detailTimerLabel.getStyle())) {
                detailTimerLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            }
        } else {
            long remaining = auction.getRemainingTime();
            if (remaining > 0) {
                long h = remaining / 3600;
                long m = (remaining % 3600) / 60;
                long s = remaining % 60;
                detailTimerLabel.setText(String.format("Thời gian còn lại: %02d:%02d:%02d", h, m, s));
                if (!"-fx-text-fill: red; -fx-font-weight: bold;".equals(detailTimerLabel.getStyle())) {
                    detailTimerLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                }
            } else {
                detailTimerLabel.setText("Phiên đấu giá đã kết thúc");
                if (!"-fx-text-fill: red; -fx-font-weight: bold;".equals(detailTimerLabel.getStyle())) {
                    detailTimerLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    detailStatusLabel.setText("KẾT THÚC");
                    detailStatusLabel.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 12px;");
                }
            }
        }

        // Vẽ lại biểu đồ
        int newSize = history != null ? history.size() : 0;
        if (detailChart.getUserData() == null || (int) detailChart.getUserData() != newSize) {
            detailChart.setAnimated(false);
            detailChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            if (history != null) {
                for (BidTransaction b : history) {
                    series.getData().add(new XYChart.Data<>(b.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss")), b.getBidAmount()));
                }
            }
            detailChart.getData().add(series);
            detailChart.setUserData(newSize);
        }
    }

    @FXML public void handleBackToList() {
        if (detailUpdateTimeline != null) detailUpdateTimeline.stop();
        activeAuction = null;
        
        auctionSidebar.setVisible(false); auctionSidebar.setManaged(false);
        auctionDetailContent.setVisible(false); auctionDetailContent.setManaged(false);
        
        if (adminUsersContent != null) {
            adminUsersContent.setVisible(false); adminUsersContent.setManaged(false);
        }
        
        normalSidebar.setVisible(true); normalSidebar.setManaged(true);
        normalCenterContent.setVisible(true); normalCenterContent.setManaged(true);
        
        loadAuctionItems();
    }

    @FXML public void handleQuickBid500() { placeBidAndUpdate(500); }
    @FXML public void handleQuickBid1000() { placeBidAndUpdate(1000); }
    @FXML public void handleQuickBid2000() { placeBidAndUpdate(2000); }
    @FXML public void handleQuickBid5000() { placeBidAndUpdate(5000); }

    private void placeBidAndUpdate(double increment) {
        if (activeAuction == null || activeAuction.getRemainingTime() <= 0) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Phiên đấu giá đã kết thúc hoặc không khả dụng.");
            return;
        }
        User user = Session.getCurrentUser();
        boolean isDemo = user == null;
        if (!isDemo && !(user instanceof Bidder)) {
            showAlert(Alert.AlertType.WARNING, "Không đủ quyền", "Chỉ tài khoản Bidder được đặt giá.");
            return;
        }
        
        double nextBid = activeAuction.getCurrentHighestPrice() + increment;
        String bidderId = isDemo ? "GUEST" : user.getId();
        BidTransaction transaction = new BidTransaction(UUID.randomUUID().toString(), activeAuction.getId(), bidderId, nextBid, LocalDateTime.now());
        
        boolean success = activeAuction.placeBid(transaction);
        if (success) {
            updateAuctionDynamicInfo(activeAuction);
            if (!isDemo) sendBidToServer(transaction);
            if (bidSuccessLabel != null) bidSuccessLabel.setText("Thành công");
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đặt giá lúc này!");
        }
    }

    @FXML public void handleCustomBid() {
        if (activeAuction == null || activeAuction.getRemainingTime() <= 0) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Phiên đấu giá đã kết thúc hoặc không khả dụng.");
            return;
        }
        User user = Session.getCurrentUser();
        boolean isDemo = user == null;
        if (!isDemo && !(user instanceof Bidder)) {
            showAlert(Alert.AlertType.WARNING, "Không đủ quyền", "Chỉ tài khoản Bidder được đặt giá.");
            return;
        }
        
        try {
            double customAmount = Double.parseDouble(customBidField.getText().trim());
            if (customAmount < 100 || customAmount > 10000) {
                showAlert(Alert.AlertType.WARNING, "Lỗi", "Số tiền phải từ 100 - 10000 USD.");
                return;
            }
            
            double nextBid = activeAuction.getCurrentHighestPrice() + customAmount;
            String bidderId = isDemo ? "GUEST" : user.getId();
            BidTransaction transaction = new BidTransaction(UUID.randomUUID().toString(), activeAuction.getId(), bidderId, nextBid, LocalDateTime.now());
            
            boolean success = activeAuction.placeBid(transaction);
            if (success) {
                updateAuctionDynamicInfo(activeAuction);
                if (!isDemo) sendBidToServer(transaction);
                if (bidSuccessLabel != null) bidSuccessLabel.setText("Thành công");
                customBidField.clear();
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể đặt giá lúc này!");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Số tiền không hợp lệ.");
        }
    }

    @FXML public void handleSaveAutoBid() {
        if (activeAuction == null) return;
        
        AutoBidConfig currentConfig = autoBidConfigs.get(activeAuction.getId());
        boolean isActivated = (currentConfig != null && currentConfig.isActivated);
        
        if (isActivated) {
            // Hủy tự động đấu giá
            if (currentConfig != null) {
                currentConfig.isActivated = false;
                autoBidConfigs.put(activeAuction.getId(), currentConfig);
            }
            btnAutoBid.setText("Tự động đấu giá");
            btnAutoBid.getStyleClass().setAll("button", "btn-primary");
            if (autoBidStatusLabel != null) autoBidStatusLabel.setText("");
            if (bidSuccessLabel != null) bidSuccessLabel.setText("");
            return;
        }

        double maxBid = 0;
        double bidStep = 0;
        int delay = 1;

        try {
            maxBid = Double.parseDouble(maxBidField.getText().trim());
            bidStep = Double.parseDouble(bidStepField.getText().trim());
            
            if (maxBid < 100 || maxBid > 1000000) {
                throw new IllegalArgumentException("Giá tối đa phải từ 100 đến 1,000,000 USD");
            }
            if (bidStep < 100 || bidStep > 10000) {
                throw new IllegalArgumentException("Bước giá phải từ 100 đến 10,000 USD");
            }
            
            String delayStr = autoBidDelayField.getText().trim();
            if (!delayStr.isEmpty()) {
                delay = Integer.parseInt(delayStr);
            }
            if (delay < 1) delay = 1;
            
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Dữ liệu không hợp lệ", e.getMessage());
            return;
        } catch (Exception e) {
            showAlert(Alert.AlertType.WARNING, "Dữ liệu không hợp lệ", "Vui lòng nhập số hợp lệ.");
            return;
        }
        
        autoBidConfigs.put(activeAuction.getId(), new AutoBidConfig(true, maxBid, bidStep, delay));
        btnAutoBid.setText("Hủy tự động đấu giá");
        btnAutoBid.setStyle("-fx-background-color: #E74C3C; -fx-text-fill: white;");
        if (autoBidStatusLabel != null) autoBidStatusLabel.setText("Đang tự động đấu giá");
        if (bidSuccessLabel != null) bidSuccessLabel.setText("");
    }

    private void checkAutoBidLogic(Auction latestAuction) {
        User user = Session.getCurrentUser();
        if (user == null || latestAuction.getRemainingTime() <= 0) return;
        
        AutoBidConfig config = autoBidConfigs.get(latestAuction.getId());
        if (config == null || !config.isActivated) return;

        List<BidTransaction> history = AppDatabase.getInstance().getBidHistory(latestAuction.getId());
        String currentHighestBidder = (history != null && !history.isEmpty()) ? history.get(history.size()-1).getBidderId() : "";
        
        // Nếu mình không phải là người đặt giá cao nhất và có thể đấu giá tiếp
        if (!user.getId().equals(currentHighestBidder)) {
            double nextBid = latestAuction.getCurrentHighestPrice() + config.bidStep;
            if (nextBid <= config.maxBid && !config.isWaitingToBid) {
                config.isWaitingToBid = true;
                System.out.println("[Auto-Bid] Chuẩn bị tự động đặt giá sau " + config.delaySeconds + " giây...");
                
                if (autoBidDelayTimeline != null) autoBidDelayTimeline.stop();
                autoBidDelayTimeline = new Timeline(new KeyFrame(Duration.seconds(config.delaySeconds), ev -> {
                    // Kiểm tra lại sau độ trễ
                    Auction recheckedAuction = database.findAuctionById(latestAuction.getId());
                    if (recheckedAuction != null && recheckedAuction.getRemainingTime() > 0) {
                        List<BidTransaction> recheckedHistory = AppDatabase.getInstance().getBidHistory(recheckedAuction.getId());
                        String recheckedBidder = (recheckedHistory != null && !recheckedHistory.isEmpty()) ? recheckedHistory.get(recheckedHistory.size()-1).getBidderId() : "";
                        
                        double recheckedNextBid = recheckedAuction.getCurrentHighestPrice() + config.bidStep;
                        if (!user.getId().equals(recheckedBidder) && recheckedNextBid <= config.maxBid) {
                            BidTransaction transaction = new BidTransaction(UUID.randomUUID().toString(), recheckedAuction.getId(), user.getId(), recheckedNextBid, LocalDateTime.now());
                            boolean success = recheckedAuction.placeBid(transaction);
                            if (success) {
                                sendBidToServer(transaction);
                                System.out.println("[Auto-Bid] Đã tự động đặt giá: " + recheckedNextBid + " USD");
                                if (activeAuction != null && activeAuction.getId().equals(recheckedAuction.getId())) {
                                    updateAuctionDynamicInfo(recheckedAuction);
                                    if (isSellerView()) loadTopBidders();
                                }
                            }
                        }
                    }
                    config.isWaitingToBid = false;
                }));
                autoBidDelayTimeline.setCycleCount(1);
                autoBidDelayTimeline.play();
            }
        }
    }

    private boolean isSellerView() {
        User user = Session.getCurrentUser();
        return user != null && user.getRole() == AccountRole.SELLER;
    }

    private void loadTopBidders() {
        if (activeAuction == null || topBiddersContainer == null) return;
        topBiddersContainer.getChildren().clear();
        List<BidTransaction> history = AppDatabase.getInstance().getBidHistory(activeAuction.getId());
        if (history == null || history.isEmpty()) {
            topBiddersContainer.getChildren().add(new Label("Chưa có lượt đặt giá nào."));
            return;
        }
        
        // Tạo một bản sao và sắp xếp từ cao xuống thấp
        List<BidTransaction> sortedHistory = new ArrayList<>(history);
        sortedHistory.sort((a, b) -> Double.compare(b.getBidAmount(), a.getBidAmount()));
        int count = 0;
        Set<String> addedUsers = new HashSet<>();
        
        for (BidTransaction b : sortedHistory) {
            if (count >= 5) break;
            if (addedUsers.add(b.getBidderId())) {
                String bidderId = b.getBidderId();
                String bidderUsername = bidderId.startsWith("U_") ? bidderId.substring(2) : bidderId;
                User bidder = AppDatabase.getInstance().findUserByUsername(bidderUsername);
                String displayName = (bidder != null && bidder.getFullName() != null && !bidder.getFullName().isEmpty()) ? bidder.getFullName() : bidderUsername;
                
                Label lbl = new Label(displayName + " - " + b.getBidAmount() + " USD");
                lbl.setStyle("-fx-text-fill: white;");
                topBiddersContainer.getChildren().add(lbl);
                count++;
            }
        }
    }

    @FXML public void handleShowAllBidders() {
        if (activeAuction == null) return;
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Toàn bộ lịch sử đấu giá");
        dialog.setHeaderText("Lịch sử đấu giá của " + activeAuction.getItem().getName());
        
        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        List<BidTransaction> history = AppDatabase.getInstance().getBidHistory(activeAuction.getId());
        if (history != null && !history.isEmpty()) {
            List<BidTransaction> sortedHistory = new ArrayList<>(history);
            sortedHistory.sort((a, b) -> Double.compare(b.getBidAmount(), a.getBidAmount()));
            for (BidTransaction b : sortedHistory) {
                String bidderId = b.getBidderId();
                String bidderUsername = bidderId.startsWith("U_") ? bidderId.substring(2) : bidderId;
                User bidder = AppDatabase.getInstance().findUserByUsername(bidderUsername);
                String displayName = (bidder != null && bidder.getFullName() != null && !bidder.getFullName().isEmpty()) ? bidder.getFullName() : bidderUsername;
                
                Label lbl = new Label(displayName + " - " + b.getBidAmount() + " USD (" + b.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")) + ")");
                container.getChildren().add(lbl);
            }
        } else {
            container.getChildren().add(new Label("Chưa có lượt đặt giá nào."));
        }
        
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(300);
        
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void handleStopAuction(Auction auction) {
        if (!isAdmin()) {
            showAlert(Alert.AlertType.WARNING, "Không đủ quyền", "Chỉ Admin được ngưng phiên đấu giá.");
            return;
        }

        if (database.stopAuction(auction.getId())) {
            handleBackToList();
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
            handleBackToList();
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
            boolean wasMaximized = stage.isMaximized();
            if (wasMaximized) stage.setMaximized(false);
            stage.setScene(scene);
            if (wasMaximized) stage.setMaximized(true);
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
