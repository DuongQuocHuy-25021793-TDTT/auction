package app.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

import com.google.gson.Gson;
import app.model.Message;
import app.model.BidTransaction;


public class ClientHandler implements Runnable {
    private Socket socket;
    private Gson gson = new com.google.gson.GsonBuilder() //Gson mặc định có thể gặp vấn đề khi parse LocalDateTime, đặc biệt trên Java mới.
            .registerTypeAdapter(java.time.LocalDateTime.class, new com.google.gson.TypeAdapter<java.time.LocalDateTime>() {
                @Override
                public void write(com.google.gson.stream.JsonWriter out, java.time.LocalDateTime value) throws java.io.IOException {
                    out.value(value != null ? value.toString() : null);
                }

                @Override
                public java.time.LocalDateTime read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
                    return java.time.LocalDateTime.parse(in.nextString());
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
                System.out.println(">>> [SERVER NHẬN ĐƯỢC DỮ LIỆU]: " + inputLine);

                Message message = gson.fromJson(inputLine, Message.class);

                if ("BID".equals(message.getAction())) {
                    BidTransaction bid = gson.fromJson(message.getData(), BidTransaction.class);

                    System.out.println("=== THÔNG TIN ĐẶT GIÁ ===");
                    System.out.println("ID Phiên: " + bid.getAuctionId());
                    System.out.println("ID Người đấu giá: " + bid.getBidderId());
                    System.out.println("Số tiền: " + bid.getBidAmount());
                    System.out.println("Time: " + bid.getTimestamp());
                }
            }
        } catch (Exception e) {
            System.out.println("[-] Một Client đã ngắt kết nối.");
            e.printStackTrace();
        }
    }
}
    

