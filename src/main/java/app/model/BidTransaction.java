package app.model;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private String auctionId;
    private String bidderId;
    private double bidAmount;
    private LocalDateTime timestamp;

    public BidTransaction(String id, String auctionId, String bidderId, double bidAmount, LocalDateTime timestamp) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.timestamp = timestamp;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}