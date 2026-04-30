package app.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Bidder extends User {
    private List<BidTransaction> bidHistory; 

    public Bidder(String id, String username, String password) {
        super(id, username, password);
        this.bidHistory = new ArrayList<>();
    }

    public boolean placeBid(Auction auction, double amount) {
        if (auction == null) {
            System.out.println("Không tìm thấy giao dịch.");
            return false;
        }
        if (amount <= 0) {
            System.out.println("Số tiền đấu giá phải lớn hơn 0.");
            return false;
        }

        BidTransaction bid = new BidTransaction(
                UUID.randomUUID().toString(), 
                auction.getId(),
                getId(),
                amount,
                LocalDateTime.now()
        );

        boolean success = auction.placeBid(bid);
        if (success) {
            this.bidHistory.add(bid);
            System.out.println(username + " đã đặt giá tiền là: " + amount);
        }
        return success;
    }

    public List<BidTransaction>  getBidHistory() {
        return bidHistory;
    }
}