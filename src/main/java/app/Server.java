package app;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 8080;
    private ExecutorService clientPool = Executors.newFixedThreadPool(50);

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("=== 🚀 SERVER ĐẤU GIÁ ĐANG CHẠY Ở PORT " + PORT + " ===");

            while (true) {

                Socket clientSocket = serverSocket.accept();
                System.out.println("🟢 Có Client mới kết nối: " + clientSocket.getInetAddress());

            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi khởi chạy Server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Khởi chạy Server
        Server server = new Server();
        server.startServer();
    }
}