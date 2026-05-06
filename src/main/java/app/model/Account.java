package app.model;

public class Account extends User {
    private AccountRole role;

    public Account(String id, String username, String password, AccountRole role) {
        super(id, username, password);
        this.role = role;
    }

    public AccountRole getRole() {
        return role;
    }

    public void setRole(AccountRole role) {
        this.role = role;
    }
}
