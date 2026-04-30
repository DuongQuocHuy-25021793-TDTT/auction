package app.controller;

import com.google.gson.Gson;

import app.model.Bidder;
import app.model.Message;
import app.network.ClientConnection;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label statusLabel;

    @FXML
    public void handleLoginButtonAction(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        System.out.println("Đang đăng nhập tài khoản: " + username);

    
        try {
      
            ClientConnection.getInstance().connect("localhost", 8080);
            
     
            Bidder loginUser = new Bidder("temp_id", username, password);
            String userPayload = new Gson().toJson(loginUser); 
            
       
            Message loginMsg = new Message("LOGIN", userPayload);
            ClientConnection.getInstance().sendMessage(loginMsg);
        } catch (Exception e) {
            System.out.println("Lỗi gửi dữ liệu lên Server: " + e.getMessage());
        }
   

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Hệ thống đấu giá - Trang chủ");
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Không mở được màn hình chính!");
        }
    }

    @FXML
    public void goToSignup(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Signup.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Đăng ký tài khoản");
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Không mở được màn hình đăng ký!");
        }
    }
}