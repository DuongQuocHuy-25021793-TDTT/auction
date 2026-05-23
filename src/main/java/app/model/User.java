package app.model;

public abstract class User extends Entity {
    protected String username;
    protected String password;
    public User() {
        super(""); // Tạm truyền chuỗi rỗng cho class Entity cha
    }

    public User(String id, String username, String password) {
        super(id);
        this.username = username;
        this.password = password;
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

    public abstract AccountRole getRole();
}
