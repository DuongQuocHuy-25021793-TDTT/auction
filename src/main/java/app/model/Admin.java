package app.model;

public class Admin extends User {

    public Admin(String id, String username, String password) {
        super(id, username, password);
    }

    public void cancelAuction(Auction auction) {
        if (auction != null) {
            auction.setStatus("CANCELLED");
            System.out.println("Admin " + this.username + " đã hủy phiên đấu giá: " + auction.getId());
        }
    }

    public void stopAuction(Auction auction) {
        if (auction != null) {
            auction.setStatus("STOPPED");
            System.out.println("Admin " + this.username + " đã ngưng phiên đấu giá: " + auction.getId());
        }
    }

    public boolean canManageHumanResources() {
        return true;
    }

    @Override
    public AccountRole getRole() {
        return AccountRole.ADMIN;
    }
}
