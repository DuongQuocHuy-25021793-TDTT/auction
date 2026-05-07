package app.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import app.database.AppDatabase;
import app.model.Art;
import app.model.Auction;
import app.model.Electronics;
import app.model.Item;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddProductController {

    @FXML private ComboBox<String> comboType;
    @FXML private TextField txtName;
    @FXML private TextField txtDesc;
    @FXML private TextField txtPrice;
    @FXML private TextField txtExtra1; 
    @FXML private TextField txtExtra2; 

    @FXML
    public void initialize() {
        comboType.getItems().addAll("Nghệ thuật", "Điện tử");
        comboType.getSelectionModel().selectFirst();
        
       
        comboType.setOnAction(e -> {
            if ("Nghệ thuật".equals(comboType.getValue())) {
                txtExtra1.setPromptText("Tên họa sĩ / Nghệ nhân");
                txtExtra2.setVisible(true);
                txtExtra2.setPromptText("Năm sáng tác (VD: 2023)");
            } else {
                txtExtra1.setPromptText("Số tháng bảo hành (VD: 12)");
                txtExtra2.setVisible(false);
            }
        });
        
        comboType.fireEvent(new ActionEvent());
    }

    @FXML
    public void handleSave(ActionEvent event) {
        try {
            String type = comboType.getValue();
            String name = txtName.getText();
            String desc = txtDesc.getText();
            double price = Double.parseDouble(txtPrice.getText());

            Item newItem;
            String itemId = "I_" + UUID.randomUUID().toString().substring(0, 8);

            if ("Nghệ thuật".equals(type)) {
                String artist = txtExtra1.getText();
                int year = Integer.parseInt(txtExtra2.getText());
                newItem = new Art(itemId, name, desc, price, artist, year);
            } else {
                int warranty = Integer.parseInt(txtExtra1.getText());
                newItem = new Electronics(itemId, name, desc, price, warranty);
            }

            Auction newAuction = new Auction("A_" + UUID.randomUUID().toString().substring(0, 8), newItem, LocalDateTime.now(), LocalDateTime.now().plusDays(1), price, "RUNNING");

            if (AppDatabase.getInstance().addAuction(newAuction)) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Đã thêm thành công!");
                alert.showAndWait();
                ((Stage) txtName.getScene().getWindow()).close(); 
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Vui lòng nhập đúng định dạng số!").showAndWait();
        }
    }
}