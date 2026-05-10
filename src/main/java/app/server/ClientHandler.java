package app.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter; 
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
            
          
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
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
  
            }
        } catch (Exception e) {
            System.out.println("[-] Một client đã ngắt kết nối.");
            e.printStackTrace();
        }
    }


    private void handleBidMessage(Message message, PrintWriter out) {
        BidTransaction bid = gson.fromJson(message.getData(), BidTransaction.class);

        System.out.println("=== THÔNG TIN ĐẤU GIÁ ===");
        System.out.println("ID phiên: " + bid.getAuctionId());
        System.out.println("ID người đấu giá: " + bid.getBidderId());
        System.out.println("Số tiền: " + bid.getBidAmount());
        System.out.println("Thời gian: " + bid.getTimestamp());

        Auction auction = AppDatabase.getInstance().findAuctionById(bid.getAuctionId());
        if (auction == null) {
            System.out.println("Không tìm thấy phiên đấu giá: " + bid.getAuctionId());
          
            out.println("ERROR"); 
            return;
        }

        boolean success = auction.placeBid(bid);
        if (success) {
            System.out.println("Server đã cập nhật giá mới: " + auction.getCurrentHighestPrice());
            
            out.println("SUCCESS"); 
        } else {
            System.out.println("Server từ chối yêu cầu đặt giá.");
            
            out.println("FAIL"); 
        }
    }


    private void handleGetHistory(Message message, PrintWriter out) {
        // 1. Lấy ID món đồ từ gói tin Message
        String auctionId = message.getData(); 
        
    
        java.util.List<String> history = AppDatabase.getInstance().getBidHistory(auctionId);
      
        out.println(gson.toJson(history)); 
    }

}