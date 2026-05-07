package app.controller;

import java.util.UUID;

import app.database.AppDatabase;
import app.model.AccountRole;
import app.model.Bidder;
import app.model.Seller;
import app.model.User;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;

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
    private ComboBox<AccountRole> roleComboBox;

    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        roleComboBox.setItems(FXCollections.observableArrayList(AccountRole.BIDDER, AccountRole.SELLER));
        roleComboBox.setValue(AccountRole.BIDDER);
    }

    @FXML
    public void handleRegisterAction() {
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        AccountRole selectedRole = roleComboBox.getValue();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showMessage("Vui lòng nhập đầy đủ thông tin!", "red");
            return;
        }

        if (selectedRole == null || !selectedRole.canSelfRegister()) {
            showMessage("Chỉ được đăng ký tài khoản Bidder hoặc Seller!", "red");
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

        User user = createUser(selectedRole, email, password);
        if (!database.addUser(user)) {
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

    private User createUser(AccountRole role, String username, String password) {
        String id = "U_" + UUID.randomUUID();
        if (role == AccountRole.SELLER) {
            return new Seller(id, username, password);
        }
        return new Bidder(id, username, password);
    }

    private void showMessage(String message, String color) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: " + color + ";");
        errorLabel.setVisible(true);
    }

    private void goBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/Login.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) fullNameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Đăng nhập");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
