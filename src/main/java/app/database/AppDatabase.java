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

import app.model.AccountRole;
import app.model.Admin;
import app.model.Art;
import app.model.Auction;
import app.model.BidTransaction;
import app.model.Bidder;
import app.model.Electronics;
import app.model.Item;
<<<<<<< HEAD
import app.model.Message;
=======
import app.model.Seller;
import app.model.User;
import app.model.Vehicle;
import app.model.SuspensionLog;
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533

public class AppDatabase {
    private static final AppDatabase INSTANCE = new AppDatabase();
    private static final String DB_URL = "jdbc:sqlite:auction_app.db";

    private AppDatabase() {
        createTables();
<<<<<<< HEAD
        if (getAccounts().isEmpty()) seedAccounts();
        if (getAuctions().isEmpty()) seedInventory();
        
        startAuctionMonitor();
    }

    public static AppDatabase getInstance() { 
        return INSTANCE; 
=======
        migrateVehicleColumns();
        migrateElectronicsColumns();
        migrateAuctionsColumns();
        migrateLegacyAccountRoles();
        migrateAccountsColumns();
        createSuspensionLogsTable();
        cleanUpSuspensionLogs();

        ensureDefaultAccounts();
    }

    public static AppDatabase getInstance() {
        return INSTANCE;
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void createTables() {
<<<<<<< HEAD
        String sqlAccounts = "CREATE TABLE IF NOT EXISTS Accounts (username TEXT PRIMARY KEY, password TEXT, role TEXT);";
        String sqlItems = "CREATE TABLE IF NOT EXISTS Items (id TEXT PRIMARY KEY, type TEXT, name TEXT, description TEXT, startingPrice REAL, artist TEXT, creationYear INTEGER, warrantyMonths INTEGER);";
        String sqlAuctions = "CREATE TABLE IF NOT EXISTS Auctions (id TEXT PRIMARY KEY, item_id TEXT, startTime TEXT, stopTime TEXT, currentHighestPrice REAL, status TEXT, FOREIGN KEY(item_id) REFERENCES Items(id));";
        String sqlBids = "CREATE TABLE IF NOT EXISTS Bids (id TEXT PRIMARY KEY, auction_id TEXT, username TEXT, bidAmount REAL, bidTime TEXT, FOREIGN KEY(auction_id) REFERENCES Auctions(id), FOREIGN KEY(username) REFERENCES Accounts(username));";
=======
        String sqlAccounts = "CREATE TABLE IF NOT EXISTS Accounts (" +
                "username TEXT PRIMARY KEY, password TEXT, role TEXT);";

        String sqlItems = "CREATE TABLE IF NOT EXISTS Items (" +
                "id TEXT PRIMARY KEY, type TEXT, name TEXT, description TEXT, " +
                "startingPrice REAL, artist TEXT, creationYear INTEGER, warrantyMonths INTEGER, " +
                "brand TEXT, mileage INTEGER, condition TEXT, purchaseDate TEXT, isRepaired TEXT, repairDate TEXT, repairedParts TEXT);";

        String sqlAuctions = "CREATE TABLE IF NOT EXISTS Auctions (" +
                "id TEXT PRIMARY KEY, item_id TEXT, startTime TEXT, stopTime TEXT, " +
                "currentHighestPrice REAL, status TEXT, highestBidderId TEXT, FOREIGN KEY(item_id) REFERENCES Items(id));";

        String sqlBidTransactions = "CREATE TABLE IF NOT EXISTS BidTransactions (" +
                "id TEXT PRIMARY KEY, auction_id TEXT, bidder_id TEXT, bidAmount REAL, timestamp TEXT, " +
                "FOREIGN KEY(auction_id) REFERENCES Auctions(id));";
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlAccounts);
            stmt.execute(sqlItems);
            stmt.execute(sqlAuctions);
<<<<<<< HEAD
            stmt.execute(sqlBids);
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
    }

