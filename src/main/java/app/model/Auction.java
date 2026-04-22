package app.model;

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

    public boolean placeBid(BidTransaction bid) {
        if (!status.equalsIgnoreCase("RUNNING")) {
            System.out.println("Phiên đấu giá không hoạt động!");
            return false;
        }

        if (LocalDateTime.now().isAfter(stopTime)) {
            status = "FINISHED";
            System.out.println("Phiên đấu giá đã hết thời gian!");
            return false;
        }

        if (bid.getBidAmount() <= currentHighestPrice) {
            System.out.println("Giá bid phải cao hơn giá hiện tại!");
            return false;
        }

        currentHighestPrice = bid.getBidAmount();
        highestBidderId = bid.getBidderId();
        bidHistory.add(bid);
        return true;
    }
}