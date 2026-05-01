package app.server;
    import java.io.IOException;
    import java.net.ServerSocket;
    import java.net.Socket;
    public class ServerMain{
        private static final int PORT = 8888;
        public static void main(String[] args) {
        System.out.println("=== HỆ THỐNG SERVER ĐẤU GIÁ ĐANG KHỞI ĐỘNG ===");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[*] Đang lắng nghe kết nối tại cổng " + PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[+] Có một Client vừa kết nối từ IP: " + clientSocket.getInetAddress());

            
                ClientHandler clientThread = new ClientHandler(clientSocket);
                new Thread(clientThread).start();
            }
        } catch (IOException e) {
            System.out.println("[-] Lỗi khởi động Server: " + e.getMessage());
        }
    }
}
    