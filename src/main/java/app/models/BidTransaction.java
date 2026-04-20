package app.models;

import java.time.LocalDateTime;

public class BidTransaction {
    private Bidder bidder;
    private double amount;
    private LocalDateTime time;
    public BidTransaction(Bidder bidder, double amount, LocalDateTime time) {
        this.bidder = bidder;
        this.amount = amount;
        this.time = time;
    }
    public Bidder getBidder() {
        return bidder;
    }
    public double getAmount() {
        return amount;
    }
    public LocalDateTime getTime() {
        return time;
    }
    public void setBidder(Bidder bidder) {
        this.bidder = bidder;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }
    public void setTime(LocalDateTime time) {
        this.time = time;
    }
    


}
