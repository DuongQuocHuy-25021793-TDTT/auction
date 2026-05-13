package app.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    private Item item;
    private LocalDateTime startTime;
    private LocalDateTime stopTime;
    private double currentHighestPrice;
    private String status;
    private String highestBidderId;
    private List<BidTransaction> bidHistory;

    public Auction(String id, Item item, LocalDateTime startTime, LocalDateTime stopTime,
                   double currentHighestPrice, String status) {
        super(id);
        this.item = item;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.currentHighestPrice = currentHighestPrice;
        this.status = status;
        this.bidHistory = new ArrayList<>();
    }

    public Item getItem() {
        return item;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getStopTime() {
        return stopTime;
    }

    public long getElapseTime() { // Kiểm tra thời gian đã qua của phiên đấu giá
        if (LocalDateTime.now().isBefore(startTime)) {
            return 0;
        }

        // Nếu phiên đã kết thúc thì tính từ stopTime đến hiện tại
        LocalDateTime endTime = LocalDateTime.now().isAfter(stopTime) ? stopTime : LocalDateTime.now();
        return Duration.between(startTime, endTime).getSeconds();
    }

    // Kiểm tra thời gian còn lại của phiên đấu giá
    public long getRemainingTime() {
        if (LocalDateTime.now().isAfter(stopTime)) {
            return 0;
        }
        return Duration.between(LocalDateTime.now(), stopTime).getSeconds();
    }

    public double getCurrentHighestPrice() {
        return currentHighestPrice;
    }

    public String getStatus() {
        return status;
    }

    public String getHighestBidderId() {
        return highestBidderId;
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setStopTime(LocalDateTime stopTime) {
        this.stopTime = stopTime;
    }

    public void setCurrentHighestPrice(double currentHighestPrice) {
        this.currentHighestPrice = currentHighestPrice;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String validateBid(BidTransaction bid) {
        if (bid == null || bid.getBidderId() == null || bid.getBidderId().isEmpty()) {
            return "INVALID_BIDDER";
        }

        if (bid.getBidAmount() <= 0) {
            return "INVALID_AMOUNT";
        }

        if (LocalDateTime.now().isBefore(startTime)) {
            return "NOT_STARTED";
        }

        if (status == null || !status.equalsIgnoreCase("RUNNING")) {
            return "NOT_RUNNING";
        }

        if (LocalDateTime.now().isAfter(stopTime)) {
            status = "FINISHED";
            return "FINISHED";
        }

        if (bid.getBidAmount() <= currentHighestPrice) {
            return "TOO_LOW";
        }

        return null;
    }

    public boolean placeBid(BidTransaction bid) {
        String validation = validateBid(bid);
        if (validation != null) {
            System.out.println("Đặt giá bị từ chối (" + validation + ")");
            return false;
        }

        currentHighestPrice = bid.getBidAmount();
        highestBidderId = bid.getBidderId();
        bidHistory.add(bid);
        return true;
    }
}