package app;

import java.time.LocalDateTime;
import java.util.List;

class Auction {
    private List<Item> item ;
    private LocalDateTime startTime;
    private LocalDateTime StopTime;
    private String status;
    public Auction(List<Item> item, LocalDateTime startTime, LocalDateTime stopTime, String status) {
        this.item = item;
        this.startTime = startTime;
        this.StopTime = stopTime;
        this.status = status;
    }
    public List<Item> getItem() {
        return item;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public LocalDateTime getStopTime() {
        return StopTime;
    }
    public String getStatus() {
        return status;
    }
    public void setItem(List<Item> item) {
        this.item = item;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    public void setStopTime(LocalDateTime stopTime) {
        StopTime = stopTime;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    
    


    
}
