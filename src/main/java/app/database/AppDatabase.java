package app.database;

import app.model.Account;
import app.model.AccountRole;
import app.model.Art;
import app.model.Auction;
import app.model.Electronics;
import app.model.Item;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppDatabase {
    private static final AppDatabase INSTANCE = new AppDatabase();

    private final Map<String, Account> accountsByUsername = new LinkedHashMap<>();
    private final Map<String, Item> itemsById = new LinkedHashMap<>();
    private final Map<String, Auction> auctionsById = new LinkedHashMap<>();

    private AppDatabase() {
        seedAccounts();
        seedInventory();
    }

    public static AppDatabase getInstance() {
        return INSTANCE;
    }

    private void seedAccounts() {
        addAccount(new Account("U_ADMIN", "admin", "admin123", AccountRole.ADMIN));
        addAccount(new Account("U_DEV", "dev", "dev123", AccountRole.DEV));
        addAccount(new Account("U_GUEST", "guest", "guest123", AccountRole.GUEST));
    }

    private void seedInventory() {
        addAuction(new Auction(
                "A01",
                new Art("I_A01", "Tranh phố cổ", "Tranh sơn dầu Hà Nội", 1200.0, "Nguyễn Xuân Phái", 1980),
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().plusHours(2),
                1200.0,
                "RUNNING"
        ));

        addAuction(new Auction(
                "A02",
                new Art("I_A02", "Tượng Gỗ Lũa", "Tượng nghệ thuật điêu khắc", 500.0, "Nghệ nhân Việt", 2023),
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(1),
                500.0,
                "RUNNING"
        ));

        addAuction(new Auction(
                "E01",
                new Electronics("I_E01", "iPhone 15 Pro", "8/128", 1000.0, 12),
                LocalDateTime.now().minusMinutes(20),
                LocalDateTime.now().plusHours(3),
                1000.0,
                "RUNNING"
        ));

        addAuction(new Auction(
                "E02",
                new Electronics("I_E02", "MacBook M4", "8/512", 2600.0, 24),
                LocalDateTime.now().minusMinutes(15),
                LocalDateTime.now().plusHours(3),
                2600.0,
                "RUNNING"
        ));
    }

    public synchronized boolean addAccount(Account account) {
        if (account == null || account.getUsername() == null) {
            return false;
        }

        String usernameKey = normalizeUsername(account.getUsername());
        if (usernameKey.isEmpty() || accountsByUsername.containsKey(usernameKey)) {
            return false;
        }

        accountsByUsername.put(usernameKey, account);
        return true;
    }

    public synchronized Account authenticate(String username, String password) {
        Account account = accountsByUsername.get(normalizeUsername(username));
        if (account == null || password == null || !password.equals(account.getPassword())) {
            return null;
        }
        return account;
    }

    public synchronized boolean usernameExists(String username) {
        return accountsByUsername.containsKey(normalizeUsername(username));
    }

    public synchronized List<Account> getAccounts() {
        return new ArrayList<>(accountsByUsername.values());
    }

    public synchronized boolean addItem(Item item) {
        if (item == null || item.getId() == null || item.getId().isBlank()) {
            return false;
        }

        if (itemsById.containsKey(item.getId())) {
            return false;
        }

        itemsById.put(item.getId(), item);
        return true;
    }

    public synchronized Item findItemById(String itemId) {
        return itemsById.get(itemId);
    }

    public synchronized List<Item> getItems() {
        return new ArrayList<>(itemsById.values());
    }

    public synchronized boolean addAuction(Auction auction) {
        if (auction == null || auction.getId() == null || auction.getId().isBlank()) {
            return false;
        }

        if (auctionsById.containsKey(auction.getId())) {
            return false;
        }

        auctionsById.put(auction.getId(), auction);
        addItemIfMissing(auction.getItem());
        return true;
    }

    public synchronized Auction findAuctionById(String auctionId) {
        return auctionsById.get(auctionId);
    }

    public synchronized List<Auction> getAuctions() {
        return new ArrayList<>(auctionsById.values());
    }

    public synchronized Auction createAuction(String auctionId, Item item, LocalDateTime startTime,
                                              LocalDateTime stopTime, double currentHighestPrice, String status) {
        Auction auction = new Auction(auctionId, item, startTime, stopTime, currentHighestPrice, status);
        if (!addAuction(auction)) {
            return null;
        }
        return auction;
    }

    private void addItemIfMissing(Item item) {
        if (item != null && item.getId() != null && !itemsById.containsKey(item.getId())) {
            itemsById.put(item.getId(), item);
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
