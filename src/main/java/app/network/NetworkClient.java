package app.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.google.gson.Gson;

import app.model.Message;
import javafx.application.Platform;

public class NetworkClient {
    private static final String SERVER_IP = "127.0.0.1"; 
    private static final int PORT = 8888;

    private static Socket listenSocket;

    
    public interface AuctionUpdateListener {
        void onAuctionUpdated(String auctionId);
    }

    public static String sendRequest(String jsonRequest) {
        try (Socket socket = new Socket(SERVER_IP, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            out.println(jsonRequest);
            return in.readLine();
            
        } catch (Exception e) {
            return "ERROR";
        }
    }

    public static void startRealtimeListener(AuctionUpdateListener listener) {
        
        new Thread(() -> {
            try {
                listenSocket = new Socket(SERVER_IP, PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(listenSocket.getInputStream()));
                Gson gson = new Gson();
                String inputLine;

         
                while ((inputLine = in.readLine()) != null) {
                    try {
                        Message msg = gson.fromJson(inputLine, Message.class);
                        if ("UPDATE_AUCTION".equals(msg.getAction())) {
                            String auctionId = msg.getData(); 
                            Platform.runLater(() -> {
                                listener.onAuctionUpdated(auctionId);
                            });
                        }
                    } catch (Exception ex) {
                       
                    }
                }
            } catch (Exception e) {
                System.out.println("[-] Mất kết nối Realtime với Server.");
            }
        }).start(); 
    }
    public static void stopRealtimeListener() {
        try {
            if (listenSocket != null && !listenSocket.isClosed()) {
                listenSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}