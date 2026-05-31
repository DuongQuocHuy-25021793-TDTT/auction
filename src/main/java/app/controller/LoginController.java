package app.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.google.gson.Gson;

import app.database.Session;
<<<<<<< HEAD
import app.model.Account;
import app.model.AccountRole;
import app.model.Message;
=======
import app.model.User;
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533
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

    private final int SERVER_PORT = 8080;
    private final String SERVER_IP = "localhost";

    @FXML
    public void handleLoginButtonAction(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Vui lòng nhập đầy đủ thông tin!");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

<<<<<<< HEAD
        
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

         
            Message msg = new Message("LOGIN", username + ":" + password);
            Gson gson = new Gson();
            out.println(gson.toJson(msg));
=======
        User user = AppDatabase.getInstance().authenticate(username, password);
        if (user == null) {
            statusLabel.setText("Sai email hoặc mật khẩu!");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if ("PENDING".equals(user.getStatus())) {
            statusLabel.setText("Tài khoản đang chờ duyệt!");
            statusLabel.setStyle("-fx-text-fill: orange;");
            return;
        }

        if ("REJECTED".equals(user.getStatus())) {
            statusLabel.setText("Tài khoản đã bị từ chối!");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        if ("SUSPENDED".equals(user.getStatus())) {
            if (user.getSuspendedUntil() > 0 && System.currentTimeMillis() > user.getSuspendedUntil()) {
                // Hết hạn đình chỉ -> Khôi phục
                user.setStatus("ACTIVE");
                user.setSuspendedUntil(0);
                AppDatabase.getInstance().updateUserStatus(user);
            } else {
                if (user.getSuspendedUntil() == -1) {
                    statusLabel.setText("Tài khoản đã bị đình chỉ vĩnh viễn!");
                } else {
                    java.time.LocalDateTime date = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(user.getSuspendedUntil()), java.time.ZoneId.systemDefault());
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    statusLabel.setText("Tài khoản bị đình chỉ đến: " + date.format(formatter));
                }
                statusLabel.setStyle("-fx-text-fill: red;");
                return;
            }
        }

        Session.setCurrentUser(user);
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533

        
            String response = in.readLine();

            if ("SUCCESS".equals(response)) {
                
                Account account = new Account("U_Temp", username, password, AccountRole.GUEST);
                Session.setCurrentAccount(account);

               
                app.network.ClientConnection.getInstance().connect(SERVER_IP, SERVER_PORT);
              
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/Main.fxml"));
                Scene scene = new Scene(loader.load());

                Stage stage = (Stage) usernameField.getScene().getWindow();
                stage.setScene(scene);
                stage.setTitle("Auction System - Xin chào " + username);
            } else {
                statusLabel.setText("Sai tài khoản hoặc mật khẩu!");
                statusLabel.setStyle("-fx-text-fill: red;");
            }

<<<<<<< HEAD
=======
            Stage stage = (Stage) usernameField.getScene().getWindow();
            boolean wasMaximized = stage.isMaximized();
            if (wasMaximized) stage.setMaximized(false);
            stage.setScene(scene);
            stage.setMaximized(true);
            stage.setTitle("Auction System - " + user.getRole());
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Không thể kết nối đến Server. Hãy kiểm tra xem Server đã chạy chưa!");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void goToSignup(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/Signup.fxml"));
            Scene scene = new Scene(loader.load());

            Stage stage = (Stage) usernameField.getScene().getWindow();
            boolean wasMaximized = stage.isMaximized();
            if (wasMaximized) stage.setMaximized(false);
            stage.setScene(scene);
            if (wasMaximized) stage.setMaximized(true);
            stage.setTitle("Đăng ký tài khoản");
        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Không mở được màn hình đăng ký!");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }
}