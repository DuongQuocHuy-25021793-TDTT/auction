package app.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.gson.Gson;

import app.model.Message;

public class ClientConnection {
    private static ClientConnection instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson; 
    
    public interface MessageListener {
        void onMessageReceived(Message message);
    }
    
    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();

    private ClientConnection() {
        gson = new Gson(); 
    }

    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
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
            
            Thread listenerThread = new Thread(() -> {
                try {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        try {
                            Message message = gson.fromJson(inputLine, Message.class);
                            for (MessageListener listener : listeners) {
                                listener.onMessageReceived(message);
                            }
                        } catch (Exception ex) {
                            System.out.println("Lỗi khi parse tin nhắn từ server: " + ex.getMessage());
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Mất kết nối tới Server: " + e.getMessage());
                }
            });
            listenerThread.setDaemon(true);
            listenerThread.start();
            
        } catch (IOException e) {
            System.out.println("Không thể kết nối đến Server: " + e.getMessage());
        }
    }

    public void sendMessage(Message message) {
        if (out != null) {
            new Thread(() -> {
                String jsonString = gson.toJson(message); 
                out.println(jsonString); 
                System.out.println("Client gửi: " + jsonString);
            }).start();
        } else {
            System.out.println("Lỗi: Chưa kết nối tới Server!");
        }
    }
}