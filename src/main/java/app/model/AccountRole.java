package app.model;

public enum AccountRole {
    ADMIN,
    SELLER,
    BIDDER;

    public boolean canSelfRegister() {
        return this == SELLER || this == BIDDER;
    }
}
