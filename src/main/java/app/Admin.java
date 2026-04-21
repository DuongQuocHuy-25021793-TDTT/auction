package app;

public class Admin extends User {

    

    public Admin(String id, String username, String password) {
        super(id, username, password);
    }

    public void cancelAuction(Auction auction){
        System.out.println("Admin " + this.username + " đã hủy phiên đấu giá: " + auction.getId());
    }
    
}
