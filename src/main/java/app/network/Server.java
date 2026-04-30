package app.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 8080;

    private ExecutorService clientPool = Executors.newFixedThreadPool(50);

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("===  SERVER ĐẤU GIÁ ĐANG CHẠY Ở PORT " + PORT + " ===");

            while (true) {
         
                Socket clientSocket = serverSocket.accept();
                System.out.println(" [+] Có Client mới kết nối: " + clientSocket.getInetAddress());

      
                clientPool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println(" Lỗi khởi chạy Server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.startServer();
    }
}


class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String jsonMessage;
            

            while ((jsonMessage = in.readLine()) != null) {
                System.out.println("SERVER NHẬN ĐƯỢC TỪ CLIENT: " + jsonMessage);

            }
        } catch (IOException e) {
            System.out.println(" [-] Một Client đã ngắt kết nối.");
        }
    }
}
