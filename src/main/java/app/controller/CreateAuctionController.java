package app.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import app.database.AppDatabase;
import app.database.Session;
import app.model.*;
import app.network.ClientConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CreateAuctionController {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private Label pageTitle;
    @FXML private VBox formContainer;
    @FXML private Button btnArt, btnElec, btnVeh;

    private final AppDatabase database = AppDatabase.getInstance();

    private String selectedType = "ART";
    private Map<String, Control> dynamicControls = new HashMap<>();

    // Common fields
    private TextField nameField;
    private TextField descriptionField;
    private TextField startingPriceField;
    private TextField startTimeField;
    private TextField durationField;

    @FXML
    public void initialize() {
        buildForm();
    }

    // --- Sidebar selection ---
    @FXML void selectArt(ActionEvent event) { setActiveType("ART", btnArt); }
    @FXML void selectElectronics(ActionEvent event) { setActiveType("ELECTRONICS", btnElec); }
    @FXML void selectVehicle(ActionEvent event) { setActiveType("VEHICLE", btnVeh); }

    private void setActiveType(String type, Button activeBtn) {
        selectedType = type;
        // Update sidebar styles
        btnArt.getStyleClass().setAll("button", "sidebar-btn");
        btnElec.getStyleClass().setAll("button", "sidebar-btn");
        btnVeh.getStyleClass().setAll("button", "sidebar-btn");
        activeBtn.getStyleClass().setAll("button", "sidebar-btn-active");
        buildForm();
    }

    // --- Build the full form ---
    private void buildForm() {
        formContainer.getChildren().clear();
        dynamicControls.clear();

        // Update page title
        switch (selectedType) {
            case "ART": pageTitle.setText("Tạo phiên đấu giá - Nghệ thuật"); break;
            case "ELECTRONICS": pageTitle.setText("Tạo phiên đấu giá - Điện tử"); break;
            case "VEHICLE": pageTitle.setText("Tạo phiên đấu giá - Phương tiện"); break;
        }

        // === Section: Basic info ===
        Label sectionBasic = new Label("Thông tin cơ bản");
        sectionBasic.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 0 0 5 0;");

        nameField = new TextField(); nameField.setPromptText("Tên sản phẩm");
        descriptionField = new TextField(); descriptionField.setPromptText("Mô tả sản phẩm");
        startingPriceField = new TextField(); startingPriceField.setPromptText("Giá khởi điểm (USD)");

        GridPane basicGrid = createFormGrid();
        basicGrid.addRow(0, new Label("Tên sản phẩm:"), nameField);
        basicGrid.addRow(1, new Label("Mô tả:"), descriptionField);
        basicGrid.addRow(2, new Label("Giá khởi điểm:"), startingPriceField);

        formContainer.getChildren().addAll(sectionBasic, basicGrid);

        // === Section: Product-specific attributes ===
        Label sectionAttributes = new Label("Thuộc tính sản phẩm");
        sectionAttributes.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 15 0 5 0;");
        formContainer.getChildren().add(sectionAttributes);

        VBox attributesContainer = new VBox();
        buildAttributeFields(attributesContainer);
        formContainer.getChildren().add(attributesContainer);

        // === Section: Auction schedule ===
        Label sectionSchedule = new Label("Thời gian phiên đấu giá");
        sectionSchedule.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-padding: 15 0 5 0;");

        startTimeField = new TextField(LocalDateTime.now().plusMinutes(10).format(DATE_TIME_FORMATTER));
        startTimeField.setPromptText("yyyy-MM-dd HH:mm");
        durationField = new TextField("60");
        durationField.setPromptText("Số phút");

        GridPane scheduleGrid = createFormGrid();
        scheduleGrid.addRow(0, new Label("Thời gian bắt đầu:"), startTimeField);
        scheduleGrid.addRow(1, new Label("Thời lượng (phút):"), durationField);

        formContainer.getChildren().addAll(sectionSchedule, scheduleGrid);

        // === Submit button ===
        Button submitBtn = new Button("Tạo phiên đấu giá");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setStyle("-fx-font-size: 15px; -fx-padding: 10 30 10 30;");
        submitBtn.setOnAction(e -> handleSubmit());

        VBox btnContainer = new VBox(submitBtn);
        btnContainer.setStyle("-fx-padding: 20 0 0 0; -fx-alignment: CENTER;");
        formContainer.getChildren().add(btnContainer);
    }

    // --- Build product-specific fields ---
    private void buildAttributeFields(VBox container) {
        GridPane grid = createFormGrid();

        if ("ART".equals(selectedType)) {
            TextField author = new TextField(); author.setPromptText("Tên tác giả");
            TextField year = new TextField(); year.setPromptText("VD: 1998");
            dynamicControls.put("art_author", author);
            dynamicControls.put("art_year", year);
            grid.addRow(0, new Label("Tác giả:"), author);
            grid.addRow(1, new Label("Năm sáng tác:"), year);
        } else if ("ELECTRONICS".equals(selectedType)) {
            TextField warranty = new TextField(); warranty.setPromptText("VD: 12");
            ComboBox<String> condition = new ComboBox<>(); condition.getItems().addAll("Mới", "Cũ"); condition.setValue("Mới");
            condition.setMaxWidth(Double.MAX_VALUE);
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
            };
            condition.setOnAction(e -> updateElec.run());
            isRepaired.setOnAction(e -> updateElec.run());
            updateElec.run();
        } else if ("VEHICLE".equals(selectedType)) {
            TextField brand = new TextField(); brand.setPromptText("VD: Toyota");
            ComboBox<String> condition = new ComboBox<>(); condition.getItems().addAll("Mới", "Cũ"); condition.setValue("Mới");
            condition.setMaxWidth(Double.MAX_VALUE);
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
            };
            condition.setOnAction(e -> updateVeh.run());
            isRepaired.setOnAction(e -> updateVeh.run());
            updateVeh.run();
        }
        container.getChildren().add(grid);
    }

    // --- Submit handler ---
    private void handleSubmit() {
        try {
            String name = requireText(nameField, "Vui lòng nhập tên sản phẩm.");
            String description = requireText(descriptionField, "Vui lòng nhập mô tả sản phẩm.");
            double startingPrice = parsePositiveDouble(startingPriceField.getText(), "Giá khởi điểm phải lớn hơn 0.");

            Item item = createItemFromForm(selectedType, name, description, startingPrice);

            LocalDateTime startTime = parseStartTime(startTimeField.getText());
            long durationMinutes = parsePositiveLong(durationField.getText(), "Thời lượng phải là số phút lớn hơn 0.");
            LocalDateTime stopTime = startTime.plusMinutes(durationMinutes);

            Auction auction = new Auction(
                    "A_" + UUID.randomUUID(),
                    item,
                    startTime,
                    stopTime,
                    item.getStartingPrice(),
                    "RUNNING");

            User user = Session.getCurrentUser();
            if (database.addAuction(auction)) {
                if (user instanceof Seller) {
                    ((Seller) user).addItem(item);
                }
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã tạo sản phẩm và phiên đấu giá mới.");
                goBackToMain();
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể lưu phiên đấu giá mới.");
            }
        } catch (IllegalArgumentException e) {
            showAlert(Alert.AlertType.WARNING, "Dữ liệu chưa hợp lệ", e.getMessage());
        }
    }

    // --- Item creation ---
    private Item createItemFromForm(String type, String name, String description, double startingPrice) {
        String itemId = "I_" + UUID.randomUUID();
        switch (type) {
            case "ART":
                String artist = getControlText("art_author");
                String year = getControlText("art_year");
                if (artist.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập tác giả.");
                return new Art(itemId, name, description, startingPrice, artist, year.isEmpty() ? 0 : parseInteger(year, "Năm sáng tác phải là số."));
            case "ELECTRONICS":
                String warranty = getControlText("elec_warranty");
                String elecCond = getControlText("condition");
                String elecPurch = getControlText("purchaseDate");
                String elecDesc = getControlText("elec_desc");
                String elecIsRepaired = getControlText("isRepaired");
                String elecRepairDate = getControlText("repairDate");
                String elecRepairParts = getControlText("repairParts");
                if (warranty.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập số tháng bảo hành.");
                String finalElecCond = "Mới".equals(elecCond) ? "Mới" : ("Cũ" + (elecDesc.isEmpty() ? "" : " - " + elecDesc));
                return new Electronics(itemId, name, description, startingPrice, parseInteger(warranty, "Bảo hành phải là số tháng."), finalElecCond, "Mới".equals(elecCond) ? "" : elecPurch, "Cũ".equals(elecCond) ? elecIsRepaired : "Không", ("Cũ".equals(elecCond) && "Có".equals(elecIsRepaired)) ? elecRepairDate : "", ("Cũ".equals(elecCond) && "Có".equals(elecIsRepaired)) ? elecRepairParts : "");
            case "VEHICLE":
                String brand = getControlText("veh_brand");
                String vehCond = getControlText("condition");
                String vehPurch = getControlText("purchaseDate");
                String mileage = getControlText("veh_mileage");
                String isRepaired = getControlText("isRepaired");
                String repairDate = getControlText("repairDate");
                String repairParts = getControlText("repairParts");
                if (brand.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập hãng xe.");
                if ("Cũ".equals(vehCond) && mileage.isEmpty()) throw new IllegalArgumentException("Vui lòng nhập số km.");
                return new Vehicle(itemId, name, description, startingPrice, brand, "Cũ".equals(vehCond) ? parseInteger(mileage, "Số km phải là số.") : 0, vehCond, "Cũ".equals(vehCond) ? vehPurch : "", "Cũ".equals(vehCond) ? isRepaired : "Không", ("Cũ".equals(vehCond) && "Có".equals(isRepaired)) ? repairDate : "", ("Cũ".equals(vehCond) && "Có".equals(isRepaired)) ? repairParts : "");
            default:
                throw new IllegalArgumentException("Loại sản phẩm không hợp lệ.");
        }
    }

    // --- Helpers ---
    private String getControlText(String key) {
        Control c = dynamicControls.get(key);
        if (c instanceof TextField) return ((TextField) c).getText() == null ? "" : ((TextField) c).getText().trim();
        if (c instanceof ComboBox) { Object v = ((ComboBox<?>) c).getValue(); return v == null ? "" : v.toString(); }
        if (c instanceof CheckBox) return ((CheckBox) c).isSelected() ? "Có" : "Không";
        return "";
    }

    private GridPane createFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        return grid;
    }

    private String requireText(TextField field, String errorMessage) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isEmpty()) throw new IllegalArgumentException(errorMessage);
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
            if (parsed <= 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private long parsePositiveLong(String value, String errorMessage) {
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed <= 0) throw new NumberFormatException();
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

    // --- Navigation ---
    @FXML
    void handleBack(ActionEvent event) {
        goBackToMain();
    }

    private void goBackToMain() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/Main.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) formContainer.getScene().getWindow();
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
