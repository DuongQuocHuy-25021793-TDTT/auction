package app.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter; 
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

import app.database.AppDatabase;
import app.model.Account;
import app.model.AccountRole;
import app.model.Auction;
import app.model.BidTransaction;
import app.model.Message;

public class ClientHandler implements Runnable {
    private Socket socket;
    
    // Lưu trữ thông tin tài khoản đang kết nối ở luồng này sau khi LOGIN thành công
    private Account loggedInAccount = null; 

    private static Set<PrintWriter> clientWriters = new CopyOnWriteArraySet<>();

    private final Gson gson = new com.google.gson.GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new com.google.gson.TypeAdapter<LocalDateTime>() {
                @Override
                public void write(com.google.gson.stream.JsonWriter out, LocalDateTime value) throws java.io.IOException {
                    out.value(value != null ? value.toString() : null);
                }

                @Override
                public LocalDateTime read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
                    return LocalDateTime.parse(in.nextString());
                }
            }).create();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        PrintWriter out = null;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            clientWriters.add(out);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println(">>> SERVER NHẬN DỮ LIỆU: " + inputLine);

                Message message = gson.fromJson(inputLine, Message.class);
                if (message == null) continue;
                
                if ("READY".equals(message.getAction())) {
                    System.out.println("[+] Client kết nối để nhận thông báo từ server.");
                    continue;
                }
                else if ("BID".equals(message.getAction())) {
                    handleBidMessage(message, out); 
                } 
                else if ("GET_AUCTIONS".equals(message.getAction())) {
                    handleGetAuctions(out);
                }
                else if ("GET_HISTORY".equals(message.getAction())) {
                    handleGetHistory(message, out);
                }
                else if ("LOGIN".equals(message.getAction())) {
                    handleLogin(message, out);
                } 
                else if ("SIGNUP".equals(message.getAction())) {
                    handleSignup(message, out);
                }
                else if ("FORCE_CLOSE".equals(message.getAction())) {
                    handleForceClose(message, out);
                }
                else if ("ADD_PRODUCT".equals(message.getAction())) {
                    handleAddProduct(message, out);
                }
                else if ("LOGOUT".equals(message.getAction())) {
                    this.loggedInAccount = null;
                    out.println("SUCCESS");
                }
            }
        } catch (Exception e) {
            System.out.println("[-] Một client đã ngắt kết nối.");
        } finally {
            if (out != null) {
                clientWriters.remove(out);
            }
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * ĐÃ BỔ SUNG: Lấy danh sách toàn bộ phiên đấu giá gửi về cho Client dưới dạng JSON
     */
    private void handleGetAuctions(PrintWriter out) {
        try {
            java.util.List<Auction> auctions = AppDatabase.getInstance().getAuctions();
            String jsonResponse = gson.toJson(auctions);
            out.println(jsonResponse);
        } catch (Exception e) {
            System.err.println("[-] Lỗi khi lấy danh sách đấu giá: " + e.getMessage());
            out.println("[]");
        }
    }

    private void handleBidMessage(Message message, PrintWriter out) {
        BidTransaction bid = gson.fromJson(message.getData(), BidTransaction.class);

        System.out.println("=== THÔNG TIN ĐẤU GIÁ ===");
        System.out.println("ID phiên: " + bid.getAuctionId());
        System.out.println("ID người đấu giá: " + bid.getBidderId());
        System.out.println("Số tiền: " + bid.getBidAmount());

        Auction auction = AppDatabase.getInstance().findAuctionById(bid.getAuctionId());
        if (auction == null) {
            System.out.println("Không tìm thấy phiên đấu giá: " + bid.getAuctionId());
            out.println("ERROR"); 
            return;
        }

        synchronized (auction) {
            System.out.println("Auction status=" + auction.getStatus() + ", currentPrice=" + auction.getCurrentHighestPrice() + ", start=" + auction.getStartTime() + ", stop=" + auction.getStopTime());
            
            String validation = auction.validateBid(bid);
            if (validation != null) {
                System.out.println("Đặt giá bị từ chối: " + validation);
                out.println("FAIL_" + validation);
                return;
            }

            boolean success = auction.placeBid(bid);
            System.out.println("Kết quả placeBid: " + success);
            if (success) {
                boolean dbSuccess = AppDatabase.getInstance().placeBid(bid.getAuctionId(), bid.getBidderId(), bid.getBidAmount());
                if (!dbSuccess) {
                    System.out.println("Lỗi lưu bid vào database!");
                    out.println("ERROR");
                    return;
                }

                if ("FINISHED".equals(auction.getStatus())) {
                    AppDatabase.getInstance().updateAuctionStatus(bid.getAuctionId(), "FINISHED");
                }

                if (auction.getStopTime() != null) {
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    long remainingSeconds = java.time.Duration.between(now, auction.getStopTime()).getSeconds();

                    if (remainingSeconds <= 30 && remainingSeconds > 0) {
                        System.out.println("[ANTI-SNIPING] Kích hoạt gia hạn cho sản phẩm: " + auction.getItem().getName());
                        auction.setStopTime(auction.getStopTime().plusSeconds(60));
                        broadcast("TIME_EXTENDED", bid.getAuctionId());
                    }
                }

                System.out.println("Server đã cập nhật giá mới: " + auction.getCurrentHighestPrice());
                out.println("SUCCESS"); 
                broadcast("UPDATE_AUCTION", bid.getAuctionId());
            } else {
                System.out.println("Server từ chối yêu cầu đặt giá.");
                out.println("FAIL_UNKNOWN"); 
            }
        }
    }

    private void broadcast(String action, String data) {
        Message broadcastMsg = new Message(action, data);
        String json = gson.toJson(broadcastMsg);
        for (PrintWriter writer : clientWriters) {
            writer.println(json);
        }
    }

    private void handleGetHistory(Message message, PrintWriter out) {
        String auctionId = message.getData(); 
        java.util.List<String> history = AppDatabase.getInstance().getBidHistory(auctionId);
        out.println(gson.toJson(history)); 
    }

    private void handleLogin(Message message, PrintWriter out) {
        String[] parts = message.getData().split(":");
        if (parts.length == 2) {
            String user = parts[0];
            String pass = parts[1];

            Account account = AppDatabase.getInstance().authenticate(user, pass);
            
            if (account != null) {
                this.loggedInAccount = account;
                System.out.println(">>> Đăng nhập thành công: " + user + " (Quyền: " + account.getRole() + ")");
                out.println("SUCCESS|" + account.getRole().name() + "|" + account.getUsername());
            } else {
                System.out.println(">>> Đăng nhập thất bại: Sai tài khoản/mật khẩu.");
                out.println("FAIL");
            }
        } else {
            out.println("FAIL");
        }
    }

    private void handleSignup(Message message, PrintWriter out) {
        String[] parts = message.getData().split(":");
        if (parts.length == 2) {
            String user = parts[0];
            String pass = parts[1];

            if (AppDatabase.getInstance().usernameExists(user)) {
                System.out.println(">>> Đăng ký thất bại: Tài khoản " + user + " đã tồn tại.");
                out.println("FAIL_EXISTS");
            } else {
                Account newAccount = new Account(
                    "U_" + System.currentTimeMillis(), 
                    user, 
                    pass, 
                    AccountRole.GUEST 
                );
                
                boolean success = AppDatabase.getInstance().addAccount(newAccount);
                if (success) {
                    System.out.println(">>> Đăng ký thành công: " + user);
                    out.println("SUCCESS");
                } else {
                    out.println("FAIL");
                }
            }
        } else {
            out.println("FAIL");
        }
    }

    private void handleAddProduct(Message message, PrintWriter out) {
        if (loggedInAccount == null || loggedInAccount.getRole() != AccountRole.ADMIN) {
            System.out.println("[-] CẢNH BÁO BẢO MẬT: Phát hiện tài khoản không hợp lệ cố tình thêm sản phẩm!");
            out.println("FAIL_UNAUTHORIZED");
            return;
        }

        try {
            Auction newAuction = gson.fromJson(message.getData(), Auction.class);
            
            if (newAuction == null || newAuction.getItem() == null) {
                out.println("FAIL_INVALID_DATA");
                return;
            }

            newAuction.setStatus("RUNNING"); 
            newAuction.setCurrentHighestPrice(newAuction.getItem().getStartingPrice()); 
            
            boolean dbSuccess = AppDatabase.getInstance().addAuction(newAuction);
            
            if (dbSuccess) {
                System.out.println("[ADMIN] Đã thêm mới một sản phẩm đấu giá thành công: " + newAuction.getItem().getName());
                out.println("SUCCESS");
                broadcast("NEW_AUCTION_ADDED", newAuction.getId()); 
            } else {
                System.out.println("[-] Thêm sản phẩm thất bại tại Database lớp nghiệp vụ.");
                out.println("FAIL_DB_ERROR");
            }

        } catch (Exception e) {
            System.err.println("[-] Lỗi xử lý khi giải mã cấu trúc sản phẩm mới: " + e.getMessage());
            out.println("FAIL_SERVER_ERROR");
        }
    }

    private void handleForceClose(Message message, PrintWriter out) {
        if (loggedInAccount == null || loggedInAccount.getRole() != AccountRole.ADMIN) {
            System.out.println("[-] CẢNH BÁO BẢO MẬT: Phát hiện kết nối không hợp lệ cố tình gửi lệnh FORCE_CLOSE!");
            out.println("FAIL_UNAUTHORIZED");
            return;
        }

        String auctionId = message.getData(); 
        Auction auction = AppDatabase.getInstance().findAuctionById(auctionId);

        if (auction != null && "RUNNING".equals(auction.getStatus())) {
            synchronized (auction) {
                auction.setStatus("FINISHED");
                auction.setStopTime(java.time.LocalDateTime.now());
                
                AppDatabase.getInstance().updateAuctionStatus(auctionId, "FINISHED"); 

                System.out.println("[ADMIN] Đã thực hiện ép chốt sổ thành công cho phiên: " + auctionId);
                
                String winner = "Không có";
                java.util.List<String> bidHistory = AppDatabase.getInstance().getBidHistory(auctionId);
                if (bidHistory != null && !bidHistory.isEmpty()) {
                    winner = bidHistory.get(0).split(" ")[0]; 
                }
                
                out.println("SUCCESS");
                
                String broadcastData = auctionId + "|" + winner + "|" + auction.getCurrentHighestPrice();
                broadcast("AUCTION_CLOSED", broadcastData); 
            }
            
        } else {
            System.out.println("[-] Ép chốt thất bại: Phiên không tồn tại hoặc không ở trạng thái RUNNING.");
            out.println("FAIL_NOT_RUNNING");
        }
    }

    public void startAutoCloseChecker() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                java.util.List<Auction> allAuctions = AppDatabase.getInstance().getAuctions();
                if (allAuctions == null) return;

                for (Auction auction : allAuctions) {
                    try {
                        if (LocalDateTime.now().isAfter(auction.getStopTime()) && "RUNNING".equals(auction.getStatus())) {
                            synchronized (auction) {
                                if (!"RUNNING".equals(auction.getStatus())) continue;
                                
                                auction.setStatus("FINISHED");
                                AppDatabase.getInstance().updateAuctionStatus(auction.getId(), "FINISHED");
                                
                                String winner = "Không có"; 
                                java.util.List<String> bidHistory = AppDatabase.getInstance().getBidHistory(auction.getId());
                                if (bidHistory != null && !bidHistory.isEmpty()) {
                                    String topBid = bidHistory.get(0); 
                                    if (topBid != null && !topBid.trim().isEmpty()) {
                                        winner = topBid.split(" ")[0]; 
                                    }
                                }
                                
                                String broadcastData = auction.getId() + "|" + winner + "|" + auction.getCurrentHighestPrice();
                                broadcast("AUCTION_CLOSED", broadcastData);
                                
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