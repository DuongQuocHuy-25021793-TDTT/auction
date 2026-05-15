package app.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import app.model.Account;
import app.model.AccountRole;
import app.model.Art;
import app.model.Auction;
import app.model.Electronics;
import app.model.Item;

public class AppDatabase {
    private static final AppDatabase INSTANCE = new AppDatabase();
    private static final String DB_URL = "jdbc:sqlite:auction_app.db";

    private AppDatabase() {
        createTables();
        if (getAccounts().isEmpty()) seedAccounts();
        if (getAuctions().isEmpty()) seedInventory();
        
       
        startAuctionMonitor();
    }

    public static AppDatabase getInstance() { return INSTANCE; }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }


    private void createTables() {
        String sqlAccounts = "CREATE TABLE IF NOT EXISTS Accounts (username TEXT PRIMARY KEY, password TEXT, role TEXT);";
        String sqlItems = "CREATE TABLE IF NOT EXISTS Items (id TEXT PRIMARY KEY, type TEXT, name TEXT, description TEXT, startingPrice REAL, artist TEXT, creationYear INTEGER, warrantyMonths INTEGER);";
        String sqlAuctions = "CREATE TABLE IF NOT EXISTS Auctions (id TEXT PRIMARY KEY, item_id TEXT, startTime TEXT, stopTime TEXT, currentHighestPrice REAL, status TEXT, FOREIGN KEY(item_id) REFERENCES Items(id));";
        
   
        String sqlBids = "CREATE TABLE IF NOT EXISTS Bids (id TEXT PRIMARY KEY, auction_id TEXT, username TEXT, bidAmount REAL, bidTime TEXT, FOREIGN KEY(auction_id) REFERENCES Auctions(id), FOREIGN KEY(username) REFERENCES Accounts(username));";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlAccounts);
            stmt.execute(sqlItems);
            stmt.execute(sqlAuctions);
            stmt.execute(sqlBids);
        } catch (SQLException e) { e.printStackTrace(); }
    }
    private void seedAccounts() {
        addAccount(new Account("U_ADMIN", "admin", "admin123", AccountRole.ADMIN));
        addAccount(new Account("U_GUEST", "guest", "guest123", AccountRole.GUEST));
    }

    private void seedInventory() {
        addAuction(new Auction("A01", new Art("I_A01", "Tranh phố cổ", "Tranh sơn dầu Hà Nội", 1200.0, "Bùi Xuân Phái", 1980), LocalDateTime.now().minusMinutes(10), LocalDateTime.now().plusHours(2), 1200.0, "RUNNING"));
        addAuction(new Auction("A02", new Art("I_A02", "Tượng Gỗ Lũa", "Tượng nghệ thuật điêu khắc", 500.0, "Nghệ nhân Việt", 2023), LocalDateTime.now().minusMinutes(5), LocalDateTime.now().plusHours(1), 500.0, "RUNNING"));
        addAuction(new Auction("E01", new Electronics("I_E01", "iPhone 15 Pro", "8/128", 1000.0, 12), LocalDateTime.now().minusMinutes(20), LocalDateTime.now().plusHours(3), 1000.0, "RUNNING"));
        addAuction(new Auction("E02", new Electronics("I_E02", "MacBook M3", "8/512", 2500.0, 24), LocalDateTime.now().minusMinutes(15), LocalDateTime.now().plusHours(3), 2500.0, "RUNNING"));
    }

    public synchronized boolean addAccount(Account account) {
        String sql = "INSERT INTO Accounts (username, password, role) VALUES (?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, account.getUsername().toLowerCase());
            pstmt.setString(2, account.getPassword());
            pstmt.setString(3, account.getRole().name());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    public synchronized boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM Accounts WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.toLowerCase());
            return pstmt.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    public synchronized List<Account> getAccounts() {
        List<Account> list = new ArrayList<>();
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM Accounts")) {
            while (rs.next()) list.add(new Account("", rs.getString("username"), rs.getString("password"), AccountRole.valueOf(rs.getString("role"))));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public synchronized boolean addItem(Item item) {
        String sql = "INSERT INTO Items (id, type, name, description, startingPrice, artist, creationYear, warrantyMonths) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.getId());
            pstmt.setString(3, item.getName());
            pstmt.setString(4, item.getDescription());
            pstmt.setDouble(5, item.getStartingPrice());
            if (item instanceof Art) {
                pstmt.setString(2, "ART"); pstmt.setString(6, ((Art) item).getArtist()); pstmt.setInt(7, ((Art) item).getCreationYear()); pstmt.setNull(8, Types.INTEGER);
            } else if (item instanceof Electronics) {
                pstmt.setString(2, "ELEC"); pstmt.setNull(6, Types.VARCHAR); pstmt.setNull(7, Types.INTEGER); pstmt.setInt(8, ((Electronics) item).getWarrantyMonths());
            }
            pstmt.executeUpdate(); return true;
        } catch (SQLException e) { return false; }
    }

    public synchronized Item findItemById(String itemId) {
        String sql = "SELECT * FROM Items WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String type = rs.getString("type");
                if ("ART".equals(type)) return new Art(rs.getString("id"), rs.getString("name"), rs.getString("description"), rs.getDouble("startingPrice"), rs.getString("artist"), rs.getInt("creationYear"));
                else return new Electronics(rs.getString("id"), rs.getString("name"), rs.getString("description"), rs.getDouble("startingPrice"), rs.getInt("warrantyMonths"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public synchronized boolean addAuction(Auction auction) {
        if (findItemById(auction.getItem().getId()) == null) addItem(auction.getItem());
        String sql = "INSERT INTO Auctions (id, item_id, startTime, stopTime, currentHighestPrice, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auction.getId()); pstmt.setString(2, auction.getItem().getId());
            pstmt.setString(3, auction.getStartTime().toString()); pstmt.setString(4, auction.getStopTime().toString());
            pstmt.setDouble(5, auction.getCurrentHighestPrice()); pstmt.setString(6, auction.getStatus());
            pstmt.executeUpdate(); return true;
        } catch (SQLException e) { return false; }
    }

    public synchronized List<Auction> getAuctions() {
        List<Auction> list = new ArrayList<>();
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM Auctions")) {
            while (rs.next()) {
                list.add(new Auction(rs.getString("id"), findItemById(rs.getString("item_id")), LocalDateTime.parse(rs.getString("startTime")), LocalDateTime.parse(rs.getString("stopTime")), rs.getDouble("currentHighestPrice"), rs.getString("status")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public synchronized boolean placeBid(String auctionId, String username, double amount) {
        String sql = "INSERT INTO Bids (id, auction_id, username, bidAmount, bidTime) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "BID_" + System.currentTimeMillis());
            pstmt.setString(2, auctionId);
            pstmt.setString(3, username);
            pstmt.setDouble(4, amount);
            pstmt.setString(5, LocalDateTime.now().toString());
            pstmt.executeUpdate();

            try (PreparedStatement updateStmt = conn.prepareStatement("UPDATE Auctions SET currentHighestPrice = ? WHERE id = ?")) {
                updateStmt.setDouble(1, amount); updateStmt.setString(2, auctionId); updateStmt.executeUpdate();
            }
            return true;
        } catch (SQLException e) { return false; }
    }

    public synchronized boolean updateAuctionStatus(String auctionId, String status) {
        String sql = "UPDATE Auctions SET status = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, auctionId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    public synchronized List<String> getBidHistory(String auctionId) {
        List<String> history = new ArrayList<>();
        String sql = "SELECT username, bidAmount, bidTime FROM Bids WHERE auction_id = ? ORDER BY bidAmount DESC";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                history.add(rs.getString("username") + " đã đặt " + rs.getDouble("bidAmount") + " USD lúc " + rs.getString("bidTime").replace("T", " "));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return history;
    }
    public synchronized Account authenticate(String username, String password) {
        String sql = "SELECT * FROM Accounts WHERE username = ? AND password = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Chuyển username về chữ thường để không phân biệt hoa/thường khi đăng nhập
            pstmt.setString(1, username.toLowerCase()); 
            pstmt.setString(2, password);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
              
                return new Account("", rs.getString("username"), rs.getString("password"), AccountRole.valueOf(rs.getString("role")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; 
    }
    public synchronized Auction findAuctionById(String auctionId) {
        String sql = "SELECT * FROM Auctions WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
         
                Item item = findItemById(rs.getString("item_id"));
                
                return new Auction(
                        rs.getString("id"),
                        item,
                        LocalDateTime.parse(rs.getString("startTime")),
                        LocalDateTime.parse(rs.getString("stopTime")),
                        rs.getDouble("currentHighestPrice"),
                        rs.getString("status")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; 
    }

   
    public void startAuctionMonitor() {
        Thread monitorThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000); 
                    
                    List<Auction> auctions = getAuctions(); 
                    for (Auction auction : auctions) {
            
                        if ("RUNNING".equalsIgnoreCase(auction.getStatus()) && 
                            LocalDateTime.now().isAfter(auction.getStopTime())) {
                            

                            updateAuctionStatus(auction.getId(), "FINISHED");
                            
                            System.out.println("=====================================");
                            System.out.println(">>> PHIÊN ĐẤU GIÁ KẾT THÚC: " + auction.getItem().getName());
                            System.out.println(">>> TRẠNG THÁI ĐÃ ĐƯỢC CHỐT SỔ THÀNH CÔNG");
                            app.network.Server.broadcast("AUCTION_CLOSED|" + auction.getId() + "|" + auction.getHighestBidderId() + "|" + auction.getCurrentHighestPrice());
                            System.out.println("=====================================\n");
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("Luồng giám sát bị gián đoạn.");
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        
        monitorThread.setDaemon(true); 
        monitorThread.start();
        System.out.println("[+] Đã khởi động luồng giám sát phiên đấu giá tự động (5s/lần).");
    }
}