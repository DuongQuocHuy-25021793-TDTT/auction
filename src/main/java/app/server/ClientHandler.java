package app.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter; 
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.gson.Gson;

import app.database.AppDatabase;
import app.model.Account;
import app.model.AccountRole;
import app.model.Auction;
import app.model.BidTransaction;
import app.model.Message;

public class ClientHandler implements Runnable {
    private Socket socket;
    

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
                
                if ("BID".equals(message.getAction())) {
                    handleBidMessage(message, out); 
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
            }
        } catch (Exception e) {
            System.out.println("[-] Một client đã ngắt kết nối.");
        } finally {
        
            if (out != null) {
                clientWriters.remove(out);
            }
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

        boolean success = auction.placeBid(bid);
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
            System.out.println("Server đã cập nhật giá mới: " + auction.getCurrentHighestPrice());
            out.println("SUCCESS"); 
            
     
            broadcast("UPDATE_AUCTION", bid.getAuctionId());
            
        } else {
            System.out.println("Server từ chối yêu cầu đặt giá.");
            out.println("FAIL"); 
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
                System.out.println(">>> Đăng nhập thành công: " + user);
                out.println("SUCCESS");
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
}