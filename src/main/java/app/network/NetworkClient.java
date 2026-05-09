package app.network;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NetworkClient {
    private static final String SERVER_IP = "127.0.0.1"; 
    private static final int PORT = 8888;

  
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
}
