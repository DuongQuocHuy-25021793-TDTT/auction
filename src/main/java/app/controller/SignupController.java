package app.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

<<<<<<< HEAD
import com.google.gson.Gson;

import app.model.Message;
=======
import app.database.AppDatabase;
import app.model.AccountRole;
import app.model.Bidder;
import app.model.Seller;
import app.model.User;
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533
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


    private final int SERVER_PORT = 8080;
    private final String SERVER_IP = "localhost";

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
<<<<<<< HEAD

=======
        AccountRole selectedRole = roleComboBox.getValue();
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533

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

        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Message msg = new Message("SIGNUP", email + ":" + password);
            Gson gson = new Gson();
            out.println(gson.toJson(msg));

         
            String response = in.readLine();

            if ("SUCCESS".equals(response)) {
                showMessage("Đăng ký thành công!", "green");

                
                PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
                pause.setOnFinished(e -> goBackToLogin());
                pause.play();
            } else if ("FAIL_EXISTS".equals(response)) {
                showMessage("Tài khoản (Email) đã tồn tại!", "red");
            } else {
                showMessage("Không thể tạo tài khoản!", "red");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Không thể kết nối đến Server!", "red");
        }
<<<<<<< HEAD
=======

        User user = createUser(selectedRole, email, password);
        user.setFullName(fullName);
        if (selectedRole == AccountRole.SELLER) {
            user.setStatus("PENDING");
        } else {
            user.setStatus("ACTIVE");
        }

        if (!database.addUser(user)) {
            showMessage("Không thể tạo tài khoản!", "red");
            return;
        }

        if (selectedRole == AccountRole.SELLER) {
            showMessage("Đăng ký thành công! Tài khoản cần được Admin duyệt.", "green");
            PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
            pause.setOnFinished(e -> goBackToLogin());
            pause.play();
        } else {
            showMessage("Đăng ký thành công!", "green");
            PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
            pause.setOnFinished(e -> goBackToLogin());
            pause.play();
        }
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533
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
            boolean wasMaximized = stage.isMaximized();
            if (wasMaximized) stage.setMaximized(false);
            stage.setScene(scene);
            if (wasMaximized) stage.setMaximized(true);
            stage.setTitle("Đăng nhập");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}