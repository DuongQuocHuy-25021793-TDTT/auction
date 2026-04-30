package app.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.google.gson.Gson;

import app.model.Message;

public class ClientConnection {
    private static ClientConnection instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson; 

    private ClientConnection() {
        gson = new Gson(); 
    }

    public static ClientConnection getInstance() {
        if (instance == null) {
            instance = new ClientConnection();
        }
        return instance;
    }

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Đã kết nối tới Server thành công!");
        } catch (IOException e) {
            System.out.println("Không thể kết nối đến Server: " + e.getMessage());
        }
    }

    public void sendMessage(Message message) {
        if (out != null) {
            String jsonString = gson.toJson(message); 
            out.println(jsonString); 
            System.out.println("Client gửi: " + jsonString);
        } else {
            System.out.println("Lỗi: Chưa kết nối tới Server!");
        }
    }
}