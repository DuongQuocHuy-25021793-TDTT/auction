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
import app.model.Seller;
import app.model.User;
import app.model.Vehicle;
import app.model.SuspensionLog;

public class AppDatabase {
    private static final AppDatabase INSTANCE = new AppDatabase();
    private static final String DB_URL = "jdbc:sqlite:auction_app.db";

    private AppDatabase() {
        createTables();
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
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void createTables() {
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

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlAccounts);
            stmt.execute(sqlItems);
            stmt.execute(sqlAuctions);
            stmt.execute(sqlBidTransactions);
        } catch (SQLException e) {
            System.err.println("Lỗi tạo bảng: " + e.getMessage());
        }
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
        }
    }

    public synchronized boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM Accounts WHERE username = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, normalizeUsername(username));
            return pstmt.executeQuery().next();
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
        }
    }

    public synchronized Item findItemById(String itemId) {
        String sql = "SELECT * FROM Items WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, itemId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return extractItemFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

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
