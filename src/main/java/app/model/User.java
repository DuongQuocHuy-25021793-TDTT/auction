package app.model;

public abstract class User extends Entity {
    protected String username;
    protected String password;
    protected String fullName;
    protected String status;
    protected int suspensionCount;
    protected long suspendedUntil;
    protected long lastSuspensionTime;

    public User() {
        super(""); // Tạm truyền chuỗi rỗng cho class Entity cha
        this.status = "ACTIVE";
    }

    public User(String id, String username, String password) {
        super(id);
        this.username = username;
        this.password = password;
        this.status = "ACTIVE";
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getSuspensionCount() { return suspensionCount; }
    public void setSuspensionCount(int suspensionCount) { this.suspensionCount = suspensionCount; }

    public long getSuspendedUntil() { return suspendedUntil; }
    public void setSuspendedUntil(long suspendedUntil) { this.suspendedUntil = suspendedUntil; }

    public long getLastSuspensionTime() { return lastSuspensionTime; }
    public void setLastSuspensionTime(long lastSuspensionTime) { this.lastSuspensionTime = lastSuspensionTime; }

    public abstract AccountRole getRole();
}
