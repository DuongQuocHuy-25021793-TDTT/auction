package app.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnection {
    private static ClientConnection instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;


    private ClientConnection() {}

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

    public void sendRequest(String jsonMessage) {
        if (out != null) {
            out.println(jsonMessage);
        }
    }
}