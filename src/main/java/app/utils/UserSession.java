package app.utils;

public class UserSession {
    private static UserSession instance;

    private String username;
    private String role; // "ADMIN" hoặc "GUEST"

    private UserSession() {}

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    // Lưu thông tin khi đăng nhập thành công
    public void createUserSession(String username, String role) {
        this.username = username;
        this.role = role;
    }

    // Xóa thông tin khi đăng xuất
    public void clearSession() {
        this.username = null;
        this.role = null;
    }

    public String getUsername() { return username; }
    public String getRole() { return role; }
    
    // Hàm tiện ích để kiểm tra nhanh xem có phải Admin không
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(this.role);
    }
}