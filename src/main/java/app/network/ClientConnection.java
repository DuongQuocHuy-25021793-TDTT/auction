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


    public interface MessageListener {
        void onMessageReceived(String message);
    }
    private MessageListener messageListener;

    public void setMessageListener(MessageListener listener) {
        this.messageListener = listener;
    }

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
            
         
            Message readyMsg = new Message("READY", "LISTENING");
            out.println(gson.toJson(readyMsg));
            
            startListening();
            
        } catch (IOException e) {
            System.out.println("Không thể kết nối đến Server: " + e.getMessage());
        }
    }

    
    private void startListening() {
        Thread listenerThread = new Thread(() -> {
            try {
                String serverMessage;
              
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println("Client nhận được từ Server: " + serverMessage);
                    
             
                    if (messageListener != null) {
                        messageListener.onMessageReceived(serverMessage);
                    }
                }
            } catch (IOException e) {
                System.out.println("Ngắt kết nối / Lỗi đọc dữ liệu từ Server.");
            }
        });
        listenerThread.setDaemon(true); 
        listenerThread.start();
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