package app.controller;

import app.database.AppDatabase;
import app.model.Account;
import app.model.AccountRole;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.UUID;

public class SignupController {

    @FXML
    private TextField fullNameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label errorLabel;

    @FXML
    public void handleRegisterAction() {
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        try {
            app.model.Seller newSeller = new app.model.Seller("ID_TEMP", email, password);

            app.SecurityUtils.saveAccount(newSeller, "account_data.json");

            System.out.println("Đăng ký thành công! Đã lưu và mã hóa vào account_data.json");
        } catch (Exception e) {
            System.out.println("Lỗi lưu file: " + e.getMessage());
        }

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showMessage("Vui lòng nhập đầy đủ thông tin!", "red");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showMessage("Mật khẩu xác nhận không khớp!", "red");
            return;
        }

        AppDatabase database = AppDatabase.getInstance();
        if (database.usernameExists(email)) {
            showMessage("Tài khoản đã tồn tại", "red");
            return;
        }

        Account account = new Account(
                "U_" + UUID.randomUUID(),
                email,
                password,
                AccountRole.GUEST
        );

        if (!database.addAccount(account)) {
            showMessage("Không thể tạo tài khoản!", "red");
            return;
        }

        showMessage("Đăng ký thành công!", "green");

        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        pause.setOnFinished(e -> goBackToLogin());
        pause.play();
    }

    @FXML
    public void handleGoToLogin() {
        goBackToLogin();
    }

    private void showMessage(String message, String color) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: " + color + ";");
        errorLabel.setVisible(true);
    }

    private void goBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) fullNameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Đăng nhập");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
