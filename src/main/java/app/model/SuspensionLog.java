package app.model;

import java.time.LocalDateTime;

public class SuspensionLog {
    private String id;
    private String userId;
    private int suspensionLevel; // 1, 2, or 3 (permanent)
    private LocalDateTime timestamp;
    private String status; // "Đang hiệu lực", "Đã được khôi phục", "Đã xóa vĩnh viễn"

    public SuspensionLog(String id, String userId, int suspensionLevel, LocalDateTime timestamp, String status) {
        this.id = id;
        this.userId = userId;
        this.suspensionLevel = suspensionLevel;
        this.timestamp = timestamp;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getSuspensionLevel() {
        return suspensionLevel;
    }

    public void setSuspensionLevel(int suspensionLevel) {
        this.suspensionLevel = suspensionLevel;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
