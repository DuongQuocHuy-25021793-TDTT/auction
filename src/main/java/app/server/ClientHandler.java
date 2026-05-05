package app.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.LocalDateTime;

import com.google.gson.Gson;

import app.database.AppDatabase;
import app.model.Auction;
import app.model.BidTransaction;
import app.model.Message;

public class ClientHandler implements Runnable {
    private Socket socket;

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
            System.out.println("[-] Một client đã ngắt kết nối.");
            e.printStackTrace();
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
            System.out.println("Server đã cập nhật giá mới: " + auction.getCurrentHighestPrice());
        } else {
            System.out.println("Server từ chối yêu cầu đặt giá.");
        }
    }
}
