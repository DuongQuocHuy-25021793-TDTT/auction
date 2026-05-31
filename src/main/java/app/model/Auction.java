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
    private String sellerId;
    private List<BidTransaction> bidHistory;


    public static final int DURATION_MINUTES = 30;

 
    public Auction(String id, Item item, double startingPrice) {
        super(id);
        this.item = item;
        this.startTime = LocalDateTime.now();
        this.stopTime = this.startTime.plusMinutes(DURATION_MINUTES);
        this.currentHighestPrice = startingPrice;
        this.status = "RUNNING";
        this.bidHistory = new ArrayList<>();
    }
  

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

    public long getElapseTime() { 
        if (LocalDateTime.now().isBefore(startTime)) {
            return 0;
        }

        LocalDateTime endTime = LocalDateTime.now().isAfter(stopTime) ? stopTime : LocalDateTime.now();
        return Duration.between(startTime, endTime).getSeconds();
    }

  
    public long getRemainingTime() {
        if (LocalDateTime.now().isAfter(stopTime)) {
            return 0;
        }
        return Duration.between(LocalDateTime.now(), stopTime).getSeconds();
    }

    // Thời gian còn lại chính xác đến mili giây (dùng cho Sniper Protection)
    public double getRemainingTimeSecondsFractional() {
        if (LocalDateTime.now().isAfter(stopTime)) {
            return 0;
        }
        return Duration.between(LocalDateTime.now(), stopTime).toMillis() / 1000.0;
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

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
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

<<<<<<< HEAD
 
    public synchronized String validateBid(BidTransaction bid) {
        if (bid == null || bid.getBidderId() == null || bid.getBidderId().isEmpty()) {
            return "INVALID_BIDDER";
=======
    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public boolean placeBid(BidTransaction bid) {
        if(LocalDateTime.now().isBefore(startTime)) {
            System.out.println("Phiên đấu giá chưa bắt đầu!");
            return false;
>>>>>>> 08664749faefa4fa4fb6e7af2e904320dd937533
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

    
    public synchronized boolean placeBid(BidTransaction bid) {
        String validation = validateBid(bid);
        if (validation != null) {
            System.out.println("Đặt giá bị từ chối (" + validation + ")");
            return false;
        }

        // Sniper Protection: Nếu bid đặt ở 0.5s cuối cùng, gia hạn thêm 0.5s
        double remainingFractional = getRemainingTimeSecondsFractional();
        if (remainingFractional >= 0 && remainingFractional <= 0.5) {
            stopTime = stopTime.plusNanos(500_000_000); // 500 milliseconds
            System.out.println("Sniper Protection: Phiên đấu giá " + getId() + " được gia hạn thêm 0.5s");
        }

        currentHighestPrice = bid.getBidAmount();
        highestBidderId = bid.getBidderId();
        bidHistory.add(bid);
        return true;
    }
}