package app;

import app.server.ServerMain;

/**
 * Lớp khởi chạy trung gian để khắc phục lỗi "JavaFX runtime components are missing"
 * và tự động khởi động hệ thống máy chủ (Server) chạy ngầm.
 */
public class Launcher {
    public static void main(String[] args) {
        // 1. Chạy Server ở một luồng (thread) riêng biệt
        Thread serverThread = new Thread(() -> {
            try {
                ServerMain.main(new String[]{});
            } catch (Exception e) {
                System.err.println("Lỗi khởi động Server từ Launcher: " + e.getMessage());
            }
        });
        // Thiết lập Daemon để Server tự động tắt khi giao diện Client bị đóng
        serverThread.setDaemon(true); 
        serverThread.start();

        // 2. Dừng 1 giây để đảm bảo Server đã khởi động và sẵn sàng lắng nghe cổng 8080
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 3. Khởi động giao diện người dùng Client
        Main.main(args);
    }
}
