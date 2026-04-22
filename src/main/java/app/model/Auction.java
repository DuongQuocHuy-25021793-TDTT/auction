package app.model;

import java.time.LocalDateTime;

class Auction  extends  Entity {
    private Item item ;
    private LocalDateTime startTime;
    private LocalDateTime stopTime;
    private double currentHighestPrice;
    private String status;
    public Auction(String id, Item  item, LocalDateTime startTime, LocalDateTime stopTime,
            double currentHighestPrice, String status) {
        super(id);
        this.item = item;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.currentHighestPrice = currentHighestPrice;
        this.status = status;
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
    
    
    
    
    
    


    
}
