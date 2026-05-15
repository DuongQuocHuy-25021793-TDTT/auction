package app.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 8080;

    private ExecutorService clientPool = Executors.newFixedThreadPool(50);
    
    public static final List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("===   SERVER ĐẤU GIÁ ĐANG CHẠY Ở PORT " + PORT + " ===");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println(" [+] Có Client mới kết nối: " + clientSocket.getInetAddress());

                ClientHandler handler = new ClientHandler(clientSocket);
                activeClients.add(handler);
      
                clientPool.execute(handler);
            }
        } catch (IOException e) {
            System.err.println(" Lỗi khởi chạy Server: " + e.getMessage());
        }
    }

    public static void broadcast(String message) {
        for (ClientHandler client : activeClients) {
            client.sendMessage(message);
        }
    }

    public static void startAuctionChecker() {
        Thread checkerThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    List<app.model.Auction> auctions = app.database.AppDatabase.getInstance().getAuctions();
                    
                    for (app.model.Auction auction : auctions) {
                       
                        if ("RUNNING".equals(auction.getStatus()) && auction.getRemainingTime() <= 0) {
                            
                       
                            auction.setStatus("FINISHED");

                          
                            String winner = auction.getHighestBidderId();
                            if (winner == null || winner.isEmpty()) {
                                winner = "null"; 
                            }
                            double finalPrice = auction.getCurrentHighestPrice();

                        
                            System.out.println("=========================================");
                            System.out.println(">>> PHIÊN ĐẤU GIÁ KẾT THÚC: " + auction.getItem().getName());
                            System.out.println(">>> TRẠNG THÁI ĐÃ ĐƯỢC CHỐT SỔ THÀNH CÔNG");
                            
                           
                            broadcast("AUCTION_CLOSED|" + auction.getId() + "|" + winner + "|" + finalPrice);
                            
                            System.out.println("=========================================\n");
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("Tiến trình kiểm tra bị gián đoạn.");
                    break;
                } catch (Exception e) {
                    System.out.println("Lỗi khi kiểm tra thời gian đấu giá: " + e.getMessage());
                }
            }
        });
        
        checkerThread.setDaemon(true); 
        checkerThread.start();
    }


    public static void main(String[] args) {
       
        startAuctionChecker();

        Server server = new Server();
        server.startServer();
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            String jsonMessage;

    
            com.google.gson.Gson gson = new com.google.gson.Gson();
            
           
            com.google.gson.Gson gsonDate = new com.google.gson.GsonBuilder()
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

            while ((jsonMessage = in.readLine()) != null) {
                System.out.println("SERVER NHẬN ĐƯỢC: " + jsonMessage);

                try {
                    
                    app.model.Message msg = gson.fromJson(jsonMessage, app.model.Message.class);
                    if (msg == null) continue;

                   
                    switch (msg.getCommand()) {
                        
                        case "BID":
                        
                            app.model.BidTransaction bid = gsonDate.fromJson(msg.getData(), app.model.BidTransaction.class);
                            app.model.Auction auction = app.database.AppDatabase.getInstance().findAuctionById(bid.getAuctionId());
                            
                            if (auction != null) {
                                
                                String validation = auction.validateBid(bid);
                                
                                if (validation == null) {
                                    auction.placeBid(bid); 
                                    sendMessage("SUCCESS"); 
                                    
                                 
                                    Server.broadcast("UPDATE_AUCTION|" + auction.getId()); 
                                } else {
                                   
                                    sendMessage("FAIL_" + validation); 
                                }
                            } else {
                                sendMessage("ERROR");
                            }
                            break;

                        case "GET_HISTORY":
                            String reqAuctionId = msg.getData();
                           
                            List<String> historyStrings = app.database.AppDatabase.getInstance().getBidHistory(reqAuctionId);
                           
                            sendMessage(gson.toJson(historyStrings));
                            break;

                        default:
                            System.out.println("Không nhận diện được lệnh: " + msg.getCommand());
                            break;
                    }

                } catch (Exception e) {
                    System.out.println("Lỗi xử lý JSON từ Client: " + e.getMessage());
                    sendMessage("ERROR");
                }
            }
        } catch (IOException e) {
            System.out.println(" [-] Một Client đã ngắt kết nối.");
        } finally {
            Server.activeClients.remove(this);
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}