    private void seedAccounts() {
        addAccount(new Account("U_ADMIN", "admin", "admin123", AccountRole.ADMIN));
        addAccount(new Account("U_GUEST", "guest", "guest123", AccountRole.GUEST));
=======
            stmt.execute(sqlBidTransactions);
        } catch (SQLException e) {
            System.err.println("Lỗi tạo bảng: " + e.getMessage());
        }
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533
    }

    private void migrateAuctionsColumns() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            if (!columnExists(conn, "Auctions", "highestBidderId")) {
                stmt.executeUpdate("ALTER TABLE Auctions ADD COLUMN highestBidderId TEXT");
            }
            if (!columnExists(conn, "Auctions", "sellerId")) {
                stmt.executeUpdate("ALTER TABLE Auctions ADD COLUMN sellerId TEXT");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi migrate cột Auctions: " + e.getMessage());
        }
    }

    private void migrateElectronicsColumns() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            if (!columnExists(conn, "Items", "condition")) {
                stmt.executeUpdate("ALTER TABLE Items ADD COLUMN condition TEXT");
            }
            if (!columnExists(conn, "Items", "purchaseDate")) {
                stmt.executeUpdate("ALTER TABLE Items ADD COLUMN purchaseDate TEXT");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi migrate cột cho Electronics: " + e.getMessage());
        }
    }

    private void migrateLegacyAccountRoles() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE Accounts SET role = 'SELLER' WHERE role = 'DEV'");
            stmt.executeUpdate("UPDATE Accounts SET role = 'BIDDER' WHERE role = 'GUEST'");
            stmt.executeUpdate("UPDATE Accounts SET role = 'BIDDER' WHERE role IS NULL OR role = ''");
        } catch (SQLException e) {
            System.err.println("Lỗi migrate role tài khoản: " + e.getMessage());
        }
    }

    private void migrateAccountsColumns() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            if (!columnExists(conn, "Accounts", "fullName")) stmt.executeUpdate("ALTER TABLE Accounts ADD COLUMN fullName TEXT");
            if (!columnExists(conn, "Accounts", "status")) stmt.executeUpdate("ALTER TABLE Accounts ADD COLUMN status TEXT DEFAULT 'ACTIVE'");
            if (!columnExists(conn, "Accounts", "suspensionCount")) stmt.executeUpdate("ALTER TABLE Accounts ADD COLUMN suspensionCount INTEGER DEFAULT 0");
            if (!columnExists(conn, "Accounts", "suspendedUntil")) stmt.executeUpdate("ALTER TABLE Accounts ADD COLUMN suspendedUntil INTEGER DEFAULT 0");
            if (!columnExists(conn, "Accounts", "lastSuspensionTime")) stmt.executeUpdate("ALTER TABLE Accounts ADD COLUMN lastSuspensionTime INTEGER DEFAULT 0");
        } catch (SQLException e) {
            System.err.println("Lỗi migrate cột Accounts: " + e.getMessage());
        }
    }

    private void createSuspensionLogsTable() {
        String sql = "CREATE TABLE IF NOT EXISTS SuspensionLogs (" +
                "id TEXT PRIMARY KEY, userId TEXT, suspensionLevel INTEGER, timestamp TEXT, status TEXT);";
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Lỗi tạo bảng SuspensionLogs: " + e.getMessage());
        }
    }

    private void cleanUpSuspensionLogs() {
        // Log sẽ tự động được quét và xóa dựa trên rule 
        String sqlSelect = "SELECT * FROM SuspensionLogs";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sqlSelect)) {
            while (rs.next()) {
                String logId = rs.getString("id");
                String userId = rs.getString("userId");
                int level = rs.getInt("suspensionLevel");
                LocalDateTime timestamp = LocalDateTime.parse(rs.getString("timestamp"));
                String status = rs.getString("status");
                
                long currentMillis = System.currentTimeMillis();
                long logMillis = timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                long diffMillis = currentMillis - logMillis;
                
                long oneYear = 365L * 24 * 60 * 60 * 1000;
                long threeMonths = 90L * 24 * 60 * 60 * 1000;
                long oneWeek = 7L * 24 * 60 * 60 * 1000;

                boolean shouldUpdate = false;
                boolean shouldDelete = false;
                String newStatus = status;

                if (level == 3) {
                    // Vĩnh viễn: đổi trạng thái thành Đã xóa vĩnh viễn, sau 1 tuần thì xóa hẳn
                    if (!"Đã xóa vĩnh viễn".equals(status)) {
                        newStatus = "Đã xóa vĩnh viễn";
                        shouldUpdate = true;
                    }
                    if (diffMillis > oneWeek) {
                        shouldDelete = true;
                    }
                } else {
                    // Lần 1 & 2
                    // Nếu > 1 năm và chưa vi phạm thêm, trạng thái thành Đã được khôi phục
                    // Kiểm tra xem user có vi phạm thêm không: 
                    User u = findUserByUsername(userId.startsWith("U_") ? userId.substring(2) : userId); // userId là id
                    boolean noFurtherViolation = false;
                    if (u != null) {
                        // Nếu số lần vi phạm của user == level (tức là không tăng thêm) hoặc đã được reset
                        if (u.getSuspensionCount() <= level) {
                            noFurtherViolation = true;
                        }
                    }
                    
                    if (diffMillis > oneYear && noFurtherViolation) {
                        if (!"Đã được khôi phục".equals(status)) {
                            newStatus = "Đã được khôi phục";
                            shouldUpdate = true;
                        }
                    }
                    
                    // Xóa nếu quá 1 năm + 3 tháng (chỉ khi đã khôi phục)
                    if ("Đã được khôi phục".equals(newStatus) && diffMillis > (oneYear + threeMonths)) {
                        shouldDelete = true;
                    }
                }
                
                if (shouldDelete) {
                    try (PreparedStatement delStmt = conn.prepareStatement("DELETE FROM SuspensionLogs WHERE id = ?")) {
                        delStmt.setString(1, logId);
                        delStmt.executeUpdate();
                    }
                } else if (shouldUpdate) {
                    try (PreparedStatement updStmt = conn.prepareStatement("UPDATE SuspensionLogs SET status = ? WHERE id = ?")) {
                        updStmt.setString(1, newStatus);
                        updStmt.setString(2, logId);
                        updStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi clean up SuspensionLogs: " + e.getMessage());
        }
    }

    private void migrateVehicleColumns() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            if (!columnExists(conn, "Items", "brand")) {
                stmt.executeUpdate("ALTER TABLE Items ADD COLUMN brand TEXT");
            }
            if (!columnExists(conn, "Items", "mileage")) {
                stmt.executeUpdate("ALTER TABLE Items ADD COLUMN mileage INTEGER");
            }
            if (!columnExists(conn, "Items", "isRepaired")) {
                stmt.executeUpdate("ALTER TABLE Items ADD COLUMN isRepaired TEXT");
            }
            if (!columnExists(conn, "Items", "repairDate")) {
                stmt.executeUpdate("ALTER TABLE Items ADD COLUMN repairDate TEXT");
            }
            if (!columnExists(conn, "Items", "repairedParts")) {
                stmt.executeUpdate("ALTER TABLE Items ADD COLUMN repairedParts TEXT");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi migrate cột Vehicle: " + e.getMessage());
        }
    }

    private boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    private void ensureDefaultAccounts() {
        ensureDefaultUser(new Admin("U_admin", "admin", "admin123"));
        ensureDefaultUser(new Seller("U_seller", "seller", "seller123"));
        ensureDefaultUser(new Bidder("U_bidder", "bidder", "bidder123"));
    }

    private void ensureDefaultUser(User user) {
        if (!usernameExists(user.getUsername())) {
            addUser(user);
        }
    }

    public synchronized boolean saveBidTransaction(BidTransaction bid) {
        String sql = "INSERT INTO BidTransactions (id, auction_id, bidder_id, bidAmount, timestamp) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, bid.getId());
            pstmt.setString(2, bid.getAuctionId());
            pstmt.setString(3, bid.getBidderId());
            pstmt.setDouble(4, bid.getBidAmount());
            pstmt.setString(5, bid.getTimestamp().toString());
            pstmt.executeUpdate();
            return true;
<<<<<<< HEAD
        } catch (SQLException e) { 
            return false; 
=======
        } catch (SQLException e) {
            System.err.println("Lỗi lưu BidTransaction: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean updateAuctionPrice(String auctionId, double price, String highestBidderId) {
        String sql = "UPDATE Auctions SET currentHighestPrice = ?, highestBidderId = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, price);
            pstmt.setString(2, highestBidderId);
            pstmt.setString(3, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi cập nhật giá Auction: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean updateAuctionStopTime(String auctionId, String newStopTime) {
        String sql = "UPDATE Auctions SET stopTime = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStopTime);
            pstmt.setString(2, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi cập nhật stopTime Auction: " + e.getMessage());
            return false;
        }
    }

    public synchronized List<BidTransaction> getBidHistory(String auctionId) {
        List<BidTransaction> history = new ArrayList<>();
        String sql = "SELECT * FROM BidTransactions WHERE auction_id = ? ORDER BY timestamp ASC";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                history.add(new BidTransaction(
                        rs.getString("id"),
                        rs.getString("auction_id"),
                        rs.getString("bidder_id"),
                        rs.getDouble("bidAmount"),
                        LocalDateTime.parse(rs.getString("timestamp"))));
            }
        } catch (SQLException e) {
            System.err.println("Lỗi lấy lịch sử đấu giá: " + e.getMessage());
        }
        return history;
    }

    public synchronized boolean addUser(User user) {
        if (user == null || user.getUsername() == null) {
            return false;
        }
        if (!user.getRole().canSelfRegister() && user.getRole() != AccountRole.ADMIN) {
            return false;
        }

        String sql = "INSERT INTO Accounts (username, password, role, fullName, status, suspensionCount, suspendedUntil, lastSuspensionTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, normalizeUsername(user.getUsername()));
            pstmt.setString(2, app.utils.PasswordUtil.hashPassword(user.getPassword()));
            pstmt.setString(3, user.getRole().name());
            pstmt.setString(4, user.getFullName());
            pstmt.setString(5, user.getStatus() != null ? user.getStatus() : "ACTIVE");
            pstmt.setInt(6, user.getSuspensionCount());
            pstmt.setLong(7, user.getSuspendedUntil());
            pstmt.setLong(8, user.getLastSuspensionTime());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public synchronized User authenticate(String username, String password) {
        String sql = "SELECT * FROM Accounts WHERE username = ?";
        User authenticatedUser = null;
        boolean needsMigration = false;

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, normalizeUsername(username));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    // Check if the stored password is plain text (legacy)
                    if (storedPassword != null && storedPassword.length() < 64) {
                        if (storedPassword.equals(password)) {
                            authenticatedUser = createUserFromResultSet(rs);
                            needsMigration = true;
                        }
                    } else {
                        if (app.utils.PasswordUtil.checkPassword(password, storedPassword)) {
                            authenticatedUser = createUserFromResultSet(rs);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (needsMigration) {
            migratePasswordToHash(username, password);
        }

        return authenticatedUser;
    }

    private void migratePasswordToHash(String username, String plainPassword) {
        String sql = "UPDATE Accounts SET password = ? WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, app.utils.PasswordUtil.hashPassword(plainPassword));
            pstmt.setString(2, normalizeUsername(username));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lỗi migrate mật khẩu: " + e.getMessage());
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533
        }
    }

    public synchronized boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM Accounts WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, normalizeUsername(username));
            return pstmt.executeQuery().next();
<<<<<<< HEAD
        } catch (SQLException e) { 
            return false; 
        }
    }

    public synchronized List<Account> getAccounts() {
        List<Account> list = new ArrayList<>();
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM Accounts")) {
            while (rs.next()) {
                list.add(new Account(rs.getString("username"), rs.getString("username"), rs.getString("password"), AccountRole.valueOf(rs.getString("role"))));
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        return list;
=======
        } catch (SQLException e) {
            return false;
        }
    }

    public synchronized User findUserByUsername(String username) {
        String sql = "SELECT * FROM Accounts WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, normalizeUsername(username));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return createUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized boolean updateUserStatus(User user) {
        String sql = "UPDATE Accounts SET status = ?, suspensionCount = ?, suspendedUntil = ?, lastSuspensionTime = ? WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getStatus());
            pstmt.setInt(2, user.getSuspensionCount());
            pstmt.setLong(3, user.getSuspendedUntil());
            pstmt.setLong(4, user.getLastSuspensionTime());
            pstmt.setString(5, normalizeUsername(user.getUsername()));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi cập nhật trạng thái User: " + e.getMessage());
            return false;
        }
    }

    public synchronized List<User> getUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM Accounts";
        try (Connection conn = connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                users.add(createUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    private User createUserFromResultSet(ResultSet rs) throws SQLException {
        String username = rs.getString("username");
        String password = rs.getString("password");
        AccountRole role = parseRole(rs.getString("role"));
        String id = "U_" + username;

        User user;
        switch (role) {
            case ADMIN:
                user = new Admin(id, username, password);
                break;
            case SELLER:
                user = new Seller(id, username, password);
                break;
            case BIDDER:
            default:
                user = new Bidder(id, username, password);
                break;
        }

        user.setFullName(rs.getString("fullName"));
        user.setStatus(rs.getString("status") != null ? rs.getString("status") : "ACTIVE");
        user.setSuspensionCount(rs.getInt("suspensionCount"));
        user.setSuspendedUntil(rs.getLong("suspendedUntil"));
        user.setLastSuspensionTime(rs.getLong("lastSuspensionTime"));

        return user;
    }

    private AccountRole parseRole(String role) {
        if ("DEV".equalsIgnoreCase(role)) {
            return AccountRole.SELLER;
        }
        if ("GUEST".equalsIgnoreCase(role)) {
            return AccountRole.BIDDER;
        }
        try {
            return AccountRole.valueOf(role);
        } catch (Exception e) {
            return AccountRole.BIDDER;
        }
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533
    }

    public synchronized boolean addItem(Item item) {
        if (item == null || item.getId() == null) {
            return false;
        }
        String sql = "INSERT INTO Items (id, type, name, description, startingPrice, artist, creationYear, warrantyMonths, brand, mileage, condition, purchaseDate, isRepaired, repairDate, repairedParts) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, item.getId());
            pstmt.setString(3, item.getName());
            pstmt.setString(4, item.getDescription());
            pstmt.setDouble(5, item.getStartingPrice());

            if (item instanceof Art) {
<<<<<<< HEAD
                pstmt.setString(2, "ART"); 
                pstmt.setString(6, ((Art) item).getArtist()); 
                pstmt.setInt(7, ((Art) item).getCreationYear()); 
                pstmt.setNull(8, Types.INTEGER);
            } else if (item instanceof Electronics) {
                pstmt.setString(2, "ELEC"); 
                pstmt.setNull(6, Types.VARCHAR); 
                pstmt.setNull(7, Types.INTEGER); 
                pstmt.setInt(8, ((Electronics) item).getWarrantyMonths());
            }
            pstmt.executeUpdate(); 
            return true;
        } catch (SQLException e) { 
            return false; 
=======
                Art art = (Art) item;
                pstmt.setString(2, "ART");
                pstmt.setString(6, art.getArtist());
                pstmt.setInt(7, art.getCreationYear());
                pstmt.setNull(8, Types.INTEGER);
                pstmt.setNull(9, Types.VARCHAR);
                pstmt.setNull(10, Types.INTEGER);
                pstmt.setNull(11, Types.VARCHAR);
                pstmt.setNull(12, Types.VARCHAR);
                pstmt.setNull(13, Types.VARCHAR);
                pstmt.setNull(14, Types.VARCHAR);
                pstmt.setNull(15, Types.VARCHAR);
            } else if (item instanceof Electronics) {
                Electronics elec = (Electronics) item;
                pstmt.setString(2, "ELEC");
                pstmt.setNull(6, Types.VARCHAR);
                pstmt.setNull(7, Types.INTEGER);
                pstmt.setInt(8, elec.getWarrantyMonths());
                pstmt.setNull(9, Types.VARCHAR);
                pstmt.setNull(10, Types.INTEGER);
                pstmt.setString(11, elec.getCondition());
                pstmt.setString(12, elec.getPurchaseDate());
                pstmt.setString(13, elec.getIsRepaired());
                pstmt.setString(14, elec.getRepairDate());
                pstmt.setString(15, elec.getRepairedParts());
            } else if (item instanceof Vehicle) {
                Vehicle vehicle = (Vehicle) item;
                pstmt.setString(2, "VEHICLE");
                pstmt.setNull(6, Types.VARCHAR);
                pstmt.setNull(7, Types.INTEGER);
                pstmt.setNull(8, Types.INTEGER);
                pstmt.setString(9, vehicle.getBrand());
                pstmt.setInt(10, vehicle.getMileage());
                pstmt.setString(11, vehicle.getCondition());
                pstmt.setString(12, vehicle.getPurchaseDate());
                pstmt.setString(13, vehicle.getIsRepaired());
                pstmt.setString(14, vehicle.getRepairDate());
                pstmt.setString(15, vehicle.getRepairedParts());
            } else {
                return false;
            }
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533
        }
    }

    public synchronized Item findItemById(String itemId) {
        String sql = "SELECT * FROM Items WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
<<<<<<< HEAD
                String type = rs.getString("type");
                if ("ART".equals(type)) {
                    return new Art(rs.getString("id"), rs.getString("name"), rs.getString("description"), rs.getDouble("startingPrice"), rs.getString("artist"), rs.getInt("creationYear"));
                } else {
                    return new Electronics(rs.getString("id"), rs.getString("name"), rs.getString("description"), rs.getDouble("startingPrice"), rs.getInt("warrantyMonths"));
                }
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        return null;
    }

    public synchronized boolean addAuction(Auction auction) {
        if (findItemById(auction.getItem().getId()) == null) {
            addItem(auction.getItem());
        }
        String sql = "INSERT INTO Auctions (id, item_id, startTime, stopTime, currentHighestPrice, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auction.getId()); 
            pstmt.setString(2, auction.getItem().getId());
            pstmt.setString(3, auction.getStartTime().toString()); 
            pstmt.setString(4, auction.getStopTime().toString());
            pstmt.setDouble(5, auction.getCurrentHighestPrice()); 
            pstmt.setString(6, auction.getStatus());
            pstmt.executeUpdate(); 
            return true;
        } catch (SQLException e) { 
            return false; 
        }
    }

    /**
     * ĐÃ TỐI ƯU HÓA: Dùng INNER JOIN để lấy cả thông tin đấu giá và sản phẩm cùng một lúc,
     * loại bỏ hoàn toàn việc tạo Connection lặp đi lặp lại lỗi bộ nhớ.
     */
    public synchronized List<Auction> getAuctions() {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT a.id AS auc_id, a.startTime, a.stopTime, a.currentHighestPrice, a.status, " +
                     "i.id AS item_id, i.type, i.name, i.description, i.startingPrice, i.artist, i.creationYear, i.warrantyMonths " +
                     "FROM Auctions a INNER JOIN Items i ON a.item_id = i.id";
                     
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String type = rs.getString("type");
                Item item;
                if ("ART".equals(type)) {
                    item = new Art(rs.getString("item_id"), rs.getString("name"), rs.getString("description"), rs.getDouble("startingPrice"), rs.getString("artist"), rs.getInt("creationYear"));
                } else {
                    item = new Electronics(rs.getString("item_id"), rs.getString("name"), rs.getString("description"), rs.getDouble("startingPrice"), rs.getInt("warrantyMonths"));
                }
                
                list.add(new Auction(
                    rs.getString("auc_id"), 
                    item, 
                    LocalDateTime.parse(rs.getString("startTime")), 
                    LocalDateTime.parse(rs.getString("stopTime")), 
                    rs.getDouble("currentHighestPrice"), 
                    rs.getString("status")
                ));
            }
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        return list;
    }

    /**
     * ĐÃ TỐI ƯU HÓA: Tìm kiếm phiên đấu giá đi kèm sản phẩm bằng cú pháp JOIN siêu tốc.
     */
    public synchronized Auction findAuctionById(String auctionId) {
        String sql = "SELECT a.id AS auc_id, a.startTime, a.stopTime, a.currentHighestPrice, a.status, " +
                     "i.id AS item_id, i.type, i.name, i.description, i.startingPrice, i.artist, i.creationYear, i.warrantyMonths " +
                     "FROM Auctions a INNER JOIN Items i ON a.item_id = i.id WHERE a.id = ?";
                     
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String type = rs.getString("type");
                Item item;
                if ("ART".equals(type)) {
                    item = new Art(rs.getString("item_id"), rs.getString("name"), rs.getString("description"), rs.getDouble("startingPrice"), rs.getString("artist"), rs.getInt("creationYear"));
                } else {
                    item = new Electronics(rs.getString("item_id"), rs.getString("name"), rs.getString("description"), rs.getDouble("startingPrice"), rs.getInt("warrantyMonths"));
                }
                
                return new Auction(
                    rs.getString("auc_id"),
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
                updateStmt.setDouble(1, amount); 
                updateStmt.setString(2, auctionId); 
                updateStmt.executeUpdate();
            }
            return true;
        } catch (SQLException e) { 
            return false; 
        }
    }

    public synchronized boolean updateAuctionStatus(String auctionId, String status) {
        String sql = "UPDATE Auctions SET status = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, auctionId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) { 
            return false; 
        }
    }

    public synchronized boolean updateAuctionStopTime(String auctionId, LocalDateTime newStopTime) {
        String sql = "UPDATE Auctions SET stopTime = ? WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStopTime.toString());
            pstmt.setString(2, auctionId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) { 
            return false; 
        }
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
        } catch (SQLException e) { 
            e.printStackTrace(); 
        }
        return history;
    }

    public synchronized Account authenticate(String username, String password) {
        String sql = "SELECT * FROM Accounts WHERE username = ? AND password = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username.toLowerCase()); 
            pstmt.setString(2, password);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Account(rs.getString("username"), rs.getString("username"), rs.getString("password"), AccountRole.valueOf(rs.getString("role")));
=======
                return extractItemFromResultSet(rs);
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

<<<<<<< HEAD
    public void startAuctionMonitor() {
        Thread monitorThread = new Thread(() -> {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            while (true) {
                try {
                    Thread.sleep(5000); 
                    
                    List<Auction> auctions = getAuctions(); 
                    for (Auction auction : auctions) {
            
                        if ("RUNNING".equalsIgnoreCase(auction.getStatus()) && 
                            LocalDateTime.now().isAfter(auction.getStopTime())) {
                            
                            updateAuctionStatus(auction.getId(), "FINISHED");
                            
                            List<String> bidHistory = getBidHistory(auction.getId());
                            String winner = "Không có";
                            if (bidHistory != null && !bidHistory.isEmpty()) {
                                String topBid = bidHistory.get(0); 
                                winner = topBid.split(" ")[0]; 
                            }
                            
                            System.out.println("=====================================");
                            System.out.println(">>> PHIÊN ĐẤU GIÁ KẾT THÚC TỰ ĐỘNG: " + auction.getItem().getName());
                            System.out.println(">>> TRẠNG THÁI ĐÃ ĐƯỢC CHỐT SỔ THÀNH CÔNG");
                            System.out.println(">>> Người thắng: " + winner + " | Giá chốt: " + auction.getCurrentHighestPrice() + " USD");
                            System.out.println("=====================================\n");
                            
                            String broadcastData = auction.getId() + "|" + winner + "|" + auction.getCurrentHighestPrice();
                            Message jsonMessage = new Message("AUCTION_CLOSED", broadcastData);
                            
                            // Gọi cơ chế phát sóng tới các client
                            app.network.Server.broadcast(gson.toJson(jsonMessage));
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
=======
    public synchronized List<Item> getItems() {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM Items";
        try (Connection conn = connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                items.add(extractItemFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    private Item extractItemFromResultSet(ResultSet rs) throws SQLException {
        String type = rs.getString("type");
        String id = rs.getString("id");
        String name = rs.getString("name");
        String desc = rs.getString("description");
        double price = rs.getDouble("startingPrice");

        if ("ART".equals(type)) {
            return new Art(id, name, desc, price, rs.getString("artist"), rs.getInt("creationYear"));
        } else if ("ELEC".equals(type)) {
            return new Electronics(id, name, desc, price, rs.getInt("warrantyMonths"), rs.getString("condition"), rs.getString("purchaseDate"), rs.getString("isRepaired"), rs.getString("repairDate"), rs.getString("repairedParts"));
        } else if ("VEHICLE".equals(type)) {
            return new Vehicle(id, name, desc, price, rs.getString("brand"), rs.getInt("mileage"), rs.getString("condition"), rs.getString("purchaseDate"), rs.getString("isRepaired"), rs.getString("repairDate"), rs.getString("repairedParts"));
        }
        return null;
    }

    public synchronized boolean addAuction(Auction auction) {
        if (auction == null || auction.getId() == null) {
            return false;
        }

        if (findItemById(auction.getItem().getId()) == null) {
            addItem(auction.getItem());
        }

        String sql = "INSERT INTO Auctions (id, item_id, startTime, stopTime, currentHighestPrice, status, sellerId) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auction.getId());
            pstmt.setString(2, auction.getItem().getId());
            pstmt.setString(3, auction.getStartTime().toString());
            pstmt.setString(4, auction.getStopTime().toString());
            pstmt.setDouble(5, auction.getCurrentHighestPrice());
            pstmt.setString(6, auction.getStatus());
            pstmt.setString(7, auction.getSellerId());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public synchronized boolean stopAuction(String auctionId) {
        String sql = "UPDATE Auctions SET status = 'STOPPED' WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public synchronized boolean deleteAuction(String auctionId) {
        String sql = "DELETE FROM Auctions WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public synchronized Auction findAuctionById(String auctionId) {
        String sql = "SELECT * FROM Auctions WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, auctionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Item item = findItemById(rs.getString("item_id"));
                Auction auction = new Auction(
                        rs.getString("id"),
                        item,
                        LocalDateTime.parse(rs.getString("startTime")),
                        LocalDateTime.parse(rs.getString("stopTime")),
                        rs.getDouble("currentHighestPrice"),
                        rs.getString("status"));
                auction.setHighestBidderId(rs.getString("highestBidderId"));
                auction.setSellerId(rs.getString("sellerId"));
                auction.getBidHistory().addAll(getBidHistory(auction.getId()));
                return auction;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533
    }

    public synchronized List<Auction> getAuctions() {
        List<Auction> auctions = new ArrayList<>();
        String sql = "SELECT * FROM Auctions";
        try (Connection conn = connect();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Item item = findItemById(rs.getString("item_id"));
                Auction auction = new Auction(
                        rs.getString("id"),
                        item,
                        LocalDateTime.parse(rs.getString("startTime")),
                        LocalDateTime.parse(rs.getString("stopTime")),
                        rs.getDouble("currentHighestPrice"),
                        rs.getString("status"));
                auction.setHighestBidderId(rs.getString("highestBidderId"));
                auction.setSellerId(rs.getString("sellerId"));
                auction.getBidHistory().addAll(getBidHistory(auction.getId()));
                auctions.add(auction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return auctions;
    }

    public synchronized Auction createAuction(String auctionId, Item item, LocalDateTime startTime,
            LocalDateTime stopTime, double currentHighestPrice, String status) {
        Auction auction = new Auction(auctionId, item, startTime, stopTime, currentHighestPrice, status);
        if (!addAuction(auction)) {
            return null;
        }
        return auction;
    }

    public synchronized List<SuspensionLog> getSuspensionHistory(int filterLevel, String userIdQuery) {
        List<SuspensionLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM SuspensionLogs";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int level = rs.getInt("suspensionLevel");
                if (filterLevel > 0 && level != filterLevel) continue;
                
                String uId = rs.getString("userId");
                if (userIdQuery != null && !userIdQuery.isEmpty()) {
                    // userIdQuery có thể là tên đăng nhập, ta cần kiểm tra
                    User u = findUserById(uId);
                    if (u == null || !u.getUsername().toLowerCase().contains(userIdQuery.toLowerCase())) {
                        continue;
                    }
                }
                
                SuspensionLog log = new SuspensionLog(
                    rs.getString("id"), uId, level, 
                    LocalDateTime.parse(rs.getString("timestamp")), 
                    rs.getString("status")
                );
                logs.add(log);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public synchronized void addSuspensionLog(SuspensionLog log) {
        String sql = "INSERT INTO SuspensionLogs (id, userId, suspensionLevel, timestamp, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, log.getId());
            pstmt.setString(2, log.getUserId());
            pstmt.setInt(3, log.getSuspensionLevel());
            pstmt.setString(4, log.getTimestamp().toString());
            pstmt.setString(5, log.getStatus());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lỗi addSuspensionLog: " + e.getMessage());
        }
    }

    public synchronized List<Auction> getBidderHistory(String bidderId) {
        List<Auction> history = new ArrayList<>();
        List<Auction> allAuctions = getAuctions();
        for (Auction a : allAuctions) {
            boolean hasBid = a.getBidHistory().stream().anyMatch(b -> b.getBidderId().equals(bidderId));
            if (hasBid) {
                history.add(a);
            }
        }
        return history;
    }

    public synchronized List<Auction> getSellerHistory(String sellerId) {
        List<Auction> history = new ArrayList<>();
        List<Auction> allAuctions = getAuctions();
        for (Auction a : allAuctions) {
            if (sellerId.equals(a.getSellerId())) {
                history.add(a);
            }
        }
        return history;
    }

    public synchronized User findUserById(String id) {
        String sql = "SELECT * FROM Accounts WHERE username = (SELECT username FROM Accounts WHERE username LIKE ? OR username = ? LIMIT 1)";
        // Thực ra username trong Accounts đang được dùng làm Primary Key. 
        // Trong hệ thống hiện tại, getUsername() == id (ví dụ: u.getUsername()). 
        // Nhưng khi tạo user, id được set là "U_" + UUID, và username là email.
        // Khoan, bảng Accounts có cột username PRIMARY KEY. Không có cột id.
        // Vậy id chính là username.
        return findUserByUsername(id);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
