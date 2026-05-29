package app.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import app.database.AppDatabase;
import app.model.Auction;
import app.model.BidTransaction;
import app.model.Message;

public class Server {
    private static final int PORT = 8080;
    private final ExecutorService clientPool = Executors.newFixedThreadPool(50);
    public static final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("=================================================");
            System.out.println("===   SERVER ĐẤU GIÁ ĐANG CHẠY Ở PORT " + PORT + " ===");
            System.out.println("=================================================");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println(" [+] Có Client mới kết nối từ IP: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                activeClients.add(handler);
                clientPool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println(" [!] Lỗi khởi chạy Server: " + e.getMessage());
        }
    }

    /**
     * PHÁT SÓNG TOÀN HỆ THỐNG
     * Có bọc try-catch nội bộ để tránh việc một Client mất mạng làm nghẽn luồng phát sóng
     */
    public static void broadcast(String message) {
        for (ClientHandler client : activeClients) {
            try {
                client.sendMessage(message);
            } catch (Exception e) {
                activeClients.remove(client);
            }
        }
    }

    /**
     * LUỒNG TỰ ĐỘNG QUÉT VÀ CHỐT PHIÊN ĐẤU GIÁ HẾT GIỜ
     */
    public static void startAutoCloseChecker() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<Auction> allAuctions = AppDatabase.getInstance().getAuctions();
                if (allAuctions == null) return;

                for (Auction auction : allAuctions) {
                    try {
                        // Nếu đã quá giờ và trạng thái trong DB vẫn là RUNNING
                        if (LocalDateTime.now().isAfter(auction.getStopTime()) && "RUNNING".equalsIgnoreCase(auction.getStatus())) {
                            
                            // ĐÃ SỬA: Cập nhật trực tiếp xuống SQLite Database thay vì chỉ sửa trên RAM
                            AppDatabase.getInstance().updateAuctionStatus(auction.getId(), "FINISHED");
                            
                            String winner = "Không có";
                            double highestPrice = auction.getCurrentHighestPrice();
                            
                            List<String> bidHistory = AppDatabase.getInstance().getBidHistory(auction.getId());
                            
                            // ĐÃ SỬA: Vị trí Index 0 luôn là giá CAO NHẤT do câu lệnh SQL ORDER BY DESC
                            if (bidHistory != null && !bidHistory.isEmpty()) {
                                String topBid = bidHistory.get(0); 
                                if (topBid != null && !topBid.trim().isEmpty()) {
                                    winner = topBid.split(" ")[0]; // Tách lấy Username người đặt
                                }
                            }
                            
                            String closeMessage = "AUCTION_CLOSED|" + auction.getId() + "|" + winner + "|" + highestPrice;
                            broadcast(closeMessage);
                            
                            System.out.println("=========================================");
                            System.out.println(">>> PHIÊN ĐẤU GIÁ HẾT GIỜ (TỰ ĐỘNG): " + auction.getItem().getName());
                            System.out.println(">>> TRẠNG THÁI: ĐÃ CHỐT XUỐNG CƠ SỞ DỮ LIỆU SQLITE");
                            System.out.println(">>> Người thắng cuộc: " + winner + " | Giá chốt: " + highestPrice + " USD");
                            System.out.println("=========================================\n");
                        }
                    } catch (Exception e) {
                        System.err.println(" Lỗi xử lý chốt phiên tự động cho mã " + auction.getId() + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.err.println(" Lỗi nghiêm trọng trong luồng quét phiên: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        // Đảm bảo Database được nạp cấu trúc bảng trước
        AppDatabase.getInstance();
        
        // Khởi động bộ quét tự động chốt sổ trước
        startAutoCloseChecker();

        // Mở cổng mạng chờ Client kết nối
        Server server = new Server();
        server.startServer();
    }
}

/**
 * LỚP XỬ LÝ LUỒNG GIAO TIẾP CHO TỪNG CLIENT RIÊNG BIỆT
 */
class ClientHandler implements Runnable {
    private final Socket socket;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            this.out = writer;
            String jsonMessage;

            Gson gson = new Gson();
            Gson gsonDate = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new com.google.gson.TypeAdapter<LocalDateTime>() {
                    @Override
                    public void write(com.google.gson.stream.JsonWriter out, LocalDateTime value) throws IOException {
                        out.value(value != null ? value.toString() : null);
                    }
                    @Override
                    public LocalDateTime read(com.google.gson.stream.JsonReader in) throws IOException {
                        return LocalDateTime.parse(in.nextString());
                    }
                }).create();

            while ((jsonMessage = in.readLine()) != null) {
                System.out.println("[SERVER RECEIVE]: " + jsonMessage);

                try {
                    Message msg = gson.fromJson(jsonMessage, Message.class);
                    if (msg == null) continue;

                    switch (msg.getCommand()) {
                        
                        case "BID":
                            BidTransaction bid = gsonDate.fromJson(msg.getData(), BidTransaction.class);
                            // ĐA SỬA: Lấy dữ liệu mới nhất từ SQLite ra để so sánh giá
                            Auction auction = AppDatabase.getInstance().findAuctionById(bid.getAuctionId());
                            
                            if (auction != null) {
                                String validation = auction.validateBid(bid);
                                
                                if (validation == null) {
                                    // ĐÃ SỬA: Lưu lượt đặt giá mới và cập nhật giá đỉnh vào SQLite DB công khai
                                    boolean dbSuccess = AppDatabase.getInstance().placeBid(bid.getAuctionId(), bid.getUsername(), bid.getBidAmount());
                                    
                                    if (dbSuccess) {
                                        sendMessage("SUCCESS"); 
                                        Server.broadcast("UPDATE_AUCTION|" + auction.getId()); 
                                    } else {
                                        sendMessage("FAIL_DATABASE_ERROR");
                                    }
                                } else {
                                    sendMessage("FAIL_" + validation); 
                                }
                            } else {
                                sendMessage("ERROR");
                            }
                            break;

                        case "GET_HISTORY":
                            String reqAuctionId = msg.getData();
                            List<String> historyStrings = AppDatabase.getInstance().getBidHistory(reqAuctionId);
                            sendMessage(gson.toJson(historyStrings));
                            break;

                        case "FORCE_CLOSE":
                            String forceAuctionId = msg.getData();
                            Auction forceAuction = AppDatabase.getInstance().findAuctionById(forceAuctionId);
                            
                            if (forceAuction != null && "RUNNING".equalsIgnoreCase(forceAuction.getStatus())) {
                                // ĐÃ SỬA: Chốt trạng thái trực tiếp xuống SQLite
                                AppDatabase.getInstance().updateAuctionStatus(forceAuctionId, "FINISHED");
                                AppDatabase.getInstance().updateAuctionStopTime(forceAuctionId, LocalDateTime.now());
                                
                                String forceWinner = "Không có";
                                double forceHighestPrice = forceAuction.getCurrentHighestPrice();
                                
                                List<String> forceBidHistory = AppDatabase.getInstance().getBidHistory(forceAuctionId);
                                // ĐÃ SỬA: Lấy phần tử Index 0 là giá cao nhất
                                if (forceBidHistory != null && !forceBidHistory.isEmpty()) {
                                    String topBid = forceBidHistory.get(0); 
                                    if (topBid != null && !topBid.trim().isEmpty()) {
                                        forceWinner = topBid.split(" ")[0]; 
                                    }
                                }
                                
                                String closeMessage = "AUCTION_CLOSED|" + forceAuctionId + "|" + forceWinner + "|" + forceHighestPrice;
                                Server.broadcast(closeMessage);
                                
                                System.out.println("[HỆ THỐNG - ÉP CHỐT ĐÃ LƯU DB] Phiên: " + forceAuctionId + " | Người thắng: " + forceWinner);
                                sendMessage("SUCCESS");
                            } else {
                                sendMessage("FAIL_NOT_RUNNING");
                            }
                            break;

                        default:
                            System.out.println(" [!] Không nhận diện được lệnh: " + msg.getCommand());
                            break;
                    }

                } catch (Exception e) {
                    System.err.println(" Lỗi giải mã gói tin JSON từ Client: " + e.getMessage());
                    sendMessage("ERROR");
                }
            }
        } catch (IOException e) {
            System.out.println(" [-] Một Client đã ngắt kết nối đột ngột.");
        } finally {
            // ĐÃ SỬA: Đảm bảo dọn dẹp bộ nhớ và đóng Socket an toàn chống leak cổng mạng
            Server.activeClients.remove(this);
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}