package app.controller;

import app.database.AppDatabase;
import app.database.Session;
import app.model.Account;
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
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        Account account = AppDatabase.getInstance().authenticate(username, password);
        if (account == null) {
            statusLabel.setText("Sai email hoặc mật khẩu!");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        Session.setCurrentAccount(account);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Auction System - " + account.getRole());
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
