package app.controller;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

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
        String fullName = fullNameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            errorLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            errorLabel.setVisible(true);
            return;
        }

        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Mật khẩu xác nhận không khớp!");
            errorLabel.setVisible(true);
            return;
        }

        errorLabel.setText("Đăng ký thành công!");
        errorLabel.setVisible(true);
    }

    @FXML
    public void handleGoToLogin() {
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

