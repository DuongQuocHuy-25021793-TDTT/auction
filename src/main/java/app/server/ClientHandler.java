package app.server;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;

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
                
            }
        } catch (Exception e) {
            System.out.println("[-] Một Client đã ngắt kết nối.");
        }
    }
}
    

