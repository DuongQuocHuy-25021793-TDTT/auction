package app.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.PrintWriter;

import com.google.gson.Gson;

import app.database.AppDatabase;
import app.model.Auction;
import app.model.BidTransaction;
import app.model.Message;

public class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;
    public static final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

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
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            clients.add(this);
            
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                System.out.println(">>> SERVER NHẬN DỮ LIỆU: " + inputLine);

                Message message = gson.fromJson(inputLine, Message.class);
                if ("BID".equals(message.getAction())) {
                    handleBidMessage(message);
                }
            }
        } catch (Exception e) {
            System.out.println("[-] Một client đã gặp lỗi.");
            e.printStackTrace();
        } finally {
            clients.remove(this);
            System.out.println("[-] Một client đã ngắt kết nối. Còn lại: " + clients.size());
        }
    }

    private void handleBidMessage(Message message) {
        BidTransaction bid = gson.fromJson(message.getData(), BidTransaction.class);

        System.out.println("=== THÔNG TIN ĐẤU GIÁ ===");
        System.out.println("ID phiên: " + bid.getAuctionId());
        System.out.println("ID người đấu giá: " + bid.getBidderId());
        System.out.println("Số tiền: " + bid.getBidAmount());
        System.out.println("Thời gian: " + bid.getTimestamp());

        Auction auction = AppDatabase.getInstance().findAuctionById(bid.getAuctionId());
        if (auction == null) {
            System.out.println("Không tìm thấy phiên đấu giá: " + bid.getAuctionId());
            return;
        }

        boolean success = auction.placeBid(bid);
        if (success) {
            AppDatabase.getInstance().updateAuctionPrice(auction.getId(), auction.getCurrentHighestPrice(), auction.getHighestBidderId());
            AppDatabase.getInstance().saveBidTransaction(bid);
            
            // Cập nhật lại stopTime lên Database (để lưu Sniper Protection nếu có)
            AppDatabase.getInstance().updateAuctionStopTime(auction.getId(), auction.getStopTime().toString());

            System.out.println("Server đã cập nhật giá mới và lưu vào CSDL: " + auction.getCurrentHighestPrice());
            broadcast(message);
        } else {
            System.out.println("Server từ chối yêu cầu đặt giá.");
        }
    }

    private void broadcast(Message message) {
        String json = gson.toJson(message);
        for (ClientHandler client : clients) {
            if (client != this) {
                client.sendMessage(json);
            }
        }
    }

    public void sendMessage(String json) {
        if (out != null) {
            out.println(json);
        }
    }
}
