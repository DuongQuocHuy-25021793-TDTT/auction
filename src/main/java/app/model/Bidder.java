package app.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import  java.util.Map;
import java.util.UUID;

public class Bidder extends User {
    private final List<BidTransaction> bidHistory;
    @Override
    public AccountRole getRole() {
        return AccountRole.BIDDER;
    }

    public Bidder(String id, String username, String password) {
        super(id, username, password);
        this.bidHistory = new ArrayList<>();
    }

    public boolean placeBid(Auction auction, double amount) {
        if (auction == null) {
            System.out.println("Không tìm thấy phiên đấu giá.");
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
                LocalDateTime.now(),
                username
        );

        boolean success = auction.placeBid(bid);
        if (success) {
            this.bidHistory.add(bid);
            System.out.println(username + " đã đặt giá: " + amount);
        }
        return success;
    }

    public AuctionProposalRequest createProductRequest(String productType,
                                                       String productName,
                                                       String productDescription,
                                                       Map<String, String> productAttributes,
                                                       double desiredPrice,
                                                       long requestedDurationMinutes,
                                                       LocalDateTime requestedStartTime) {
        return new AuctionProposalRequest(
                getId(),
                getUsername(),
                productType,
                productName,
                productDescription,
                productAttributes,
                desiredPrice,
                requestedDurationMinutes,
                requestedStartTime);
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }
}