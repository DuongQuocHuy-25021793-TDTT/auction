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

    public static void main(String[] args) {
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
            

            while ((jsonMessage = in.readLine()) != null) {
                System.out.println("SERVER NHẬN ĐƯỢC TỪ CLIENT: " + jsonMessage);

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