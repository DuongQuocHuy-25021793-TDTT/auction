package app.server;
    import java.io.IOException;
    import java.net.ServerSocket;
    import java.net.Socket;
    import java.time.LocalDateTime;
    import java.util.concurrent.Executors;
    import java.util.concurrent.ScheduledExecutorService;
    import java.util.concurrent.TimeUnit;
    import java.util.List;
    
    import app.database.AppDatabase;
    import app.model.Auction;
    import app.model.BidTransaction;
    public class ServerMain{
        private static final int PORT = 8080;
        public static void main(String[] args) {
        System.out.println("=== HỆ THỐNG SERVER ĐẤU GIÁ ĐANG KHỞI ĐỘNG ===");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[*] Đang lắng nghe kết nối tại cổng " + PORT + "...");
            
            startAutoCloseChecker();

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

    public static void startAutoCloseChecker() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<Auction> allAuctions = AppDatabase.getInstance().getAuctions();
                if (allAuctions == null) return;

                for (Auction auction : allAuctions) {
                    try {
                        if (LocalDateTime.now().isAfter(auction.getStopTime()) && "RUNNING".equals(auction.getStatus())) {
                            synchronized (auction) {
                                if (!"RUNNING".equals(auction.getStatus())) continue;
                                
                                AppDatabase.getInstance().stopAuction(auction.getId());
                                
                                String winner = "Không có"; 
                                List<BidTransaction> bidHistory = AppDatabase.getInstance().getBidHistory(auction.getId());
                                if (bidHistory != null && !bidHistory.isEmpty()) {
                                    BidTransaction topBid = bidHistory.get(bidHistory.size() - 1); 
                                    if (topBid != null) {
                                        winner = topBid.getBidderId(); 
                                    }
                                }
                                
                                System.out.println("[HỆ THỐNG TỰ ĐỘNG] Đã hết giờ và chốt phiên " + auction.getId() + ". Người thắng: " + winner + " | Giá: " + auction.getCurrentHighestPrice() + " USD");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi xử lý chốt phiên cho mã " + auction.getId() + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println("Lỗi nghiêm trọng trong luồng quét phiên: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}
    